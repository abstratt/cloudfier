package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RelationshipJSONRepresentation {
    @JsonProperty
    public String name;
    @JsonProperty
    public String type;
    @JsonProperty
    public String typeUri;
}
