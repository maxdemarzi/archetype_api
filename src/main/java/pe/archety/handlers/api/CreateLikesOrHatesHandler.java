package pe.archety.handlers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.neo4j.graphdb.*;
import pe.archety.*;

import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Logger;

import static pe.archety.ArchetypeConstants.EMAIL_VALIDATOR;
import static pe.archety.ArchetypeConstants.PHONE_UTIL;
import static pe.archety.ArchetypeConstants.URLPREFIX;
import static pe.archety.ArchetypeConstants.HTTP_CLIENT;
import static pe.archety.ArchetypeConstants.BATCH_WRITER_SERVICE;

public class CreateLikesOrHatesHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(CreateLikesOrHatesHandler.class.getName());
    private static GraphDatabaseService graphDB;
    private static ObjectMapper objectMapper;
    private String relationshipTypeName;

    public CreateLikesOrHatesHandler(GraphDatabaseService graphDB, ObjectMapper objectMapper, String relationshipTypeName) {
        this.graphDB = graphDB;
        this.objectMapper = objectMapper;
        this.relationshipTypeName = relationshipTypeName;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, ArchetypeServer.JSON_UTF8 );

        String identity = exchange.getAttachment( PathTemplateMatch.ATTACHMENT_KEY )
                .getParameters().get( "identity" );

        if( identity.contains( "@" ) ){
            if( !EMAIL_VALIDATOR.isValid( identity ) ){
                String error = "Email not valid.";
                exchange.setResponseCode( 400 );
                exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
                return;
            }
        } else {
            Phonenumber.PhoneNumber phoneNumber;
            try {
                phoneNumber = PHONE_UTIL.parse( identity, "US" );
            } catch (NumberParseException e) {
                String error = "Error Parsing Phone Number.";
                logger.severe( error );
                exchange.setResponseCode( 400 );
                exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
                return;
            }

            if( PHONE_UTIL.isValidNumber( phoneNumber ) ) {
                identity = PHONE_UTIL.format( phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164 ); // "+41446681800"
            } else {
                String error = "Invalid Phone Number.";
                exchange.setResponseCode( 400 );
                exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
                return;
            }
        }

        HashMap<String, Object> write = new HashMap<>();
        HashMap<String, Object> data = new HashMap<>();

        boolean createIdentity = true;

        String identityHash = ArchetypeConstants.calculateHash(identity);

        Long identityNodeId = ArchetypeServer.identityCache.getIfPresent( identityHash );

        if( identityNodeId == null ) try ( Transaction tx = graphDB.beginTx() ) {

            // If the node id is not in the cache, let's try to find the node in the index.
            ResourceIterator<Node> results = graphDB.findNodesByLabelAndProperty( Labels.Identity, "identity", identityHash ).iterator();

            // If it's in the index, cache it
            if ( results.hasNext() ) {
                Node identityNode = results.next();
                ArchetypeServer.identityCache.put( identityHash, identityNode.getId() );
                data.put( "identityNodeId", identityNode.getId() );
                createIdentity = false;
            } else {
                // If it's not in the index go create it asynchronously
                data.put( "identityHash", identityHash );
            }
        } else {
            data.put( "identityNodeId", identityNodeId );
            createIdentity = false;
        }

        boolean createPage = true;

        exchange.startBlocking();
        final InputStream inputStream = exchange.getInputStream();
        final String body = new String(ByteStreams.toByteArray( inputStream), Charsets.UTF_8 );
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

        Long pageNodeId = ArchetypeServer.urlCache.getIfPresent(url);
        if( pageNodeId == null ) try ( Transaction tx = graphDB.beginTx() ) {

            // If the node id is not in the cache, let's try to find the node in the index.
            ResourceIterator<Node> results = graphDB.findNodesByLabelAndProperty( Labels.Page, "url", url ).iterator();

            // If it's in the index, cache it
            if (results.hasNext()) {
                Node pageNode = results.next();
                ArchetypeServer.urlCache.put( url, pageNode.getId() );
                data.put( "pageNodeId", pageNode.getId() );
                createPage = false;
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
                    data.put( "url", url );
                    data.put( "title", title );
                } else {
                    String error = url + " not found. HTTP Code: " + code;
                    exchange.setResponseCode( 400 );
                    exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
                    return;
                }
            }
        } else {
            data.put( "pageNodeId", pageNodeId );
            createPage = false;
        }

        if( !createPage && createIdentity ){
            if ( relationshipTypeName.equals(Relationships.LIKES.name())){
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_IDENTITY_AND_LIKES_RELATIONSHIP );
            } else {
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_IDENTITY_AND_HATES_RELATIONSHIP );
            }
        }

        if( createPage && !createIdentity ){
            if ( relationshipTypeName.equals(Relationships.LIKES.name())){
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_PAGE_AND_LIKES_RELATIONSHIP );
            } else {
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_PAGE_AND_HATES_RELATIONSHIP );
            }
        }

        if( createPage && createIdentity ){
            if ( relationshipTypeName.equals(Relationships.LIKES.name())){
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_BOTH_AND_LIKES_RELATIONSHIP );
            } else {
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_BOTH_AND_HATES_RELATIONSHIP );
            }
        }

        if( !createPage && !createIdentity ){
            if ( relationshipTypeName.equals(Relationships.LIKES.name())){
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_LIKES_RELATIONSHIP );
            } else {
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_HATES_RELATIONSHIP );
            }
        }

        write.put( ArchetypeConstants.DATA, data );
        BATCH_WRITER_SERVICE.queue.put( write );

        HashMap<String, String> response = new HashMap<>();
        response.put( "identity", identity );
        response.put( "url", url );
        response.put( "title", title );
        response.put( "relationship_type", relationshipTypeName);

        exchange.setResponseCode( 201 );
        exchange.getResponseSender().send( ByteBuffer.wrap(
                objectMapper.writeValueAsBytes(
                        response)));

    }

}