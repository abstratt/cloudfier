package com.abstratt.kirra.mdd.rest.representation;

import org.codehaus.jackson.annotate.JsonProperty;

public class EntityLinkJSONRepresentation {
    @JsonProperty
    public String description;
    public String extentUri;
    @JsonProperty
    public String label;
    @JsonProperty
    public String name;
    @JsonProperty
    public String namespace;
    public Boolean topLevel;
    @JsonProperty
    public String uri;
}