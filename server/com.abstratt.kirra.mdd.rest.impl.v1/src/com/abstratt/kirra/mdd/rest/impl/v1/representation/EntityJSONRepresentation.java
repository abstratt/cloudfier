package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import java.util.ArrayList;
import java.util.List;

import com.abstratt.kirra.Property;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityJSONRepresentation {
    @JsonProperty
    public List<ActionJSONRepresentation> actions = new ArrayList<ActionJSONRepresentation>();
    @JsonProperty
    public String description;
    @JsonProperty
    public List<QueryJSONRepresentation> finders = new ArrayList<QueryJSONRepresentation>();
    @JsonProperty
    public String instances;
    @JsonProperty
    public String label;
    @JsonProperty
    public String name;
    @JsonProperty
    public String namespace;
    @JsonProperty
    public List<Property> properties = new ArrayList<Property>();
    public List<RelationshipJSONRepresentation> relationships = new ArrayList<RelationshipJSONRepresentation>();
    @JsonProperty
    public String rootUri;
    public String template;
    public boolean topLevel;
    @JsonProperty
    public String uri;
}