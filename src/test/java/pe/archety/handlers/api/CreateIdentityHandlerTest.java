package pe.archety.handlers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import org.glassfish.jersey.client.*;

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
    public void shouldCreateIdentity() throws IOException {
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

    public static final HashMap<String, Object> identity1 =
            new HashMap<String, Object>() {{
                put( "email", "maxdemarzi@gmail.com" );
            }};

    public static final HashMap<String, Object> identity1Response =
            new HashMap<String, Object>() {{
                put( "identity", "maxdemarzi@gmail.com" );
            }};

}
