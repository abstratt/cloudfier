package com.abstratt.kirra.mdd.rest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

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
import com.abstratt.kirra.json.InstanceJSONRepresentation;
import com.abstratt.kirra.json.InstanceJSONRepresentation.Action;
import com.abstratt.kirra.json.InstanceJSONRepresentation.ActionParameter;
import com.abstratt.kirra.json.InstanceJSONRepresentation.MultipleLink;
import com.abstratt.kirra.json.InstanceJSONRepresentation.SingleLink;

public class InstanceJSONRepresentationBuilder extends BasicDataJSONRepresentationBuilder<InstanceJSONRepresentation> {
	public void build(InstanceJSONRepresentation representation, KirraReferenceBuilder refBuilder, TopLevelElement element, Tuple tuple) {
		super.build(representation, refBuilder, element, tuple);
		String namespace = tuple.getScopeNamespace();
		String entityName = tuple.getScopeName();
		Instance instance = (Instance) tuple;
		representation.type = buildTypeReference(refBuilder, namespace, entityName).toString();
		representation.entityNamespace = namespace;
		representation.entityName = entityName;
		Entity entity = (Entity) element;
		representation.actions = new HashMap<String, Action>();
		representation.links = new HashMap<String, MultipleLink>();

		for (Relationship relationship : entity.getRelationships())
			if (!relationship.isMultiple()) {
				Instance related = instance.getSingleRelated(relationship.getName());
				representation.values.put(relationship.getName(),
						buildSingleLink(refBuilder, instance, relationship, related, relationship.getTypeRef()));
			}
		if (!instance.isNew()) {
			Reference instanceUri = refBuilder.buildInstanceReference(instance);
			representation.uri = instanceUri.toString();
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
							actionParameter.domainUri = parameterBaseUri.clone().addSegment(parameter.getName()).addSegment(Paths.DOMAIN)
									.toString();
							action.parameters.put(parameter.getName(), actionParameter);
						}
					representation.actions.put(operation.getName(), action);
				}
			// multi-relationships
			for (Relationship relationship : entity.getRelationships())
				if (relationship.isMultiple())
					representation.links.put(relationship.getName(), buildMultipleLink(refBuilder, instance, relationship));
		}
		representation.shorthand = "" + instance.getShorthand();
	}

	public static Reference buildRelatedInstanceListReference(KirraReferenceBuilder refBuilder, Instance instance, Relationship relationship) {
		Reference instanceUri = refBuilder.buildInstanceReference(instance);
		Reference relatedUri = instanceUri.clone().addSegment(Paths.RELATIONSHIPS).addSegment(relationship.getName()).addSegment("");
		return relatedUri;
	}

	public static Reference buildRelationshipDomainReference(KirraReferenceBuilder refBuilder, Instance instance, Relationship relationship) {
		Reference instanceUri = refBuilder.buildInstanceReference(instance);
		Reference relatedUri = instanceUri.clone().addSegment(Paths.RELATIONSHIPS).addSegment(relationship.getName())
				.addSegment(Paths.DOMAIN);
		return relatedUri;
	}

	private static SingleLink buildSingleLink(KirraReferenceBuilder referenceBuilder, Instance instance, Relationship relationship,
			Instance relatedInstance, TypeRef relatedEntity) {
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

	protected Reference buildTypeReference(KirraReferenceBuilder refBuilder, String namespace, String entityName) {
		return refBuilder.buildEntityReference(namespace, entityName);
	}

}
