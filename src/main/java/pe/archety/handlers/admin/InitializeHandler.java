package pe.archety.handlers.admin;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.joda.time.DateTime;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.tooling.GlobalGraphOperations;
import pe.archety.Labels;

import java.util.concurrent.TimeUnit;

import static pe.archety.ArchetypeServer.TEXT_PLAIN;

public class InitializeHandler implements HttpHandler {
    private static final String TITLE = "title";
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
            schema.constraintFor( Labels.Identity )
                    .assertPropertyIsUnique("generatedToken")
                    .create();
            schema.constraintFor( Labels.Identity )
                    .assertPropertyIsUnique("authenticatedToken")
                    .create();
            schema.constraintFor( Labels.Page )
                    .assertPropertyIsUnique("url")
                    .create();
            tx.success();

            db.index().forNodes( "node_auto_index",
                    MapUtil.stringMap( IndexManager.PROVIDER, "lucene",
                                       "type", "fulltext",
                                       "to_lower_case", "true") );
            db.index().getNodeAutoIndexer().setEnabled(true);
            db.index().getNodeAutoIndexer().startAutoIndexingProperty("title");
        }

        try ( Transaction tx = db.beginTx()) {
            Schema schema = db.schema();
            schema.awaitIndexesOnline(1, TimeUnit.DAYS);
        }

        long startTime = System.nanoTime();
        long transactionTime = System.nanoTime();
        int i = 0;
        Transaction tx = db.beginTx();
        try {
            for (Node page : GlobalGraphOperations.at(db).getAllNodesWithLabel(Labels.Page)) {
                page.setProperty(TITLE, page.getProperty(TITLE));
                i++;

                if(i % 40000 == 0){
                    tx.success();
                    tx.close();
                    DateTime currently = new DateTime();
                    System.out.printf("Performed a transaction of 40000 writes in  %d [msec] @ %s \n", (System.nanoTime() - transactionTime) / 1000000, currently.toDateTimeISO());
                    transactionTime = System.nanoTime();
                    tx = db.beginTx();
                }
            }

            tx.success();
        } finally {
            tx.close();
            DateTime currently = new DateTime();
            System.out.printf("Performed a set of transactions with %d writes in  %d [msec] @ %s \n", i, (System.nanoTime() - startTime) / 1000000, currently.toDateTimeISO());
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TEXT_PLAIN);
        exchange.getResponseSender().send("Initialized!");
    }
}
