package com.abstratt.kirra.mdd.rest.representation;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

public class InvocationJSONRepresentation {
    @JsonProperty
    public Map<String, Object> arguments;
}