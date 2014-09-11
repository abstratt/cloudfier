package com.abstratt.kirra.mdd.rest.impl.v1.representation;

import org.codehaus.jackson.annotate.JsonProperty;

public class RelatedInstanceJSONRepresentation extends InstanceJSONRepresentation {
    @JsonProperty
    public String relatedUri;
}