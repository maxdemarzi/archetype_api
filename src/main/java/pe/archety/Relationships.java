package pe.archety;

import org.neo4j.graphdb.RelationshipType;

public enum Relationships implements RelationshipType {
    LINK, LIKES, HATES, KNOWS
}