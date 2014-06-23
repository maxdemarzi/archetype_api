package pe.archety.handlers.admin;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import static pe.archety.ArchetypeServer.TEXT_PLAIN;

public class WarmUpHandler implements HttpHandler {

    GraphDatabaseService graphDatabaseService;

    public WarmUpHandler(GraphDatabaseService graphDb){
        this.graphDatabaseService = graphDb;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        try ( Transaction tx = graphDatabaseService.beginTx() )
        {
            for ( Node n : GlobalGraphOperations.at(graphDatabaseService).getAllNodes() ) {
                n.getPropertyKeys();
                for ( Relationship relationship : n.getRelationships() ) {
                    relationship.getPropertyKeys();
                    relationship.getStartNode();
                }
            }
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TEXT_PLAIN);
        exchange.getResponseSender().send("Warmed up and ready to go!");
    }
}