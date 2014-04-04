package com.abstratt.kirra.mdd.rest;

import org.codehaus.jackson.annotate.JsonProperty;

public class EntityLinkJSONRepresentation {
	@JsonProperty
	public String namespace;
	@JsonProperty
	public String name;
	@JsonProperty
	public String uri;
	public String extentUri;
}