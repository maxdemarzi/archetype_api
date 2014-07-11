package pe.archety.handlers.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import pe.archety.Labels;

import org.apache.log4j.Logger;

import static pe.archety.ArchetypeServer.TEXT_PLAIN;

public class GetTokenHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger( GetTokenHandler.class.getName() );
    private static GraphDatabaseService graphDB;

    public GetTokenHandler( GraphDatabaseService graphDB ) {
        this.graphDB = graphDB;
    }

    @Override
    public void handleRequest( final HttpServerExchange exchange ) throws Exception {

        String token = exchange.getAttachment( io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY )
                .getParameters().get( "token" );

        try ( Transaction tx = graphDB.beginTx() ){
            final Node identityNode = IteratorUtil.singleOrNull(graphDB.findNodesByLabelAndProperty( Labels.Identity, "generatedToken", token ) );

            if (identityNode != null) {
                identityNode.setProperty( "authenticatedToken", token );
            } else {
                String error = "Error authenticating token.";
                exchange.setResponseCode( 400 );
                exchange.getResponseSender().send("{\"error\":\"" + error + "\"}");
                return;
            }
            tx.success();
            exchange.setResponseCode( 200 );
            exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, TEXT_PLAIN );
            exchange.getResponseSender().send( "Token Authenticated!" );
        } catch (Exception e) {
            logger.error( "Error Authenticating token: " + e.getMessage() );
            exchange.setResponseCode( 500 );
            exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, TEXT_PLAIN );
            exchange.getResponseSender().send( "An error has occurred" );
        }

    }
}
