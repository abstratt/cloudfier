package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RelatedInstanceJSONRepresentation extends InstanceJSONRepresentation {
    @JsonProperty
    public String relatedUri;
}