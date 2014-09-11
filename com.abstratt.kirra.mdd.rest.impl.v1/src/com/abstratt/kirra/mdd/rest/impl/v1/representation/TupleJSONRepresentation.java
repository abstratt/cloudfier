package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

public class TupleJSONRepresentation {

    @JsonProperty
    public String typeName;

    @JsonProperty
    public Map<String, Object> values;
}
