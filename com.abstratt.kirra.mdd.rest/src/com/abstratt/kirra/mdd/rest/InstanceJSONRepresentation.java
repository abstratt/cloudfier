package com.abstratt.kirra.mdd.rest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;
import org.restlet.data.Reference;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Parameter;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.TopLevelElement;
import com.abstratt.kirra.Tuple;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.frontend.web.Paths;

public class InstanceJSONRepresentation extends TupleJSONRepresentation {
    public static class ActionParameter {
    	public String domainUri;
    }
	
	public static class Action {
		public String uri;
		public boolean enabled;
		public Map<String, ActionParameter> parameters;
	}
	
	public static class MultipleLink {
		public String uri;
		public String domainUri;
	}
	
	public static class SingleLink {
		public String uri;
		public String domainUri;
		public String shorthand;
	}
	
	public String typeName;

	@JsonProperty
	public String shorthand;
	@JsonProperty
	public String uri;
	@JsonProperty
	public String type;
	@JsonProperty
	public Map<String, MultipleLink> links;
	@JsonProperty
	public Map<String, Action> actions;

	@Override
	protected Reference buildTypeReference(KirraReferenceBuilder refBuilder,
			String namespace, String entityName) {
		return refBuilder.buildEntityReference(namespace, entityName);
	}
	
	
	@Override
	public void build(KirraReferenceBuilder refBuilder, TopLevelElement element, Tuple tuple) {
		super.build(refBuilder, element, tuple);
		String namespace = tuple.getScopeNamespace();
		String entityName = tuple.getScopeName();
		this.type = buildTypeReference(refBuilder, namespace, entityName).toString();
		Instance instance = (Instance) tuple;
		Entity entity = (Entity) element;
		this.actions = new HashMap<String, Action>();
		this.links = new HashMap<String, MultipleLink>();
		
		for (Relationship relationship : entity.getRelationships())
			if (!relationship.isMultiple()) {
				Instance related = instance.getSingleRelated(relationship.getName());
				this.values.put(relationship.getName(), buildSingleLink(refBuilder, instance, relationship, related, relationship.getTypeRef()));
			}
		if (!instance.isNew()) {
			Reference instanceUri = refBuilder.buildInstanceReference(instance);
			this.uri = instanceUri.toString();
			// actions
			for (Operation operation : entity.getOperations())
				if (operation.isInstanceOperation()) {
					Action action = new Action();
					Reference actionReference = instanceUri.clone().addSegment(Paths.ACTIONS).addSegment(operation.getName());
					action.uri = actionReference.toString();
					action.enabled = !instance.getDisabledActions().containsKey(operation.getName());
					List<Parameter> parameters = operation.getParameters();
					Reference parameterBaseUri = actionReference.clone().addSegment(Paths.PARAMETERS);
					action.parameters = new LinkedHashMap<String, ActionParameter>();
					for (Parameter parameter : parameters)
						if (parameter.getTypeRef().getKind() == TypeKind.Entity) {
							ActionParameter actionParameter = new ActionParameter();
							actionParameter.domainUri = parameterBaseUri.clone().addSegment(parameter.getName()).addSegment(Paths.DOMAIN).toString();
							action.parameters.put(parameter.getName(), actionParameter);
						}
					this.actions.put(operation.getName(), action);
				}
			// multi-relationships
			for (Relationship relationship : entity.getRelationships())
				if (relationship.isMultiple())
					this.links.put(relationship.getName(), buildMultipleLink(refBuilder, instance, relationship));
		}
		this.shorthand = "" + instance.getShorthand();
	}


	public static Reference buildRelatedInstanceListReference(KirraReferenceBuilder refBuilder, Instance instance,
			Relationship relationship) {
		Reference instanceUri = refBuilder.buildInstanceReference(instance);
		Reference relatedUri = instanceUri.clone().addSegment(Paths.RELATIONSHIPS)
				.addSegment(relationship.getName()).addSegment("");
		return relatedUri;
	}
	
	public static Reference buildRelationshipDomainReference(KirraReferenceBuilder refBuilder, Instance instance,
			Relationship relationship) {
		Reference instanceUri = refBuilder.buildInstanceReference(instance);
		Reference relatedUri = instanceUri.clone().addSegment(Paths.RELATIONSHIPS)
				.addSegment(relationship.getName()).addSegment(Paths.DOMAIN);
		return relatedUri;
	}
	
	private static SingleLink buildSingleLink(KirraReferenceBuilder referenceBuilder, Instance instance, Relationship relationship, Instance relatedInstance, TypeRef relatedEntity) {
		SingleLink relValue = null;
		relValue = new SingleLink();
		if (relatedInstance != null) {
			relValue.uri = referenceBuilder.buildInstanceReference(relatedInstance).toString();
			relValue.shorthand = relatedInstance.getShorthand();
		}
		if (!relationship.isDerived() && relationship.isEditable())
		    relValue.domainUri = buildRelationshipDomainReference(referenceBuilder, instance, relationship).toString();
		return relValue;
	}
	
	private static MultipleLink buildMultipleLink(KirraReferenceBuilder referenceBuilder, Instance instance, Relationship relationship) {
		MultipleLink relValue = null;
		relValue = new MultipleLink();
		relValue.uri = buildRelatedInstanceListReference(referenceBuilder, instance, relationship).toString();
		if (!relationship.isDerived() && relationship.isEditable())
		    relValue.domainUri = buildRelationshipDomainReference(referenceBuilder, instance, relationship).toString();
		return relValue;
	}
}