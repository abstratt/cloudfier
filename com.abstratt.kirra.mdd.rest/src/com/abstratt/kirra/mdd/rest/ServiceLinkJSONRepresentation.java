package com.abstratt.kirra.mdd.rest;

import org.codehaus.jackson.annotate.JsonProperty;

public class ServiceLinkJSONRepresentation {
	@JsonProperty
	public String namespace;
	@JsonProperty
	public String name;
	@JsonProperty
	public String uri;
}