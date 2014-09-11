package com.abstratt.kirra.mdd.rest.representation;

import org.codehaus.jackson.annotate.JsonProperty;

public class ServiceLinkJSONRepresentation {
    @JsonProperty
    public String name;
    @JsonProperty
    public String namespace;
    @JsonProperty
    public String uri;
}