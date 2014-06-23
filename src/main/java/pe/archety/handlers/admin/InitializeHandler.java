package pe.archety.handlers.admin;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.Schema;
import pe.archety.Labels;

import java.util.concurrent.TimeUnit;

import static pe.archety.ArchetypeServer.TEXT_PLAIN;

public class InitializeHandler implements HttpHandler {

    GraphDatabaseService db;

    public InitializeHandler(GraphDatabaseService db){
        this.db = db;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        try (Transaction tx = db.beginTx()) {
            Schema schema = db.schema();
            schema.constraintFor( Labels.Identity )
                    .assertPropertyIsUnique("identity")
                    .create();
            schema.constraintFor( Labels.Page )
                    .assertPropertyIsUnique("url")
                    .create();
            tx.success();

            db.index().getNodeAutoIndexer().setEnabled(true);
            db.index().getNodeAutoIndexer().startAutoIndexingProperty("title");
        }

        try ( Transaction tx = db.beginTx()) {
            Schema schema = db.schema();
            schema.awaitIndexesOnline(1, TimeUnit.DAYS);
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TEXT_PLAIN);
        exchange.getResponseSender().send("Initialized!");
    }
}
