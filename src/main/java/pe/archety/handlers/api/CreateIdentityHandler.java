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
import org.apache.commons.validator.routines.EmailValidator;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.util.encoders.Base64;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.helpers.Strings;
import pe.archety.*;


import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

public class CreateIdentityHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(CreateIdentityHandler.class.getName());
    private static GraphDatabaseService graphDB;
    private static ObjectMapper objectMapper;
    private static final EmailValidator validator = EmailValidator.getInstance();
    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private static final BatchWriterService batchWriterService = BatchWriterService.INSTANCE;

    public CreateIdentityHandler( GraphDatabaseService graphDB, ObjectMapper objectMapper) {
        this.graphDB = graphDB;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleRequest( final HttpServerExchange exchange ) throws Exception {
        exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, ArchetypeServer.JSON_UTF8 );
        exchange.startBlocking();
        final InputStream inputStream = exchange.getInputStream();
        final String body = new String(ByteStreams.toByteArray(inputStream), Charsets.UTF_8);
        HashMap input = objectMapper.readValue(body, HashMap.class);

        boolean validIdentity = false;
        String identity = "";

        if( input.containsKey( "email" ) ){
            String email = (String)input.get( "email" );
            if( validator.isValid( email ) ){
                validIdentity = true;
                identity = email;
            }
        } else if( input.containsKey( "phone" ) ){
            Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber();
            try {
                String phone = (String) input.get("phone");
                if (input.containsKey("region")) {
                    String region = (String) input.get("region");

                    phoneNumber = phoneUtil.parse(phone, region);
                } else {
                    phoneNumber = phoneUtil.parse(phone, "US");
                }
            } catch (NumberParseException e) {
                String error = "Error Parsing Phone Number: " + body;
                logger.severe(error);
                exchange.getResponseSender().send("{\"error\":\"" + error + "\"}");
                return;
            }

            if( phoneUtil.isValidNumber( phoneNumber ) ) {
                validIdentity = true;
                identity = phoneUtil.format( phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164 ); // "+41446681800"
            } else {
                String error = "Parameters email or phone required.";
                exchange.getResponseSender().send("{\"error\":\"" + error + "\"}");
                return;
            }
        }

        String identityHash = "";
        if( validIdentity ) {
            identityHash = calculateHash(identity);
            Long identityNodeId = ArchetypeServer.identityCache.getIfPresent(identityHash);

            if( identityNodeId == null ) {
                try ( Transaction tx = graphDB.beginTx() ) {

                    // If the node id is not in the cache, let's try to find the node in the index.
                    ResourceIterator<Node> results = graphDB.findNodesByLabelAndProperty( Labels.Identity, "identity", identityHash ).iterator();

                    // If it's not in the index go create it asynchronously
                    if ( !results.hasNext() ) {
                        HashMap<String, Object> write = new HashMap<>();
                        HashMap<String, Object> data = new HashMap<>();
                        data.put( "identity", identityHash );
                        write.put( ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_IDENTITY );
                        write.put( ArchetypeConstants.DATA, data );
                        batchWriterService.queue.put( write );
                    }
                }
            }
        }

        exchange.setResponseCode(201);
        exchange.getResponseSender().send(ByteBuffer.wrap(
                objectMapper.writeValueAsBytes(
                        Collections.singletonMap("identity", identity))));
    }

    private static String calculateHash(String input) {
        SHA3Digest digest = new SHA3Digest(512);
        byte[] inputAsBytes = input.getBytes(Charsets.UTF_8);
        byte[] retValue = new byte[digest.getDigestSize()];
        digest.update(inputAsBytes, 0, inputAsBytes.length);
        digest.doFinal(retValue, 0);
        return Base64.toBase64String(retValue);
    }

}