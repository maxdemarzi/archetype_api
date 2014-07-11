package pe.archety;

import com.google.common.util.concurrent.AbstractScheduledService;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.security.SecureRandom;

import static pe.archety.ArchetypeConstants.DATA;
import static pe.archety.ArchetypeConstants.ACTION;

public class BatchWriterService extends AbstractScheduledService {

    private final static Logger logger = Logger.getLogger( BatchWriterService.class );
    private static final PathExpander LIKES_EXPANDER = PathExpanders.forTypeAndDirection( Relationships.LIKES, Direction.OUTGOING );
    private static final PathFinder<Path> ONE_HOP_LIKES_PATH = GraphAlgoFactory.shortestPath( LIKES_EXPANDER, 1 );
    private static final PathExpander HATES_EXPANDER = PathExpanders.forTypeAndDirection( Relationships.HATES, Direction.OUTGOING );
    private static final PathFinder<Path> ONE_HOP_HATES_PATH = GraphAlgoFactory.shortestPath( HATES_EXPANDER, 1 );
    private static final PathExpander KNOWS_EXPANDER = PathExpanders.forTypeAndDirection( Relationships.KNOWS, Direction.OUTGOING );
    private static final PathFinder<Path> ONE_HOP_KNOWS_PATH = GraphAlgoFactory.shortestPath( KNOWS_EXPANDER, 1 );
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private GraphDatabaseService graphDb;
    public LinkedBlockingQueue<HashMap<String, Object>> queue = new LinkedBlockingQueue<>();

    public void SetGraphDatabase(GraphDatabaseService graphDb){
        this.graphDb = graphDb;
    }

    public final static BatchWriterService INSTANCE = new BatchWriterService();
    private BatchWriterService() {
        if ( !this.isRunning() ){
            logger.info( "Starting BatchWriterService" );
            this.startAsync();
            this.awaitRunning();
            logger.info( "Started BatchWriterService" );
        }
    }

