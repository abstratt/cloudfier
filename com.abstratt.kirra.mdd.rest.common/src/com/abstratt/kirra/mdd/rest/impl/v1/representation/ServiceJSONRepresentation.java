package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class ServiceJSONRepresentation {
    @JsonProperty
    public List<ActionJSONRepresentation> events = new ArrayList<ActionJSONRepresentation>();
    @JsonProperty
    public String name;
    @JsonProperty
    public String namespace;
    @JsonProperty
    public List<QueryJSONRepresentation> queries = new ArrayList<QueryJSONRepresentation>();
    @JsonProperty
    public String rootUri;
    @JsonProperty
    public String uri;
}