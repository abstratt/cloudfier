package com.abstratt.kirra.mdd.rest;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class EntityJSONRepresentation {
	@JsonProperty
	public String namespace;
	@JsonProperty
	public String name;
	@JsonProperty
	public String uri;
	@JsonProperty
	public String rootUri;
	@JsonProperty
	public String instances;
	public String template;
	@JsonProperty
	public List<ActionJSONRepresentation> actions = new ArrayList<ActionJSONRepresentation>();
	@JsonProperty
	public List<QueryJSONRepresentation> finders = new ArrayList<QueryJSONRepresentation>();
	@JsonProperty
	public List<PropertyJSONRepresentation> properties = new ArrayList<PropertyJSONRepresentation>();
	public List<RelationshipJSONRepresentation> relationships = new ArrayList<RelationshipJSONRepresentation>();
}