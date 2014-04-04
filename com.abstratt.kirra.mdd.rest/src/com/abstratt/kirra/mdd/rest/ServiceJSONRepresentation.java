package com.abstratt.kirra.mdd.rest;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class ServiceJSONRepresentation {
	@JsonProperty
	public String namespace;
	@JsonProperty
	public String name;
	@JsonProperty
	public String uri;
	@JsonProperty
	public String rootUri;
	@JsonProperty
	public List<ActionJSONRepresentation> events = new ArrayList<ActionJSONRepresentation>();
	@JsonProperty
	public List<QueryJSONRepresentation> queries = new ArrayList<QueryJSONRepresentation>();
}