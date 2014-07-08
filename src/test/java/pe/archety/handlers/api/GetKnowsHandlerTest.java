package pe.archety.handlers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;
import pe.archety.ArchetypeConstants;
import pe.archety.ArchetypeServer;
import pe.archety.Labels;
import pe.archety.Relationships;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static pe.archety.ArchetypeServer.JSON_UTF8;

public class GetKnowsHandlerTest {
    private static GraphDatabaseService db;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Undertow undertow;
    private static final JerseyClient client = JerseyClientBuilder.createClient();

    @Before
    public void setUp() throws Exception {
        ArchetypeServer.identityCache.invalidateAll();
        ArchetypeServer.urlCache.invalidateAll();

        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        populateDb(db);
        undertow = Undertow.builder()
                .addHttpListener( 9090, "localhost" )
                .setHandler(new RoutingHandler()
                                .add( "GET", "/v1/identities/{identity}/knows", new GetKnowsHandler( db, objectMapper ))
                )
                .build();
        undertow.start();

    }

    private void populateDb(GraphDatabaseService db) throws Exception {
        try ( Transaction tx = db.beginTx() ) {
            Node identity1Node = createIdentity(db, "maxdemarzi@gmail.com");
            Node identity2Node = createIdentity(db, "+13125137509");
            Relationship rel1 = identity1Node.createRelationshipTo(identity2Node, Relationships.KNOWS);
            rel1.setProperty("encryptedIdentity", ArchetypeConstants.encrypt("+13125137509", "maxdemarzi@gmail.com" ));
            
            Node identity3Node = createIdentity(db, "max@maxdemarzi.com");
            Relationship rel2 = identity3Node.createRelationshipTo(identity1Node, Relationships.KNOWS);
            rel2.setProperty("encryptedIdentity", ArchetypeConstants.encrypt("maxdemarzi@gmail.com", "max@maxdemarzi.com" ));
            Relationship rel3 = identity3Node.createRelationshipTo(identity2Node, Relationships.KNOWS);
            rel3.setProperty("encryptedIdentity", ArchetypeConstants.encrypt("+13125137509", "max@maxdemarzi.com" ));

            tx.success();
        }
    }

    private Node createIdentity(GraphDatabaseService db, String identity) {
        Node identityNode = db.createNode(Labels.Identity);
        String identityHash = ArchetypeConstants.calculateHash(identity);
        identityNode.setProperty("identity", identityHash);
        return identityNode;
    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();
        undertow.stop();
    }

    @Test
    public void shouldGetKnowsUsingEmail() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/identities/" + identity1.get("email") + "/knows")
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();
        ArrayList<HashMap<String, String>> actual = objectMapper.readValue( response.readEntity( String.class ), ArrayList.class );

        assertEquals( 200, code );
        assertEquals( likes1Response, actual );
    }

    @Test
    public void shouldGetMultipleKnowsUsingEmail() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/identities/" + identity4.get("email") + "/knows")
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();
        ArrayList<HashMap<String, String>> actual = objectMapper.readValue( response.readEntity( String.class ), ArrayList.class );

        assertEquals( 200, code );
        assertEquals( likes3Response, actual );
    }

    @Test
    public void shouldGetEmptyKnowsUsingPhone() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/identities/" + identity2.get("phone") + "/knows")
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();
        ArrayList actual = objectMapper.readValue( response.readEntity( String.class ), ArrayList.class );

        assertEquals( 200, code );
        assertEquals( likes2Response, actual );
    }

    @Test
    public void shouldNotGetKnowsFromUnknownIdentity() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/identities/" + identity3.get("email") + "/knows")
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();

        assertEquals( 404, code );
    }

    @Test
    public void shouldNotGetKnowsWithInvalidEmail() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidEmail.get( "email" ) + "/knows" )
                .request( JSON_UTF8 )
                .get();

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidEmailResponse, actual );
    }

    @Test
    public void shouldNotGetKnowsWithInvalidEmail2() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidEmail2.get( "email" ) + "/knows" )
                .request( JSON_UTF8 )
                .get();

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidEmailResponse2, actual );
    }

    @Test
    public void shouldNotGetKnowsWithInvalidPhoneNumber() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidPhoneNumber.get( "phone" ) + "/knows" )
                .request( JSON_UTF8 )
                .get();

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidPhoneNumberResponse, actual );
    }

    public static final HashMap<String, Object> identity1 =
            new HashMap<String, Object>() {{
                put( "email", "maxdemarzi@gmail.com" );
            }};

    public static final ArrayList<HashMap<String, String>> likes1Response = new ArrayList<HashMap<String, String>>(){{
        add( new HashMap<String, String>() {{
                 put( "identity", "+13125137509");
             }}
        );
    }};

    public static final HashMap<String, Object> identity2 =
            new HashMap<String, Object>() {{
                put( "phone", "+13125137509" );
            }};

    public static final ArrayList<HashMap<String, String>> likes2Response = new ArrayList<>();


    public static final HashMap<String, Object> identity3 =
            new HashMap<String, Object>() {{
                put( "email", "idont@exist.com" );
            }};

    public static final HashMap<String, Object> identity4 =
            new HashMap<String, Object>() {{
                put( "email", "max@maxdemarzi.com" );
            }};

    public static final ArrayList<HashMap<String, String>> likes3Response = new ArrayList<HashMap<String, String>>(){{
        add( new HashMap<String, String>() {{
                 put( "identity", "maxdemarzi@gmail.com");
             }}
        );
        add( new HashMap<String, String>() {{
                 put( "identity", "+13125137509");
             }}
        );

    }};

    public static final HashMap<String, Object> identityWithInvalidEmail =
            new HashMap<String, Object>() {{
                put( "email", "@boo" );
            }};

    public static final HashMap<String, Object> errorInvalidEmailResponse =
            new HashMap<String, Object>() {{
                put( "error", "Email not valid." );
            }};

    public static final HashMap<String, Object> identityWithInvalidEmail2 =
            new HashMap<String, Object>() {{
                put( "email", "booATboo.com" );
            }};

    public static final HashMap<String, Object> errorInvalidEmailResponse2 =
            new HashMap<String, Object>() {{
                put( "error", "Error Parsing Phone Number." );
            }};


    public static final HashMap<String, Object> identityWithInvalidPhoneNumber =
            new HashMap<String, Object>() {{
                put( "phone", "000000000" );
            }};

    public static final HashMap<String, Object> errorInvalidPhoneNumberResponse =
            new HashMap<String, Object>() {{
                put( "error", "Invalid Phone Number." );
            }};
}
