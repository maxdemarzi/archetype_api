package pe.archety.handlers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import org.glassfish.jersey.client.*;
import pe.archety.ArchetypeServer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.HashMap;

import static pe.archety.ArchetypeServer.JSON_UTF8;

import static org.junit.Assert.assertEquals;

public class CreateIdentityHandlerTest {
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
                                .add( "POST", "/v1/identities", new CreateIdentityHandler( db, objectMapper ) )
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
    public void shouldCreateIdentityUsingEmail() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities" )
                .request( JSON_UTF8 )
                .post(Entity.entity( objectMapper.writeValueAsString( identity1 ), JSON_UTF8 ) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( identity1Response, actual );
    }

    @Test
    public void shouldCreateIdentityUsingPhone() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities" )
                .request( JSON_UTF8 )
                .post(Entity.entity( objectMapper.writeValueAsString( identity2 ), JSON_UTF8 ) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( identity2Response, actual );
    }

    @Test
    public void shouldCreateIdentityUsingPhoneAndRegion() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities" )
                .request( JSON_UTF8 )
                .post(Entity.entity( objectMapper.writeValueAsString( identity3 ), JSON_UTF8 ) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( identity3Response, actual );
    }

    @Test
    public void shouldNotCreateIdentityWithGarbageJSON() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities" )
                .request( JSON_UTF8 )
                .post(Entity.entity( objectMapper.writeValueAsString( "Garbage" ), JSON_UTF8 ) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorParsingJSONResponse, actual );
    }

    @Test
    public void shouldNotCreateIdentityWithBadParameters() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities" )
                .request( JSON_UTF8 )
                .post(Entity.entity( objectMapper.writeValueAsString( identityWithTypo ), JSON_UTF8 ) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorBadParametersResponse, actual );
    }

    @Test
    public void shouldNotCreateIdentityWithBadPhoneNumber() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities" )
                .request( JSON_UTF8 )
                .post(Entity.entity( objectMapper.writeValueAsString( identityWithBadPhoneNumber ), JSON_UTF8 ) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorBadPhoneNumberResponse, actual );
    }

    @Test
    public void shouldNotCreateIdentityWithInvalidPhoneNumber() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/identities" )
                .request( JSON_UTF8 )
                .post(Entity.entity( objectMapper.writeValueAsString( identityWithInvalidPhoneNumber ), JSON_UTF8 ) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidPhoneNumberResponse, actual );
    }

    public static final HashMap<String, Object> identity1 =
            new HashMap<String, Object>() {{
                put( "email", "maxdemarzi@gmail.com" );
            }};

    public static final HashMap<String, Object> identity1Response =
            new HashMap<String, Object>() {{
                put( "identity", "maxdemarzi@gmail.com" );
            }};

    public static final HashMap<String, Object> identity2 =
            new HashMap<String, Object>() {{
                put( "phone", "3125137509" );
            }};

    public static final HashMap<String, Object> identity2Response =
            new HashMap<String, Object>() {{
                put( "identity", "+13125137509" );
            }};

    public static final HashMap<String, Object> identity3 =
            new HashMap<String, Object>() {{
                put( "phone", "3125137509" );
                put( "region", "US");
            }};

    public static final HashMap<String, Object> identity3Response =
            new HashMap<String, Object>() {{
                put( "identity", "+13125137509" );
            }};

    public static final HashMap<String, Object> identityWithTypo =
            new HashMap<String, Object>() {{
                put( "typoemail", "maxdemarzi@gmail.com" );
            }};

    public static final HashMap<String, Object> identityWithBadPhoneNumber =
            new HashMap<String, Object>() {{
                put( "phone", "garbage" );
            }};

    public static final HashMap<String, Object> identityWithInvalidPhoneNumber =
            new HashMap<String, Object>() {{
                put( "phone", "000000000" );
            }};

    public static final HashMap<String, Object> errorParsingJSONResponse =
            new HashMap<String, Object>() {{
                put( "error", "Error parsing JSON." );
            }};

    public static final HashMap<String, Object> errorBadParametersResponse =
            new HashMap<String, Object>() {{
                put( "error", "Parameters email or phone required." );
            }};

    public static final HashMap<String, Object> errorBadPhoneNumberResponse =
            new HashMap<String, Object>() {{
                put( "error", "Error Parsing Phone Number." );
            }};

    public static final HashMap<String, Object> errorInvalidPhoneNumberResponse =
            new HashMap<String, Object>() {{
                put( "error", "Invalid Phone Number." );
            }};
}
