package pe.archety.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.neo4j.graphdb.GraphDatabaseService;
import pe.archety.ArchetypeServer;

public class GetIdentityHandler implements HttpHandler {

    private static GraphDatabaseService graphDB;

    public GetIdentityHandler( GraphDatabaseService graphDB ) {
        this.graphDB = graphDB;
    }

    @Override
    public void handleRequest( final HttpServerExchange exchange ) throws Exception {
        exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, ArchetypeServer.JSON_UTF8 );

        String identity = exchange.getAttachment( io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY )
                .getParameters().get( "identity" );

        exchange.getResponseSender().send( "Hello " + identity );
    }
}

