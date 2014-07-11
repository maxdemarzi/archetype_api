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
    GET     /v1/hello/{name}                  # Example Path Parameters
    GET     /v1/hi?name={name}                # Example Query Parameters
    GET     /v1/admin/warmup                  # Warm up database
    GET     /v1/admin/wikipedia               # Add url property to Pages
    GET     /v1/admin/initialize              # Create Indexes
    
    
Go to http://localhost:8080 for API:    
    
    GET     /v1/identities/{identity}                   # Get profile of identity
    POST    /v1/identities                              # Create identity (takes email/phone+region(opt) as parameters)
    POST    /v1/identities/{identity}/likes             # Create a likes relationship between identity and page (param)
    GET     /v1/identities/{identity}/likes             # Get likes for identity
    POST    /v1/identities/{identity}/hates             # Create a hates relationship between identity and page (param)
    GET     /v1/identities/{identity}/hates             # Get hates for identity
    POST    /v1/identities/{identity}/knows             # Create a knows relationship between two identities (param)
    GET     /v1/identities/{identity}/knows             # Get knows for identity

    POST    /v1/pages                                   # Create page (takes title or url) as parameter

    POST    /v1/tokens                                  # Create a new potential token (takes e-mail/phone)
    GET     /v1/tokens/{signature}                      # Get an identity token given signature
     
TODO:
    
    Identity Registration
    Wall/Events/Posts (?)