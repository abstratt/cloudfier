package com.abstratt.kirra.mdd.rest.representation;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

public class InstanceJSONRepresentation extends TupleJSONRepresentation {
    public static class Action {
        public boolean enabled;
        public Map<String, ActionParameter> parameters;
        public String uri;
    }

    public static class ActionParameter {
        public String domainUri;
    }

    public static class MultipleLink {
        public String domainUri;
        public String uri;
    }

    public static class SingleLink {
        public String domainUri;
        public String shorthand;
        public String uri;
    }

    @JsonProperty
    public Map<String, Action> actions;
    public String entityName;
    public String entityNamespace;
    @JsonProperty
    public String id;
    @JsonProperty
    public Map<String, MultipleLink> links;
    @JsonProperty
    public String shorthand;
    @JsonProperty
    public String type;
    @JsonProperty
    public String uri;

}