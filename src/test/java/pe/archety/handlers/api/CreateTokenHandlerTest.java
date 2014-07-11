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

public class CreateTokenHandlerTest {
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
                                .add( "POST", "/v1/tokens", new CreateTokenHandler( db, objectMapper ) )
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
    public void shouldCreateTokenUsingEmail() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/tokens" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(identity1), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( identity1Response, actual );
    }

    @Test
    public void shouldNotCreateTokenUsingPhone() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/tokens" )
                .request( JSON_UTF8 )
                .post(Entity.entity( objectMapper.writeValueAsString( identity2 ), JSON_UTF8 ) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorEmailParameterResponse, actual );
    }

    @Test
    public void shouldNotCreateTokenWithGarbageJSON() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/tokens" )
                .request( JSON_UTF8 )
                .post(Entity.entity( objectMapper.writeValueAsString( "Garbage" ), JSON_UTF8 ) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorParsingJSONResponse, actual );
    }

    @Test
    public void shouldNotCreateTokenWithBadParameters() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/tokens" )
                .request( JSON_UTF8 )
                .post(Entity.entity( objectMapper.writeValueAsString( identityWithTypo ), JSON_UTF8 ) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorEmailParameterResponse, actual );
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

    public static final HashMap<String, Object> identityWithTypo =
            new HashMap<String, Object>() {{
                put( "typoemail", "maxdemarzi@gmail.com" );
            }};

    public static final HashMap<String, Object> errorParsingJSONResponse =
            new HashMap<String, Object>() {{
                put( "error", "Error parsing JSON." );
            }};

    public static final HashMap<String, Object> errorEmailParameterResponse =
            new HashMap<String, Object>() {{
                put( "error", "Email parameter required." );
            }};

}