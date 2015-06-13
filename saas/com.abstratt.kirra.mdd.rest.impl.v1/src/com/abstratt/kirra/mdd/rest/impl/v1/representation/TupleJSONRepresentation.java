package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TupleJSONRepresentation {

    @JsonProperty
    public String typeName;

    @JsonProperty
    public Map<String, Object> values;
}
