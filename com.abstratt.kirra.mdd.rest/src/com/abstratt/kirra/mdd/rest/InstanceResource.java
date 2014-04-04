package com.abstratt.kirra.mdd.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Relationship.Style;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.Tuple;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.TypeRef;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ResourceUtils;
import com.abstratt.pluginutils.LogUtils;

public class InstanceResource extends AbstractKirraRepositoryResource {

	@Get("json")
	public Representation getInstance() {
		String objectId = getObjectId();
		Entity targetEntity = getTargetEntity();
		
		Instance instance = "_template".equals(objectId) ? getRepository().newInstance(targetEntity.getEntityNamespace(), targetEntity.getName()) : lookupInstance(objectId);
		ResourceUtils.ensure(instance != null, "Instance not found: " + objectId, Status.CLIENT_ERROR_NOT_FOUND);
		return jsonToStringRepresentation(getInstanceJSONRepresentation(instance, targetEntity));
	}

	protected String getObjectId() {
		return (String) getRequestAttributes().get("objectId");
	}

	@Put("json")
	public Representation update(Representation requestRepresentation) throws IOException {
		String objectId = getObjectId();
		JsonNode toUpdate = JsonHelper.parse(requestRepresentation.getReader());
		Instance existingInstance = lookupInstance(objectId);
		if (existingInstance == null) {
			setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return new EmptyRepresentation();
		}
		Entity entity = getRepository().getEntity(getEntityNamespace(), getEntityName());
		updateInstanceFromJsonRepresentation(toUpdate, entity, existingInstance);
		setStatus(Status.SUCCESS_OK);
	    return jsonToStringRepresentation(getInstanceJSONRepresentation(getRepository().updateInstance(existingInstance), entity));
	}

	protected Instance lookupInstance(String objectId) {
		Entity targetEntity = getTargetEntity();
		return getRepository().getInstance(targetEntity.getNamespace(), targetEntity.getName(), objectId, true);
	}
	
	static List<?> getValuesFromJsonRepresentation(SchemaManagement repository, JsonNode representation, TypeRef type, boolean multiple) {
		if (representation == null)
			return null;
		ArrayNode asArray = multiple ? (ArrayNode) representation : null;
		switch (type.getKind()) {
		case Tuple:
		case Entity:
			TupleType tupleType = repository.getTupleType(type);
			return multiple ? getTuplesFromJsonRepresentation(asArray, tupleType) : Arrays.asList(getTupleFromJsonRepresentation(representation, tupleType));
		case Primitive:
		case Enumeration:
			return multiple ? getPrimitiveValuesFromJsonRepresentation(asArray) : Arrays.asList(getPrimitiveValueFromJsonRepresentation(representation));
		default: throw new KirraException("Unexpected result type", null, KirraException.Kind.SCHEMA);  
		}
		
	}
	
	private static List<?> getPrimitiveValuesFromJsonRepresentation(
			ArrayNode asArray) {
        List values = new ArrayList();
		for (JsonNode jsonNode : asArray)
			values.add(getPrimitiveValueFromJsonRepresentation(jsonNode));
		return values;
	}

	private static Object getPrimitiveValueFromJsonRepresentation(
			JsonNode jsonNode) {
		Object value = null;
		if (jsonNode.isNumber())
		    value = jsonNode.getNumberValue();
		else if (jsonNode.isBoolean())
		    value = jsonNode.getBooleanValue();
		else if (jsonNode.isTextual())
		    value = jsonNode.getTextValue();
		return value;
	}

	private static List<?> getTuplesFromJsonRepresentation(ArrayNode asArray,
			TupleType tupleType) {
        List values = new ArrayList();
		for (JsonNode jsonNode : asArray)
        	values.add(getTupleFromJsonRepresentation(jsonNode, tupleType));
		return values;
	}

	static Tuple getTupleFromJsonRepresentation(JsonNode tupleRepresentation, TupleType tupleType) {
		Tuple newTuple = new Tuple(tupleType.getTypeRef());
		if (tupleRepresentation == null || tupleRepresentation.isNull())
			return null;
		Iterator<String> fieldNames = tupleRepresentation.getFieldNames();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			JsonNode fieldValueNode = tupleRepresentation.get(fieldName);
			Property property = tupleType.getProperty(fieldName);
			if (property != null) {
				setProperty(newTuple, fieldName, fieldValueNode);
			} else {
				LogUtils.logWarning(InstanceResource.class.getPackage().getName(),
						"Ignored unknown field: " + fieldName + ": " + fieldValueNode, null);
		    }
		}
		return newTuple;
	}


	static void updateInstanceFromJsonRepresentation(JsonNode toUpdate, Entity entity, Instance existingInstance) {
		JsonNode values = toUpdate.get("values");
		if (values == null || values.isNull())
			return;
		Iterator<String> fieldNames = values.getFieldNames();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			JsonNode fieldValueNode = values.get(fieldName);
			Property property = entity.getProperty(fieldName);
			if (property != null) {
				if (property.isEditable())
					setProperty(existingInstance, fieldName, fieldValueNode);
			} else {
				Relationship relationship = entity.getRelationship(fieldName);
				if (relationship != null) {
					if (relationship.getStyle() != Style.PARENT && relationship.isEditable())
						updateRelationship(entity, existingInstance, relationship, fieldValueNode);
				} else
					LogUtils.logWarning(InstanceResource.class.getPackage().getName(),
							"Ignored unknown field: " + fieldName + ": " + fieldValueNode, null);
			}
		}
	}

	protected static void updateRelationship(Entity entity, Instance existingInstance, Relationship relationship, JsonNode fieldValueNode) {
	    if (fieldValueNode == null || fieldValueNode.isNull())
		    return;
		if (fieldValueNode.isTextual())
			existingInstance.setSingleRelated(relationship.getName(), resolveLink(fieldValueNode, relationship.getTypeRef()));
		else if (fieldValueNode.isArray()) {
			List<Instance> allRelated = new ArrayList<Instance>();
			for(Iterator<JsonNode> elements = ((ArrayNode) fieldValueNode).getElements();elements.hasNext();)
		    	allRelated.add(resolveLink(elements.next(), relationship.getTypeRef()));
			existingInstance.setRelated(relationship.getName(), allRelated);
		} else if (fieldValueNode.isObject()) {
			existingInstance.setSingleRelated(relationship.getName(), resolveLink(fieldValueNode.get("uri"), relationship.getTypeRef()));
		} else {
			ResourceUtils.ensure(false, relationship.getName() + ": " + fieldValueNode.toString(), Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
		}
	}

	protected static void setProperty(Tuple record, String fieldName, JsonNode fieldValueNode) {
		record.setValue(fieldName, convertSlotValue(fieldName, fieldValueNode));
	}

	@Delete
	public Representation delete() {
		String objectId = getObjectId();
		String entityName = (String) getRequestAttributes().get("entityName");
		String entityNamespace = (String) getRequestAttributes().get("entityNamespace");

	    getRepository().deleteInstance(entityNamespace, entityName, objectId);
		return new EmptyRepresentation();
	}
}
