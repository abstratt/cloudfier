package com.abstratt.kirra.mdd.rest;

import org.codehaus.jackson.annotate.JsonProperty;

public class QueryJSONRepresentation {
    @JsonProperty
    public String name;

    @JsonProperty
    public String uri;
}
