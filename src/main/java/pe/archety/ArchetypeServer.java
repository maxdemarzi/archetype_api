package pe.archety;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import pe.archety.handlers.admin.*;
import pe.archety.handlers.api.GetIdentityHandler;
import pe.archety.handlers.api.PostConceptHandler;

public class ArchetypeServer {

    public static final String JSON_UTF8 = MediaType.JSON_UTF_8.toString();
    public static final String TEXT_PLAIN = MediaType.PLAIN_TEXT_UTF_8.toString();
    public static final String HTML_UTF8 = MediaType.HTML_UTF_8.toString();

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String STOREDIR = "/Users/maxdemarzi/Projects/archetype_api/neo4j/data/graph.db";
    private static final String CONFIG = "/Users/maxdemarzi/Projects/archetype_api/neo4j/conf/neo4j.properties";

    static GraphDatabaseService graphDb = new GraphDatabaseFactory()
            .newEmbeddedDatabaseBuilder( STOREDIR )
            .loadPropertiesFromFile( CONFIG )
            .newGraphDatabase();


    public static void main(final String[] args) {
        registerShutdownHook(graphDb);

        // Administrative server accessible internally only
        Undertow.builder()
                .addHttpListener(8079, "archety.pe")
                .setBufferSize(1024 * 16)
                .setIoThreads(Runtime.getRuntime().availableProcessors() * 2) //this seems slightly faster in some configurations
                .setHandler(new RoutingHandler()
                                .add("GET", "/", new HelloWorldHandler())
                                .add("GET", "/v1/admin/warmup", new WarmUpHandler(graphDb))
                                .add("GET", "/v1/admin/initialize", new InitializeHandler(graphDb))
                                .add("GET", "/v1/admin/wikipedia", new WikipediaHandler(graphDb))
                                .add("GET", "/v1/hello/{name}", new HelloNameHandler())
                )
                .setWorkerThreads(200).build().start();

        // Public API
        Undertow.builder()
                .addHttpListener(8080, "archety.pe")
                .setBufferSize(1024 * 16)
                .setIoThreads(Runtime.getRuntime().availableProcessors() * 2) //this seems slightly faster in some configurations
                .setHandler(new RoutingHandler()
                                .add("GET", "/v1/identities/{identity}", new GetIdentityHandler(graphDb))
                                .add("POST", "/v1/pages/{url}", new PostConceptHandler(graphDb))
                )
                .setWorkerThreads(200).build().start();


    }

    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }
}
