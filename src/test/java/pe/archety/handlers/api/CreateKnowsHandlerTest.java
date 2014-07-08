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

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static pe.archety.ArchetypeServer.JSON_UTF8;

public class CreateKnowsHandlerTest {
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
                                .add( "POST", "/v1/identities/{identity}/knows", new CreateKnowsHandler( db, objectMapper ) )
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
    public void shouldCreateKnowsUsingEmails() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/knows"  )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identity2), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( knows1Response, actual );
    }

    @Test
    public void shouldCreateKnowsUsingEmailAndPhone() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/knows"  )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identity3), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( knows2Response, actual );
    }

    @Test
    public void shouldCreateKnowsUsingPhones() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity4.get( "phone" ) + "/knows"  )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identity3), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( knows3Response, actual );
    }

    @Test
    public void shouldCreateKnowsUsingPhoneAndEmail() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity4.get( "phone" ) + "/knows"  )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identity2), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( knows4Response, actual );
    }

    @Test
    public void shouldNotCreateKnowsWithInvalidEmail() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidEmail.get( "email" ) + "/knows" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identity1), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidEmailResponse, actual );
    }

    @Test
    public void shouldNotCreateKnowsWithInvalidEmail2() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidEmail2.get( "email" ) + "/knows" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identity1), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidEmailResponse2, actual );
    }

    @Test
    public void shouldNotCreateKnowsWithInvalidPhoneNumber() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identityWithInvalidPhoneNumber.get( "phone" ) + "/knows" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identity1), JSON_UTF8));

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidPhoneNumberResponse, actual );
    }

    @Test
    public void shouldNotCreateKnowsWithInvalid2ndEmail() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/knows" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identityWithInvalidEmail), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidEmailResponse, actual );
    }

    @Test
    public void shouldNotCreateKnowsWithInvalid2ndEmail2() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/knows" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identityWithInvalidEmail2), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidEmailResponse, actual );
    }

    @Test
    public void shouldNotCreateKnowsWithInvalid2ndPhoneNumber() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/knows" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identityWithInvalidPhoneNumber), JSON_UTF8));

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidPhoneNumberResponse, actual );
    }

    @Test
    public void shouldNotCreateKnowsWithGarbageJSON() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/knows" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString("Garbage"), JSON_UTF8));

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorParsingJSONResponse, actual );
    }

    @Test
    public void shouldNotCreateKnowsWithGarbage2ndPhoneNumber() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities/" + identity1.get( "email" ) + "/knows" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identityWithMixedUpValues), JSON_UTF8));

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorParsingPhoneNumberResponse, actual );
    }



    public static final HashMap<String, Object> identity1 =
            new HashMap<String, Object>() {{
                put( "email", "maxdemarzi@gmail.com" );
            }};

    public static final HashMap<String, Object> identity2 =
            new HashMap<String, Object>() {{
                put( "email", "max@maxdemarzi.com" );
            }};

    public static final HashMap<String, Object> knows1Response =
            new HashMap<String, Object>() {{
                put( "identity", "maxdemarzi@gmail.com");
                put( "identity2", "max@maxdemarzi.com");
                put( "relationship_type", "KNOWS" );
            }};

    public static final HashMap<String, Object> identity3 =
            new HashMap<String, Object>() {{
                put( "phone", "3125137509" );
            }};

    public static final HashMap<String, Object> knows2Response =
            new HashMap<String, Object>() {{
                put( "identity", "maxdemarzi@gmail.com");
                put( "identity2", "+13125137509");
                put( "relationship_type", "KNOWS" );
            }};

    public static final HashMap<String, Object> identity4 =
            new HashMap<String, Object>() {{
                put( "phone", "3128675309" );
            }};

    public static final HashMap<String, Object> knows3Response =
            new HashMap<String, Object>() {{
                put( "identity", "+13128675309");
                put( "identity2", "+13125137509");
                put( "relationship_type", "KNOWS" );
            }};

    public static final HashMap<String, Object> knows4Response =
            new HashMap<String, Object>() {{
                put( "identity", "+13128675309");
                put( "identity2", "max@maxdemarzi.com");
                put( "relationship_type", "KNOWS" );
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

    public static final HashMap<String, Object> errorParsingPhoneNumberResponse =
            new HashMap<String, Object>() {{
                put( "error", "Error Parsing Phone Number." );
            }};

    public static final HashMap<String, Object> errorParsingJSONResponse =
            new HashMap<String, Object>() {{
                put( "error", "Error parsing JSON." );
            }};

    public static final HashMap<String, Object> identityWithMixedUpValues =
            new HashMap<String, Object>() {{
                put( "phone", "maxdemarzi@gmail.com" );
            }};
}
