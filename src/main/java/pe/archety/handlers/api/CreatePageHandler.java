package pe.archety.handlers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import pe.archety.*;

import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.HashMap;

import static pe.archety.ArchetypeConstants.URLPREFIX;
import static pe.archety.ArchetypeConstants.HTTP_CLIENT;
import static pe.archety.ArchetypeConstants.BATCH_WRITER_SERVICE;

public class CreatePageHandler implements HttpHandler {

    private static GraphDatabaseService graphDB;
    private static ObjectMapper objectMapper;


    public CreatePageHandler( GraphDatabaseService graphDB, ObjectMapper objectMapper ) {
        this.graphDB = graphDB;
        this.objectMapper = objectMapper;
    }

    /*
       Input:
       { "url": "http://en.wikipedia.org/wiki/Neo4j" }
       or
       { "title": "Neo4j" }
       { "url": "http://en.wikipedia.org/wiki/Neo4j", "title": "Neo4j" }
    */
    @Override
    public void handleRequest( final HttpServerExchange exchange ) throws Exception {
        exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, ArchetypeServer.JSON_UTF8 );
        exchange.startBlocking();
        final InputStream inputStream = exchange.getInputStream();
        final String body = new String( ByteStreams.toByteArray( inputStream ), Charsets.UTF_8 );
        HashMap input = objectMapper.readValue( body, HashMap.class );

        String url = "";
        String title = "";
        if( input.containsKey( "url" ) ) {
            url = (String) input.get( "url" );
            if ( !url.startsWith( URLPREFIX ) ) {
                String error = "URL must start with " +  URLPREFIX;
                exchange.setResponseCode( 400 );
                exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
                return;
            }
        } else if ( input.containsKey( "title" ) ) {
            title = (String) input.get( "title" );
            url = title.replace( " ", "_" );
            url = URLEncoder.encode( url, "UTF-8" );
            url = URLPREFIX + url;
        }

        Long pageNodeId = ArchetypeServer.urlCache.getIfPresent( url );
        if( pageNodeId == null ) try (Transaction tx = graphDB.beginTx()) {

            // If the node id is not in the cache, let's try to find the node in the index.
            ResourceIterator<Node> results = graphDB.findNodesByLabelAndProperty( Labels.Page, "url", url ).iterator();

            // If it's in the index, cache it
            if (results.hasNext()) {
                Node pageNode = results.next();
                ArchetypeServer.urlCache.put( url, pageNode.getId() );
            } else {
                // Check that it is a valid page
                HttpHead httpHead = new HttpHead(url);
                CloseableHttpResponse response = HTTP_CLIENT.execute( httpHead );
                int code = response.getStatusLine().getStatusCode();
                response.close();

                if ( code == 200 ){
                    if ( title.equals( "" ) ) {
                        title = url.substring( URLPREFIX.length() );
                        title = URLDecoder.decode( title, "UTF-8" );
                        title = title.replace( "_", " " );
                    }

                    // If it's not in the index go create it asynchronously
                    HashMap<String, Object> write = new HashMap<>();
                    HashMap<String, Object> data = new HashMap<>();
                    data.put( "url", url );
                    data.put( "title", title );
                    write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_PAGE );
                    write.put( ArchetypeConstants.DATA, data );
                    BATCH_WRITER_SERVICE.queue.put( write );
                } else {
                    String error = url + " not found. HTTP Code: " + code;
                    exchange.setResponseCode( 400 );
                    exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
                    return;
                }
            }
        }
        HashMap<String, String> response = new HashMap<>();
        response.put( "url", url );
        response.put( "title", title );
        exchange.setResponseCode( 201 );
        exchange.getResponseSender().send( ByteBuffer.wrap(
                objectMapper.writeValueAsBytes(
                        response)));

    }
}


