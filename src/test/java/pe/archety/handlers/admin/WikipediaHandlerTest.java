package pe.archety.handlers.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.test.TestGraphDatabaseFactory;
import pe.archety.ArchetypeConstants;
import pe.archety.Labels;
import pe.archety.Relationships;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class WikipediaHandlerTest {
    private static GraphDatabaseService db;
    private static Undertow undertow;
    private static final JerseyClient client = JerseyClientBuilder.createClient();

    @Before
    public void setUp() throws JsonProcessingException {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        populateDb(db);
        undertow = Undertow.builder()
                .addHttpListener(9090, "localhost")
                .setHandler(new RoutingHandler()
                                .add( "GET", "/v1/admin/wikipedia", new WikipediaHandler( db ) )
                )
                .build();
        undertow.start();

    }

    private void populateDb(GraphDatabaseService db) {
        try (Transaction tx = db.beginTx()) {
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
        //pageNode.setProperty("url", ArchetypeConstants.URLPREFIX + title);
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
    public void shouldPrepareWikipedia() throws IOException {
        Response response = client.target("http://localhost:9090")
                .register(HashMap.class)
                .path("/v1/admin/wikipedia")
                .request()
                .get();

        int code = response.getStatus();
        String actual = response.readEntity(String.class);

        assertEquals(200, code);
        assertEquals("Wikipedia Data Ready!", actual);

        try (Transaction tx = db.beginTx() ) {

            final Node page1 = IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(Labels.Page, "title", "Neo4j"));
            final Node page2 = IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(Labels.Page, "title", "Mongodb"));

            assertEquals(ArchetypeConstants.URLPREFIX + "Neo4j", page1.getProperty("url"));
            assertEquals(ArchetypeConstants.URLPREFIX + "Mongodb", page2.getProperty("url"));

        }
    }
}