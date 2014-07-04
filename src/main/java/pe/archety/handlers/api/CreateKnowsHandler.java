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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import pe.archety.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Logger;

import static pe.archety.ArchetypeConstants.BATCH_WRITER_SERVICE;
import static pe.archety.ArchetypeConstants.EMAIL_VALIDATOR;
import static pe.archety.ArchetypeConstants.PHONE_UTIL;

public class CreateKnowsHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger( CreateKnowsHandler.class.getName() );
    private static GraphDatabaseService graphDB;
    private static ObjectMapper objectMapper;

    public CreateKnowsHandler( GraphDatabaseService graphDB, ObjectMapper objectMapper ) {
        this.graphDB = graphDB;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleRequest( final HttpServerExchange exchange ) throws Exception {
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

        // here
        exchange.startBlocking();
        final InputStream inputStream = exchange.getInputStream();
        final String body = new String( ByteStreams.toByteArray(inputStream), Charsets.UTF_8 );
        HashMap input = new HashMap();
        try {
            input = objectMapper.readValue(body, HashMap.class);
        } catch (Exception e) {
            String error = "Error parsing JSON.";
            exchange.setResponseCode( 400 );
            exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
            return;
        }

        String identity2 = "";

        if( input.containsKey( "email" ) ){
            String email = (String)input.get( "email" );
            if( EMAIL_VALIDATOR.isValid( email ) ){
                identity2 = email;
            } else {
                String error = "Email not valid.";
                exchange.setResponseCode( 400 );
                exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
                return;
            }
        } else if( input.containsKey( "phone" ) ){
            Phonenumber.PhoneNumber phoneNumber;
            try {
                String phone = (String) input.get( "phone" );
                if (input.containsKey( "region" )) {
                    String region = (String) input.get("region");
                    phoneNumber = PHONE_UTIL.parse( phone, region );
                } else {
                    phoneNumber = PHONE_UTIL.parse( phone, "US" );
                }
            } catch (NumberParseException e) {
                String error = "Error Parsing Phone Number.";
                logger.severe( error );
                exchange.setResponseCode( 400 );
                exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
                return;
            }

            if( PHONE_UTIL.isValidNumber( phoneNumber ) ) {
                identity2 = PHONE_UTIL.format( phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164 ); // "+41446681800"
            } else {
                String error = "Invalid Phone Number.";
                exchange.setResponseCode( 400 );
                exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
                return;
            }
        } else {
            String error = "Parameters email or phone required.";
            exchange.setResponseCode( 400 );
            exchange.getResponseSender().send( "{\"error\":\"" + error + "\"}" );
            return;
        }

        boolean createIdentity2 = true;

        String identityHash2 = ArchetypeConstants.calculateHash(identity2);

        Long identityNodeId2 = ArchetypeServer.identityCache.getIfPresent( identityHash2 );

        if( identityNodeId2 == null ) try ( Transaction tx = graphDB.beginTx() ) {

            // If the node id is not in the cache, let's try to find the node in the index.
            ResourceIterator<Node> results = graphDB.findNodesByLabelAndProperty( Labels.Identity, "identity", identityHash2 ).iterator();

            // If it's in the index, cache it
            if ( results.hasNext() ) {
                Node identityNode2 = results.next();
                ArchetypeServer.identityCache.put( identityHash, identityNode2.getId() );
                data.put( "identityNodeId2", identityNode2.getId() );
                createIdentity = false;
            } else {
                // If it's not in the index go create it asynchronously
                data.put( "identityHash2", identityHash2 );
            }
        } else {
            data.put( "identityNodeId2", identityNodeId2 );
            createIdentity2 = false;
        }

        if( !createIdentity && createIdentity2 ){
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_2ND_IDENTITY_AND_KNOWS_RELATIONSHIP );
        }

        if( createIdentity && !createIdentity2 ){
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_IDENTITY_AND_KNOWS_RELATIONSHIP );
        }

        if( createIdentity && createIdentity2 ){
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_BOTH_AND_KNOWS_RELATIONSHIP );
        }

        if( !createIdentity && !createIdentity2 ){
                write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_KNOWS_RELATIONSHIP );
        }

        data.put( "encryptedIdentity", ArchetypeConstants.encrypt( identity2, identity ) );

        write.put( ArchetypeConstants.DATA, data );
        BATCH_WRITER_SERVICE.queue.put( write );

        HashMap<String, String> response = new HashMap<>();
        response.put( "identity", identity );
        response.put( "identity2", identity2 );
        response.put( "relationship_type", "KNOWS");

        exchange.setResponseCode( 201 );
        exchange.getResponseSender().send( ByteBuffer.wrap(
                objectMapper.writeValueAsBytes(
                        response)));

    }

}
