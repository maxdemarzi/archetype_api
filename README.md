Archetype API
=============

Archetype API

## Instructions

Edit the ArchetypeServer.java file to point to your graph.db directory:

    private static final String STOREDIR = "your graph.db directory ";
    private static final String CONFIG = "your neo4j.properties file in conf directory";

Compile it and run it:

    mvn clean compile assembly:single
    java -jar target/ArchetypeServer-jar-with-dependencies.jar

Go to http://localhost:8080 and you should get a "Hello World".

    /admin/warmup
    
    /identities/{email}
