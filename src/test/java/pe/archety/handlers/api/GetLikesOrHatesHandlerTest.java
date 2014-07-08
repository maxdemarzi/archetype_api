package pe.archety.handlers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
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

public class GetLikesOrHatesHandlerTest {
    private static GraphDatabaseService db;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Undertow undertow;
    private static final JerseyClient client = JerseyClientBuilder.createClient();

    @Before
    public void setUp() throws JsonProcessingException {
        ArchetypeServer.identityCache.invalidateAll();
        ArchetypeServer.urlCache.invalidateAll();
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        populateDB(db);
        undertow = Undertow.builder()
                .addHttpListener( 9090, "localhost" )
                .setHandler(new RoutingHandler()
                                .add( "GET", "/v1/identities/{identity}/likes", new GetLikesOrHatesHandler(db, objectMapper, Relationships.LIKES))
                                .add( "GET", "/v1/identities/{identity}/hates", new GetLikesOrHatesHandler(db, objectMapper, Relationships.HATES))
                )
                .build();
        undertow.start();
    }

    private void populateDB(GraphDatabaseService db) {
        try ( Transaction tx = db.beginTx() ) {
            Node identity1Node = createIdentity(db, "maxdemarzi@gmail.com");

            Node page1Node = createPage(db, "Neo4j");
            identity1Node.createRelationshipTo(page1Node, Relationships.LIKES);

            Node page2Node = createPage(db, "Mongodb");
            identity1Node.createRelationshipTo(page2Node, Relationships.HATES);

            Node identity2Node = createIdentity(db, "+13125137509");

            Node identity3Node = createIdentity(db, "max@maxdemarzi.com");
            identity3Node.createRelationshipTo(page1Node, Relationships.LIKES);
            identity3Node.createRelationshipTo(page2Node, Relationships.LIKES);

            tx.success();
        }
    }

    private Node createPage(GraphDatabaseService db, String title) {
        Node pageNode = db.createNode(Labels.Page);
        pageNode.setProperty("title", title);
        pageNode.setProperty("url", ArchetypeConstants.URLPREFIX + title);
        return pageNode;
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
    public void shouldGetLikesUsingEmail() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/identities/" + identity1.get("email") + "/likes")
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();
        ArrayList<HashMap<String, String>> actual = objectMapper.readValue( response.readEntity( String.class ), ArrayList.class );

        assertEquals( 200, code );
        assertEquals( likes1Response, actual );
    }

    @Test
    public void shouldGetHatesUsingEmail() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/identities/" + identity1.get("email") + "/hates")
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();
        ArrayList<HashMap<String, String>> actual = objectMapper.readValue( response.readEntity( String.class ), ArrayList.class );

        assertEquals( 200, code );
        assertEquals( hates1Response, actual );
    }


    @Test
    public void shouldGetMultipleLikesUsingEmail() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/identities/" + identity4.get("email") + "/likes")
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();
        ArrayList<HashMap<String, String>> actual = objectMapper.readValue( response.readEntity( String.class ), ArrayList.class );

        assertEquals( 200, code );
        assertEquals( likes3Response, actual );
    }

    @Test
    public void shouldGetEmptyLikesUsingPhone() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/identities/" + identity2.get("phone") + "/likes")
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();
        ArrayList actual = objectMapper.readValue( response.readEntity( String.class ), ArrayList.class );

        assertEquals( 200, code );
        assertEquals( likes2Response, actual );
    }

    @Test
    public void shouldNotGetLikesFromUnknownIdentity() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/identities/" + identity3.get("email") + "/likes")
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();

        assertEquals( 404, code );
    }

    @Test
    public void shouldNotGetLikesWithInvalidEmail() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidEmail.get( "email" ) + "/likes" )
                .request( JSON_UTF8 )
                .get();

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidEmailResponse, actual );
    }

    @Test
    public void shouldNotGetLikesWithInvalidEmail2() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidEmail2.get( "email" ) + "/likes" )
                .request( JSON_UTF8 )
                .get();

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidEmailResponse2, actual );
    }

    @Test
    public void shouldNotGetLikesWithInvalidPhoneNumber() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidPhoneNumber.get( "phone" ) + "/likes" )
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
                put( "title", "Neo4j");
                put( "url", "http://en.wikipedia.org/wiki/Neo4j" );
             }}
        );
    }};

    public static final ArrayList<HashMap<String, String>> hates1Response = new ArrayList<HashMap<String, String>>(){{
        add( new HashMap<String, String>() {{
                put( "title", "Mongodb");
                put( "url", "http://en.wikipedia.org/wiki/Mongodb" );
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
                 put( "title", "Neo4j");
                 put( "url", "http://en.wikipedia.org/wiki/Neo4j" );
             }}
        );
        add( new HashMap<String, String>() {{
                 put( "title", "Mongodb");
                 put( "url", "http://en.wikipedia.org/wiki/Mongodb" );
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