    @Override
    protected void runOneIteration() throws Exception {
        long startTime = System.nanoTime();
        long transactionTime = System.nanoTime();
        Collection<HashMap<String, Object>> writes = new ArrayList<>();
        queue.drainTo( writes );

        if(!writes.isEmpty()){
            int i = 0;
            Transaction tx = graphDb.beginTx();
            try {
                for( HashMap write : writes ){
                    try {
                        i++;
                        if ( write.get( DATA ) == null) {
                            logger.error( "BatchWriterService received write without data:" + write.toString() );
                            continue;
                        }
                        switch ((BatchWriterServiceAction) write.get( ACTION )) {
                            case CREATE_IDENTITY: {
                                createIdentity((String)((HashMap)write.get( DATA )).get( "identityHash" ));
                                break;
                            }
                            case CREATE_IDENTITY_AND_LIKES_RELATIONSHIP: {
                                Node identityNode = createIdentity((String)((HashMap)write.get( DATA )).get( "identityHash" ));
                                Node pageNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get( "pageNodeId" ));
                                CreateLikesRelationship(identityNode, pageNode);
                                break;
                            }
                            case CREATE_IDENTITY_AND_HATES_RELATIONSHIP: {
                                Node identityNode = createIdentity((String)((HashMap)write.get( DATA )).get( "identityHash" ));
                                Node pageNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get( "pageNodeId" ));
                                CreateHatesRelationship(identityNode, pageNode);
                                break;
                            }

                            case CREATE_IDENTITY_WITH_TOKEN: {
                                Node identityNode = createIdentity((String)((HashMap)write.get( DATA )).get( "identityHash" ));
                                identityNode.setProperty( "generatedToken", createToken() );
                                emailTokenAsync((String)((HashMap)write.get( DATA )).get( "identity" ) );
                                break;
                            }

                            case CREATE_TOKEN: {
                                Node identityNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get( "identityNodeId" ));
                                identityNode.setProperty( "generatedToken", createToken() );
                                emailTokenAsync((String)((HashMap)write.get( DATA )).get( "identity" ) );
                                break;
                            }

                            case CREATE_PAGE: {
                                createPage(write);
                                break;
                            }
                            case CREATE_PAGE_AND_LIKES_RELATIONSHIP: {
                                Node identityNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get( "identityNodeId" ));
                                Node pageNode = createPage(write);
                                CreateLikesRelationship(identityNode, pageNode);
                                break;
                            }
                            case CREATE_PAGE_AND_HATES_RELATIONSHIP: {
                                Node identityNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get( "identityNodeId" ));
                                Node pageNode = createPage(write);
                                CreateHatesRelationship(identityNode, pageNode);
                                break;
                            }

                            case CREATE_BOTH_AND_LIKES_RELATIONSHIP: {
                                Node identityNode = createIdentity((String)((HashMap)write.get( DATA )).get( "identityHash" ));
                                Node pageNode = createPage(write);
                                CreateLikesRelationship(identityNode, pageNode);
                                break;
                            }
                            case CREATE_BOTH_AND_HATES_RELATIONSHIP: {
                                Node identityNode = createIdentity((String)((HashMap)write.get( DATA )).get( "identityHash" ));
                                Node pageNode = createPage(write);
                                CreateHatesRelationship(identityNode, pageNode);
                                break;
                            }

                            case CREATE_LIKES_RELATIONSHIP: {
                                Node identityNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get( "identityNodeId" ));
                                Node pageNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get( "pageNodeId" ));
                                CreateLikesRelationship(identityNode, pageNode);
                                break;
                            }
                            case CREATE_HATES_RELATIONSHIP: {
                                Node identityNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get( "identityNodeId" ));
                                Node pageNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get( "pageNodeId" ));
                                CreateHatesRelationship(identityNode, pageNode);
                                break;
                            }

                            case CREATE_BOTH_AND_KNOWS_RELATIONSHIP: {
                                Node identityNode = createIdentity((String)((HashMap)write.get( DATA )).get( "identityHash" ));
                                Node identityNode2 = createIdentity((String)((HashMap)write.get( DATA )).get( "identityHash2" ));
                                CreateKnowsRelationship( identityNode, identityNode2, (String)((HashMap)write.get( DATA )).get( "encryptedIdentity" ) );
                                break;
                            }
                            case CREATE_IDENTITY_AND_KNOWS_RELATIONSHIP: {
                                Node identityNode = createIdentity((String)((HashMap)write.get( DATA )).get( "identityHash" ));
                                Node identityNode2 = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get("identityNodeId2"));
                                CreateKnowsRelationship( identityNode, identityNode2, (String)((HashMap)write.get( DATA )).get( "encryptedIdentity" ) );
                                break;
                            }
                            case CREATE_2ND_IDENTITY_AND_KNOWS_RELATIONSHIP: {
                                Node identityNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get("identityNodeId"));
                                Node identityNode2 = createIdentity((String)((HashMap)write.get( DATA )).get( "identityHash2" ));
                                CreateKnowsRelationship( identityNode, identityNode2, (String)((HashMap)write.get( DATA )).get( "encryptedIdentity" ) );
                                break;
                            }
                            case CREATE_KNOWS_RELATIONSHIP: {
                                Node identityNode = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get("identityNodeId"));
                                Node identityNode2 = graphDb.getNodeById((Long) ((HashMap) write.get(DATA)).get("identityNodeId2"));
                                CreateKnowsRelationship( identityNode, identityNode2, (String)((HashMap)write.get( DATA )).get( "encryptedIdentity" ) );
                                break;
                            }

                        }

                    } catch ( Exception exception ) {
                        logger.error( "Error in Write: " + write );
                    }

                    if(i % 40000 == 0){
                        tx.success();
                        tx.close();
                        DateTime currently = new DateTime();
                        System.out.printf( "Performed a transaction of 40000 writes in  %d [msec] @ %s \n", ( System.nanoTime() - transactionTime ) / 1000000, currently.toDateTimeISO() );
                        transactionTime = System.nanoTime();
                        tx = graphDb.beginTx();
                    }
                }

                tx.success();
            } finally {
                tx.close();
                DateTime currently = new DateTime();
                System.out.printf("Performed a set of transactions with %d writes in  %d [msec] @ %s \n", writes.size(), (System.nanoTime() - startTime) / 1000000, currently.toDateTimeISO());
            }
        }
    }

    private void emailTokenAsync(String identity) {
        // TODO: Create me
    }

    private String createToken() {
        byte bytes[] = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return String.format( "%x", new BigInteger(bytes)).substring( 0, 64 );
    }

    private void CreateLikesRelationship(Node identityNode, Node pageNode) {
        Relationship rel;

        org.neo4j.graphdb.Path relPath = ONE_HOP_LIKES_PATH.findSinglePath(identityNode, pageNode);
        if (relPath == null) {
            rel = identityNode.createRelationshipTo(pageNode, Relationships.LIKES);
        } else {
            rel = relPath.lastRelationship();
        }
    }

    private void CreateHatesRelationship(Node identityNode, Node pageNode) {
        Relationship rel;

        org.neo4j.graphdb.Path relPath = ONE_HOP_HATES_PATH.findSinglePath(identityNode, pageNode);
        if (relPath == null) {
            rel = identityNode.createRelationshipTo(pageNode, Relationships.HATES);
        } else {
            rel = relPath.lastRelationship();
        }
    }

    private void CreateKnowsRelationship(Node identityNode, Node identityNode2, String encryptedIdentity) {
        Relationship rel;

        org.neo4j.graphdb.Path relPath = ONE_HOP_KNOWS_PATH.findSinglePath(identityNode, identityNode2);
        if (relPath == null) {
            rel = identityNode.createRelationshipTo(identityNode2, Relationships.KNOWS);
            rel.setProperty("encryptedIdentity", encryptedIdentity);
        } else {
            rel = relPath.lastRelationship();
        }
    }

    private Node createIdentity(String identityHash) {
        UniqueFactory.UniqueNodeFactory identityFactory = getUniqueIdentityFactory( graphDb );
        Node identityNode = identityFactory.getOrCreate( "identity", identityHash );
        ArchetypeServer.identityCache.put(identityHash, identityNode.getId());
        return identityNode;
    }

    private Node createPage(HashMap write) {
        Node pageNode = null;
        if (((HashMap) write.get(DATA)).containsKey("url") &&
                ((HashMap) write.get(DATA)).containsKey("title")) {
            String url = (String) ((HashMap) write.get(DATA)).get("url");
            String title = (String) ((HashMap) write.get(DATA)).get("title");
            UniqueFactory.UniqueNodeFactory pageFactory = getUniquePageFactory(graphDb);
            pageNode = pageFactory.getOrCreate("url", url);
            if (!pageNode.hasProperty("title")) {
                pageNode.setProperty("title", title);
            }
            ArchetypeServer.urlCache.put(url, pageNode.getId());
        }
        return pageNode;
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.SECONDS);
    }

    private UniqueFactory.UniqueNodeFactory getUniquePageFactory(final GraphDatabaseService db) {
        return new UniqueFactory.UniqueNodeFactory( db, Labels.Page.name() )
        {
            @Override
            protected void initialize( Node created, Map<String, Object> properties )
            {
                created.addLabel( Labels.Page );
                created.setProperty( "url", properties.get( "url" ) );
            }
        };
    }

    private UniqueFactory.UniqueNodeFactory getUniqueIdentityFactory(final GraphDatabaseService db) {
        return new UniqueFactory.UniqueNodeFactory( db, Labels.Identity.name() )
        {
            @Override
            protected void initialize( Node created, Map<String, Object> properties )
            {
                created.addLabel( Labels.Identity );
                created.setProperty( "identity", properties.get( "identity" ) );
            }
        };
    }
}