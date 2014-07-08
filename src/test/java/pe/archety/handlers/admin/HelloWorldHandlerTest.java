package pe.archety.handlers.admin;

import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class HelloWorldHandlerTest {
    private static Undertow undertow;
    private static final JerseyClient client = JerseyClientBuilder.createClient();

    @Before
    public void setUp() {
        undertow = Undertow.builder()
                .addHttpListener( 9090, "localhost" )
                .setHandler(new RoutingHandler()
                                .add( "GET", "/", new HelloWorldHandler() )
                )
                .build();
        undertow.start();
    }

    @After
    public void tearDown() throws Exception {
        undertow.stop();
    }

    @Test
    public void shouldSayHelloWorld() throws IOException {
        Response response = client.target( "http://localhost:9090" )
                .register( HashMap.class )
                .path( "/" )
                .request()
                .get();

        int code = response.getStatus();
        String actual =  response.readEntity( String.class );

        assertEquals( 200, code );
        assertEquals( "Hello, World!", actual );
    }
}