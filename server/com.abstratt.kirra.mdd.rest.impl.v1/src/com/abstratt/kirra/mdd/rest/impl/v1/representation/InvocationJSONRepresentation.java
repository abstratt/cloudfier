package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InvocationJSONRepresentation {
    @JsonProperty
    public Map<String, Object> arguments;
}