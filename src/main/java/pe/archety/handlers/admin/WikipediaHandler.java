package pe.archety.handlers.admin;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.joda.time.DateTime;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import pe.archety.*;

import java.net.URLEncoder;
import static pe.archety.ArchetypeConstants.URLPREFIX;
import static pe.archety.ArchetypeServer.TEXT_PLAIN;

public class WikipediaHandler implements HttpHandler {

    GraphDatabaseService db;
    private static final String URL = "url";
    private static final String TITLE = "title";

    public WikipediaHandler(GraphDatabaseService db){
        this.db = db;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        long startTime = System.nanoTime();
        long transactionTime = System.nanoTime();
        int i = 0;
        Transaction tx = db.beginTx();
        try {
            for ( Node page : GlobalGraphOperations.at(db).getAllNodesWithLabel( Labels.Page )) {
                    if (!( page.hasProperty( URL ) ) ) {
                        i++;
                        String url = (String)page.getProperty( TITLE );
                        url = url.replace(" ", "_");
                        url = URLEncoder.encode(url, "UTF-8");
                        page.setProperty( URL, URLPREFIX + url );
                    } else {
                        continue;
                    }

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
        exchange.getResponseSender().send("Wikipedia Data Ready!");
    }
}
