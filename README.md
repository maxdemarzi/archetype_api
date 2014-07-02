Archetype API
=============

Archetype API

## Instructions

Edit the ArchetypeServer.java file to point to your graph.db directory:

    private static final String STOREDIR = "your graph.db directory ";
    private static final String CONFIG = "your neo4j.properties file in conf directory";

Compile it and run it:

    mvn clean package
    java -jar target/ArchetypeServer-jar-with-dependencies.jar

Go to http://localhost:8079 for administrative interface:

    GET     /                                 # Hello World
    GET     /v1/hello/{name}                  # Example Parameters
    GET     /v1/admin/warmup                  # Warm up database
    GET     /v1/admin/wikipedia               # Add url property to Pages
    GET     /v1/admin/initialize              # Create Indexes
    
    
Go to http://localhost:8080 for API:    
    
    GET     /v1/identities/{identity}         # Get profile of identity
    POST    /v1/identities                    # Create identity (takes email/phone+region(opt) as parameters)
    POST    /v1/pages
