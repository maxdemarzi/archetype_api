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
import org.neo4j.test.TestGraphDatabaseFactory;
import pe.archety.ArchetypeServer;
import pe.archety.Relationships;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static pe.archety.ArchetypeConstants.URLPREFIX;
import static pe.archety.ArchetypeServer.JSON_UTF8;

public class CreateLikesOrHatesHandlerTest {
    private static GraphDatabaseService db;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Undertow undertow;
    private static final JerseyClient client = JerseyClientBuilder.createClient();

    @Before
    public void setUp() {
        ArchetypeServer.identityCache.invalidateAll();
        ArchetypeServer.urlCache.invalidateAll();

        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        undertow = Undertow.builder()
                .addHttpListener( 9090, "localhost" )
                .setHandler(new RoutingHandler()
                    .add( "POST", "/v1/identities/{identity}/likes", new CreateLikesOrHatesHandler( db, objectMapper, Relationships.LIKES.name() ) )
                    .add( "POST", "/v1/identities/{identity}/hates", new CreateLikesOrHatesHandler( db, objectMapper, Relationships.HATES.name() ) )
                )
                .build();
        undertow.start();
    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();
        undertow.stop();
    }

    @Test
    public void shouldCreateLikesUsingEmail() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/likes"  )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(page1), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( likes1Response, actual );
    }

    @Test
    public void shouldCreateHatesUsingEmail() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/hates"  )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(page1), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( hates1Response, actual );
    }

    @Test
    public void shouldCreateLikesUsingPhone() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity2.get( "phone" ) + "/likes" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(page1), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( likes2Response, actual );
    }

    @Test
    public void shouldNotCreateLikesWithInvalidEmail() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidEmail.get( "email" ) + "/likes" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(page1), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidEmailResponse, actual );
    }

    @Test
    public void shouldNotCreateLikesWithInvalidEmail2() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidEmail2.get( "email" ) + "/likes" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(page1), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidEmailResponse2, actual );
    }

    @Test
    public void shouldNotCreateLikesWithInvalidPhoneNumber() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidPhoneNumber.get( "phone" ) + "/likes" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(page1), JSON_UTF8));

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidPhoneNumberResponse, actual );
    }

    @Test
    public void shouldNotCreateLikesUsingInvalidURLPrefix() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/likes" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(pageWithInvalidURLPrefix), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidURLPrefixResponse, actual );
    }

    @Test
    public void shouldNotCreateLikesUsingInvalidURL() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/likes" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(pageWithInvalidURL), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidURLResponse, actual );
    }

    public static final HashMap<String, Object> identity1 =
            new HashMap<String, Object>() {{
                put( "email", "maxdemarzi@gmail.com" );
            }};

    public static final HashMap<String, Object> page1 =
            new HashMap<String, Object>() {{
                put( "url", "http://en.wikipedia.org/wiki/Neo4j" );
            }};

    public static final HashMap<String, Object> likes1Response =
            new HashMap<String, Object>() {{
                put( "identity", "maxdemarzi@gmail.com");
                put( "title", "Neo4j");
                put( "url", "http://en.wikipedia.org/wiki/Neo4j" );
                put( "relationship_type", "LIKES" );
            }};

    public static final HashMap<String, Object> hates1Response =
            new HashMap<String, Object>() {{
                put( "identity", "maxdemarzi@gmail.com");
                put( "title", "Neo4j");
                put( "url", "http://en.wikipedia.org/wiki/Neo4j" );
                put( "relationship_type", "HATES" );
            }};

    public static final HashMap<String, Object> identity2 =
            new HashMap<String, Object>() {{
                put( "phone", "3125137509" );
            }};

    public static final HashMap<String, Object> likes2Response =
            new HashMap<String, Object>() {{
                put( "identity", "+13125137509");
                put( "title", "Neo4j");
                put( "url", "http://en.wikipedia.org/wiki/Neo4j" );
                put( "relationship_type", "LIKES" );
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

    public static final HashMap<String, Object> pageWithInvalidURLPrefix =
            new HashMap<String, Object>() {{
                put( "url", "000000000" );
            }};

    public static final HashMap<String, Object> errorInvalidURLPrefixResponse =
            new HashMap<String, Object>() {{
                put( "error", "URL must start with " +  URLPREFIX );
            }};

    public static final HashMap<String, Object> pageWithInvalidURL =
            new HashMap<String, Object>() {{
                put( "url", "http://en.wikipedia.org/wiki/AKDFDDFKHKJHJLHDJKHDLHSLDHFJKDS" );
            }};

    public static final HashMap<String, Object> errorInvalidURLResponse =
            new HashMap<String, Object>() {{
                put( "error", "http://en.wikipedia.org/wiki/AKDFDDFKHKJHJLHDJKHDLHSLDHFJKDS not found. HTTP Code: 404" );
            }};

}
