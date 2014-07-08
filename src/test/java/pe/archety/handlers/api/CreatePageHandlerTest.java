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
import static pe.archety.ArchetypeConstants.URLPREFIX;
import static pe.archety.ArchetypeServer.JSON_UTF8;

public class CreatePageHandlerTest {
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
                                .add( "POST", "/v1/pages", new CreatePageHandler( db, objectMapper ) )
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
    public void shouldCreatePageUsingURL() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/pages" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(page1), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( page1Response, actual );
    }

    @Test
    public void shouldCreatePageUsingTitle() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/pages" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(page2), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( page2Response, actual );
    }

    @Test
    public void shouldCreatePageUsingURLandTitle() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/pages" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(page3), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( page3Response, actual );
    }

    @Test
    public void shouldCreatePageWithSpaces() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/pages" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(page4), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 201, code );
        assertEquals( page4Response, actual );
    }

    @Test
    public void shouldNotCreatePageUsingInvalidURLPrefix() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/pages" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(pageWithInvalidURLPrefix), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidURLPrefixResponse, actual );
    }

    @Test
    public void shouldNotCreatePageUsingInvalidURL() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/pages" )
                .request( JSON_UTF8 )
                .post(Entity.entity(objectMapper.writeValueAsString(pageWithInvalidURL), JSON_UTF8) );

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorInvalidURLResponse, actual );
    }

    public static final HashMap<String, Object> page1 =
            new HashMap<String, Object>() {{
                put( "url", "http://en.wikipedia.org/wiki/Neo4j" );
            }};

    public static final HashMap<String, Object> page1Response =
            new HashMap<String, Object>() {{
                put( "title", "Neo4j");
                put( "url", "http://en.wikipedia.org/wiki/Neo4j" );
            }};

    public static final HashMap<String, Object> page2 =
            new HashMap<String, Object>() {{
                put( "title", "Chicago" );
            }};

    public static final HashMap<String, Object> page2Response =
            new HashMap<String, Object>() {{
                put( "title", "Chicago");
                put( "url", "http://en.wikipedia.org/wiki/Chicago" );
            }};

    public static final HashMap<String, Object> page3 =
            new HashMap<String, Object>() {{
                put( "title", "Peru" );
                put( "url", "http://en.wikipedia.org/wiki/Peru" );
            }};

    public static final HashMap<String, Object> page3Response =
            new HashMap<String, Object>() {{
                put( "title", "Peru");
                put( "url", "http://en.wikipedia.org/wiki/Peru" );
            }};

    public static final HashMap<String, Object> page4 =
            new HashMap<String, Object>() {{
                put( "title", "Machu Picchu" );
            }};

    public static final HashMap<String, Object> page4Response =
            new HashMap<String, Object>() {{
                put( "title", "Machu Picchu");
                put( "url", "http://en.wikipedia.org/wiki/Machu_Picchu" );
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
