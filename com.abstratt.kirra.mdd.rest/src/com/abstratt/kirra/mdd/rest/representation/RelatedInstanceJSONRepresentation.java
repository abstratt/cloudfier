package com.abstratt.kirra.mdd.rest.representation;

import org.codehaus.jackson.annotate.JsonProperty;

public class RelatedInstanceJSONRepresentation extends InstanceJSONRepresentation {
    @JsonProperty
    public String relatedUri;
}