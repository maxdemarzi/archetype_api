package pe.archety;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.MediaType;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.HighlyAvailableGraphDatabaseFactory;
import pe.archety.handlers.admin.*;
import pe.archety.handlers.api.*;

public class ArchetypeServer {

    public static final String JSON_UTF8 = MediaType.JSON_UTF_8.toString();
    public static final String TEXT_PLAIN = MediaType.PLAIN_TEXT_UTF_8.toString();

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String STOREDIR = "/home/shroot/graphipedia/neo4j/data/graph.db";
    private static final String CONFIG = "/home/shroot/graphipedia/neo4j/conf/neo4j.properties";

    private static GraphDatabaseService graphDb;

    private static final BatchWriterService batchWriterService = BatchWriterService.INSTANCE;

    public static final Cache<String, Long> identityCache = CacheBuilder.newBuilder().maximumSize( 10_000_000 ).build();
    public static final Cache<String, Long> urlCache = CacheBuilder.newBuilder().maximumSize( 11_000_000 ).build();

    public static void main(final String[] args) {
        graphDb = new HighlyAvailableGraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder( STOREDIR )
                .loadPropertiesFromFile( CONFIG )
                .newGraphDatabase();

        registerShutdownHook( graphDb );

        batchWriterService.SetGraphDatabase( graphDb );

        // Administrative server accessible internally only
        Undertow.builder()
                .addHttpListener( 8079, "archety.pe" )
                .setBufferSize( 1024 * 16 )
                .setIoThreads( Runtime.getRuntime().availableProcessors() * 2 ) //this seems slightly faster in some configurations
                .setHandler( new RoutingHandler()
                                .add( "GET", "/", new HelloWorldHandler() )
                                .add( "GET", "/v1/admin/warmup", new WarmUpHandler( graphDb ) )
                                .add( "GET", "/v1/admin/initialize", new InitializeHandler( graphDb ) )
                                .add( "GET", "/v1/admin/wikipedia", new WikipediaHandler( graphDb ) )
                                .add( "GET", "/v1/hello/{name}", new HelloNameHandler() )
                                .add( "GET", "/v1/hi", new HiNameHandler() )
                )
                .setWorkerThreads( 200 ).build().start();

        // Public API
        Undertow.builder()
                .addHttpListener( 8080, "archety.pe" )
                .setBufferSize( 1024 * 16 )
                .setIoThreads(Runtime.getRuntime().availableProcessors() * 2) //this seems slightly faster in some configurations
                .setHandler(new RoutingHandler()
                                .add( "GET",  "/v1/identities/{identity}", new GetIdentityHandler( graphDb, objectMapper ) )
                                .add( "POST", "/v1/identities", new CreateIdentityHandler( graphDb, objectMapper ) )
                                .add( "POST", "/v1/identities/{identity}/likes", new CreateLikesOrHatesHandler( graphDb, objectMapper, Relationships.LIKES.name() ) )
                                .add( "GET", "/v1/identities/{identity}/likes", new GetLikesOrHatesHandler( graphDb, objectMapper, Relationships.LIKES ) )
                                .add( "POST", "/v1/identities/{identity}/hates", new CreateLikesOrHatesHandler( graphDb, objectMapper, Relationships.HATES.name() ) )
                                .add( "GET", "/v1/identities/{identity}/hates", new GetLikesOrHatesHandler( graphDb, objectMapper, Relationships.HATES ) )
                                .add( "POST", "/v1/identities/{identity}/knows", new CreateKnowsHandler( graphDb, objectMapper ) )
                                .add( "GET", "/v1/identities/{identity}/knows", new GetKnowsHandler( graphDb, objectMapper ) )
                                .add( "POST", "/v1/pages", new CreatePageHandler( graphDb, objectMapper ) )
                                .add( "POST", "/v1/tokens", new CreateTokenHandler( graphDb, objectMapper ) )
                                .add( "GET",  "/v1/tokens/{token}", new GetTokenHandler( graphDb ) )
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
