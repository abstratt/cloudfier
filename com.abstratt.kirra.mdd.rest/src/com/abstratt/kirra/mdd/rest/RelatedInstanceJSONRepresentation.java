package com.abstratt.kirra.mdd.rest;

import org.codehaus.jackson.annotate.JsonProperty;

public class RelatedInstanceJSONRepresentation extends InstanceJSONRepresentation {
	@JsonProperty
	public String relatedUri;
}