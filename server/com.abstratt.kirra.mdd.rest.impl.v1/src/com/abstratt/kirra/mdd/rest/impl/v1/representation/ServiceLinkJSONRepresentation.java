package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceLinkJSONRepresentation {
    @JsonProperty
    public String name;
    @JsonProperty
    public String namespace;
    @JsonProperty
    public String uri;
}