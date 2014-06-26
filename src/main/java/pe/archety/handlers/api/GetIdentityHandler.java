package pe.archety.handlers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import pe.archety.ArchetypeConstants;
import pe.archety.ArchetypeServer;
import pe.archety.Labels;

import java.nio.ByteBuffer;
import java.util.Collections;

public class GetIdentityHandler implements HttpHandler {

    private static GraphDatabaseService graphDB;
    private static ObjectMapper objectMapper;

    public GetIdentityHandler( GraphDatabaseService graphDB, ObjectMapper objectMapper ) {
        this.graphDB = graphDB;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleRequest( final HttpServerExchange exchange ) throws Exception {
        exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, ArchetypeServer.JSON_UTF8 );

        String identity = exchange.getAttachment( io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY )
                .getParameters().get( "identity" );

        String identityHash = ArchetypeConstants.calculateHash( identity );

        Long identityNodeId = ArchetypeServer.identityCache.getIfPresent( identityHash );

        if( identityNodeId == null ) try ( Transaction tx = graphDB.beginTx() ) {

            // If the node id is not in the cache, let's try to find the node in the index.
            ResourceIterator<Node> results = graphDB.findNodesByLabelAndProperty( Labels.Identity, "identity", identityHash ).iterator();

            // If it's in the index, cache it
            if ( results.hasNext() ) {
                Node identityNode = results.next();
                ArchetypeServer.identityCache.put( identityHash, identityNode.getId() );
            } else {
                exchange.setResponseCode( 404 );
                return;
            }
        }

        exchange.setResponseCode( 200 );
        exchange.getResponseSender().send( ByteBuffer.wrap(
                objectMapper.writeValueAsBytes(
                        Collections.singletonMap( "identity", identity ))));

    }
}

