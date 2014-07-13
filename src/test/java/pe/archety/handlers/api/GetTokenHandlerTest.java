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
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;
import pe.archety.ArchetypeConstants;
import pe.archety.ArchetypeServer;
import pe.archety.Labels;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.security.SecureRandom;

import static org.junit.Assert.assertEquals;
import static pe.archety.ArchetypeServer.JSON_UTF8;

public class GetTokenHandlerTest {
    private static GraphDatabaseService db;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Undertow undertow;
    private static final JerseyClient client = JerseyClientBuilder.createClient();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String token1 = createToken();
    private static final String token2 = createToken();
    private static final String badtoken = createToken();

    @Before
    public void setUp() throws Exception {
        ArchetypeServer.identityCache.invalidateAll();
        ArchetypeServer.urlCache.invalidateAll();

        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        populateDb(db);
        undertow = Undertow.builder()
                .addHttpListener( 9090, "localhost" )
                .setHandler(new RoutingHandler()
                                .add( "GET", "/v1/tokens/{token}", new GetTokenHandler( db ))
                )
                .build();
        undertow.start();

    }

    private void populateDb(GraphDatabaseService db) throws Exception {
        try ( Transaction tx = db.beginTx() ) {
            Node identity1Node = createIdentity(db, "maxdemarzi@gmail.com");
            Node identity2Node = createIdentity(db, "max@maxdemarzi.com");
            identity1Node.setProperty( "generatedToken", token1 );
            identity2Node.setProperty( "generatedToken", token2 );
            identity2Node.setProperty( "authenticatedToken", token2 );
            tx.success();
        }
    }

    private Node createIdentity(GraphDatabaseService db, String identity) {
        Node identityNode = db.createNode(Labels.Identity);
        String identityHash = ArchetypeConstants.calculateHash(identity);
        identityNode.setProperty("identity", identityHash);
        return identityNode;
    }

    private static String createToken() {
        byte bytes[] = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return String.format( "%x", new BigInteger(bytes)).substring( 0, 64 );
    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();
        undertow.stop();
    }

    @Test
    public void shouldGetTokenUsingEmail() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/tokens/" + token1 )
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();
        String actual =  response.readEntity( String.class );

        assertEquals( 200, code );
        assertEquals( "Token Authenticated!", actual );
    }

    @Test
    public void shouldNotAuthenticateWithBadToken() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/tokens/" + badtoken )
                .request(JSON_UTF8)
                .get();

        int code = response.getStatus();
        HashMap actual = objectMapper.readValue( response.readEntity( String.class ), HashMap.class );

        assertEquals( 400, code );
        assertEquals( errorAuthenticatingTokenResponse, actual );
    }

    public static final HashMap<String, Object> errorAuthenticatingTokenResponse =
            new HashMap<String, Object>() {{
                put( "error", "Error authenticating token." );
            }};
}
