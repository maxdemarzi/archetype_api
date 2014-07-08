package pe.archety.handlers.admin;

import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class WarmUpHandlerTest  {
    private static GraphDatabaseService db;
    private static Undertow undertow;
    private static final JerseyClient client = JerseyClientBuilder.createClient();

    @Before
    public void setUp() {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        undertow = Undertow.builder()
                .addHttpListener( 9090, "localhost" )
                .setHandler(new RoutingHandler()
                                .add( "GET", "/v1/admin/warmup", new WarmUpHandler( db ) )
                )
                .build();
        undertow.start();
    }

    @After
    public void tearDown() throws Exception {
        undertow.stop();
    }

    @Test
    public void shouldWarmUp() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/v1/admin/warmup" )
                .request()
                .get();

        int code = response.getStatus();
        String actual =  response.readEntity( String.class );

        assertEquals( 200, code );
        assertEquals( "Warmed up and ready to go!", actual );
    }
}
