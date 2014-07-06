package pe.archety.handlers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.neo4j.graphdb.*;
import pe.archety.ArchetypeConstants;
import pe.archety.ArchetypeServer;
import pe.archety.Labels;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import static pe.archety.ArchetypeConstants.EMAIL_VALIDATOR;
import static pe.archety.ArchetypeConstants.PHONE_UTIL;

public class GetLikesOrHatesHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(GetLikesOrHatesHandler.class.getName());
    private static GraphDatabaseService graphDB;
    private static ObjectMapper objectMapper;
    private RelationshipType relationshipType;

    public GetLikesOrHatesHandler( GraphDatabaseService graphDB, ObjectMapper objectMapper, RelationshipType relationshipType ) {
        this.graphDB = graphDB;
        this.objectMapper = objectMapper;
        this.relationshipType = relationshipType;
    }

    @Override
    public void handleRequest( final HttpServerExchange exchange ) throws Exception {
        exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, ArchetypeServer.JSON_UTF8 );


        ArrayList<HashMap<String, String>> results = new ArrayList<>();

        String identity = exchange.getAttachment( io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY )
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

        String identityHash = ArchetypeConstants.calculateHash(identity);

        Long identityNodeId = ArchetypeServer.identityCache.getIfPresent( identityHash );

        try ( Transaction tx = graphDB.beginTx() ){
            Node identityNode = null;
            if( identityNodeId == null )  {

                // If the node id is not in the cache, let's try to find the node in the index.
                ResourceIterator<Node> iterator = graphDB.findNodesByLabelAndProperty( Labels.Identity, "identity", identityHash ).iterator();

                // If it's in the index, cache it
                if ( iterator.hasNext() ) {
                    identityNode = iterator.next();
                    ArchetypeServer.identityCache.put( identityHash, identityNode.getId() );
                } else {
                    logger.warning( identity + " not found." );
                    exchange.setResponseCode( 404 );
                    return;
                }
            } else {
                identityNode = graphDB.getNodeById( identityNodeId );
            }

            for ( Relationship relationship : identityNode.getRelationships( Direction.OUTGOING, relationshipType ) ) {
                Node pageNode = relationship.getEndNode();
                HashMap<String, String> result = new HashMap();
                result.put( "title", (String)pageNode.getProperty( "title" ) );
                result.put( "url", (String)pageNode.getProperty( "url" ) );
                results.add(result);
            }
        }

        exchange.setResponseCode( 200 );
        exchange.getResponseSender().send( ByteBuffer.wrap(
                objectMapper.writeValueAsBytes(results)));

    }
}
