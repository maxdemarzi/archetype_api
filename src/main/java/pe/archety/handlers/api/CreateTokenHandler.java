package pe.archety.handlers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import pe.archety.ArchetypeConstants;
import pe.archety.ArchetypeServer;
import pe.archety.BatchWriterServiceAction;
import pe.archety.Labels;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

import static pe.archety.ArchetypeConstants.BATCH_WRITER_SERVICE;
import static pe.archety.ArchetypeConstants.EMAIL_VALIDATOR;

public class CreateTokenHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(CreateTokenHandler.class.getName());
    private static GraphDatabaseService graphDB;
    private static ObjectMapper objectMapper;

    public CreateTokenHandler(GraphDatabaseService graphDB, ObjectMapper objectMapper) {
        this.graphDB = graphDB;
        this.objectMapper = objectMapper;
    }

    /*
        Input:
        { "email": "me@meh.com" }
     */

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ArchetypeServer.JSON_UTF8);
        exchange.startBlocking();
        final InputStream inputStream = exchange.getInputStream();
        final String body = new String(ByteStreams.toByteArray(inputStream), Charsets.UTF_8);
        HashMap input = new HashMap();
        try {
            input = objectMapper.readValue(body, HashMap.class);
        } catch (Exception e) {
            String error = "Error parsing JSON.";
            exchange.setResponseCode(400);
            exchange.getResponseSender().send("{\"error\":\"" + error + "\"}");
            return;
        }

        boolean validIdentity = false;
        String identity = "";

        if (input.containsKey("email")) {
            String email = (String) input.get("email");
            if (EMAIL_VALIDATOR.isValid(email)) {
                validIdentity = true;
                identity = email;
            }
        }  else {
            String error = "Email parameter required.";
            exchange.setResponseCode(400);
            exchange.getResponseSender().send("{\"error\":\"" + error + "\"}");
            return;
        }

        String identityHash = "";
        if (validIdentity) {
            identityHash = ArchetypeConstants.calculateHash(identity);
            Long identityNodeId = ArchetypeServer.identityCache.getIfPresent(identityHash);

            HashMap<String, Object> write = new HashMap<>();
            HashMap<String, Object> data = new HashMap<>();

            if (identityNodeId == null) try (Transaction tx = graphDB.beginTx()) {

                // If the node id is not in the cache, let's try to find the node in the index.
                final Node identityNode = IteratorUtil.singleOrNull(graphDB.findNodesByLabelAndProperty(Labels.Identity, "identity", identityHash));

                // If it's in the index, cache it
                if (identityNode != null) {

                    ArchetypeServer.identityCache.put(identityHash, identityNode.getId() );
                    write.put(ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_TOKEN);
                    data.put( "identityNodeId", identityNode.getId() );
                } else {
                    // If it's not in the index go create it asynchronously
                    data.put("identityHash", identityHash);
                    write.put(ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_IDENTITY_WITH_TOKEN);
                }
            } else {
                write.put(ArchetypeConstants.ACTION, BatchWriterServiceAction.CREATE_TOKEN);
                data.put( "identityNodeId", identityNodeId );
            }

            data.put("identity", identity);
            write.put(ArchetypeConstants.DATA, data);
            BATCH_WRITER_SERVICE.queue.put(write);

        }

        exchange.setResponseCode(201);
        exchange.getResponseSender().send(ByteBuffer.wrap(
                objectMapper.writeValueAsBytes(
                        Collections.singletonMap("identity", identity))));
    }

}
