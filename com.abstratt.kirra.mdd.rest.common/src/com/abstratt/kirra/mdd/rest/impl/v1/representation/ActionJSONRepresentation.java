package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import org.codehaus.jackson.annotate.JsonProperty;

public class ActionJSONRepresentation {
    @JsonProperty
    public String name;

    @JsonProperty
    public String uri;

    public Boolean enabled;

    public boolean instance;
}
