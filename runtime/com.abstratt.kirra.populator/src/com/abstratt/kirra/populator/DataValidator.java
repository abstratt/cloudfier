package com.abstratt.kirra.populator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.abstratt.kirra.DataElement;
import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.fasterxml.jackson.databind.JsonNode;

public class DataValidator {
    public interface ErrorCollector {
        void addError(String description);

        void addWarning(String description);
    }

    private ErrorCollector collector;
    private SchemaManagement schema;
    private AtomicInteger index;
    private Map<String, AtomicInteger> instanceCount = new HashMap<String, AtomicInteger>();
    private List<Reference> delayedReferences = new ArrayList<Reference>();

    public DataValidator(SchemaManagement schema, ErrorCollector collector) {
        this.schema = schema;
        this.collector = collector;
    }

    public void validate(JsonNode root) {
        List<String> availableNamespaces = schema.getNamespaces();
        for (Iterator<Map.Entry<String, JsonNode>> namespaceNodes = root.fields(); namespaceNodes.hasNext();) {
            Entry<String, JsonNode> namespaceNode = namespaceNodes.next();
            String namespace = namespaceNode.getKey();
            if (!availableNamespaces.contains(namespace)) {
                collector.addError("Invalid namespace: " + namespace);
                continue;
            }
            if (!namespaceNode.getValue().isObject()) {
                collector.addError("Invalid namespace data for '" + namespace
                        + "'. Namespace data must be a map of entity names to arrays of instances.");
                continue;
            }
            Iterator<Map.Entry<String, JsonNode>> entityNodes = namespaceNode.getValue().fields();
            for (; entityNodes.hasNext();) {
                Entry<String, JsonNode> entityNode = entityNodes.next();
                if (entityNode == null || !entityNode.getValue().isArray()) {
                    collector.addError("Invalid entity: " + entityNode.getKey() + " for namespace " + namespace
                            + ". Entities must be arrays of instances. ");
                    continue;
                }
                Entity entity = schema.getEntity(namespace, entityNode.getKey());
                if (entity == null) {
                    collector.addError("Invalid entity: " + entityNode.getKey() + " for namespace " + namespace);
                    continue;
                }
                String entityName = entity.getName();
                index = instanceCount.get(namespace + '.' + entityName);
                if (index == null)
                    instanceCount.put(namespace + '.' + entityName, index = new AtomicInteger(0));
                for (Iterator<JsonNode> instanceNodes = entityNode.getValue().elements(); instanceNodes.hasNext();) {
                    index.incrementAndGet();
                    JsonNode instanceNode = instanceNodes.next();
                    if (instanceNode == null || !instanceNode.isObject()) {
                        collector.addError("Invalid instance " + entityName + "#" + index + ". Expected a map.");
                        continue;
                    }
                    validateInstance(entity, instanceNode);
                }
            }
        }
        for (Reference delayedRef : delayedReferences)
            validateReferenceIndex(delayedRef);
        delayedReferences.clear();
        instanceCount.clear();
    }

    private void validateInstance(Entity entity, JsonNode instanceNode) {
        String entityName = entity.getName();
        Map<String, Property> validProperties = new HashMap<String, Property>();
        Set<String> requiredProperties = new HashSet<String>();
        for (Property property : entity.getProperties()) {
            validProperties.put(property.getName(), property);
            if (property.isRequired() && property.getDefaultValue() == null)
                requiredProperties.add(property.getName());
        }
        Map<String, Relationship> validRelationships = new HashMap<String, Relationship>();
        for (Relationship relationship : entity.getRelationships())
            validRelationships.put(relationship.getName(), relationship);
        Set<String> propertiesFound = new HashSet<String>();
        for (Iterator<String> propertyNames = instanceNode.fieldNames(); propertyNames.hasNext();) {
            String propertyName = propertyNames.next();
            if (validProperties.containsKey(propertyName)) {
                validateProperty(entityName, instanceNode.get(propertyName), validProperties.get(propertyName));
                propertiesFound.add(propertyName);
            } else if (validRelationships.containsKey(propertyName))
                validateRelationship(entity, instanceNode.get(propertyName), validRelationships.get(propertyName));
            else
                collector.addError("Instance " + entityName + "#" + index + " refers to an unknown property: '" + propertyName + "'");
        }
        requiredProperties.removeAll(propertiesFound);
        for (String missing : requiredProperties)
            collector.addError("Instance " + entityName + "#" + index + " missing required property: '" + missing + "'");
    }

    private void validateProperty(String entityName, JsonNode propertyValue, DataElement property) {
        if (property.isDerived()) {
            collector.addError("Instance " + entityName + "#" + index + " has value for a derived property (" + property.getName() + "): "
                    + propertyValue.asToken());
            return;
        }
        String validPropertyTypeName = property.getTypeRef().getTypeName();
        String valueTypeError = null;
        switch (propertyValue.asToken()) {
        case VALUE_TRUE:
            if (!"Boolean".equals(validPropertyTypeName))
                valueTypeError = "Expected " + validPropertyTypeName + " value, found boolean";
            break;
        case VALUE_FALSE:
            if (!"Boolean".equals(validPropertyTypeName))
                valueTypeError = "Expected " + validPropertyTypeName + " value, found boolean";
            break;
        case VALUE_STRING:
            if (validPropertyTypeName.equals("Date"))
                try {
                    new SimpleDateFormat("yyyy/MM/dd").parse(propertyValue.asText());
                } catch (ParseException e) {
                    valueTypeError = "Dates must be in the format yyyy/MM/dd";
                }
            else if (property.getTypeRef().getKind() == TypeKind.Enumeration) {
                if (!property.getEnumerationLiterals().contains(propertyValue.asText())) {
                    valueTypeError = "Expected one of " + property.getEnumerationLiterals().toString() + ", found: "
                            + propertyValue.textValue();
                }
            } else if (!validPropertyTypeName.equals("String") && !validPropertyTypeName.equals("Memo"))
                valueTypeError = "Expected " + validPropertyTypeName + " value, found string value";
            break;
        case VALUE_NUMBER_INT:
            if (!"Integer".equals(validPropertyTypeName) && !"Double".equals(validPropertyTypeName))
                valueTypeError = "Expected " + validPropertyTypeName + " value, found integer value";
            break;
        case VALUE_NUMBER_FLOAT:
            if (!"Double".equals(validPropertyTypeName))
                valueTypeError = "Expected " + validPropertyTypeName + " value, found double value";
            break;
        }
        if (valueTypeError != null)
            collector.addError("Instance " + entityName + "#" + index + " has invalid value for " + property.getName() + ". "
                    + valueTypeError);
    }

    private void validateReference(TypeRef typeRef, String currentNamespace, JsonNode jsonNode) {
        Reference ref = Reference.parse(currentNamespace, jsonNode.textValue());
        if (ref == null) {
            collector.addError(jsonNode + " is not a valid reference (expected format: entity@index)");
            return;
        }
        if (ref.getNamespace() == null)
            ref.setNamespace(currentNamespace);
        if (!schema.getNamespaces().contains(ref.getNamespace())) {
            collector.addError(ref.getNamespace() + " is not a known namespace");
            return;
        }
        Entity referredEntity = schema.getEntity(ref.getNamespace(), ref.getEntity());
        if (referredEntity == null) {
            collector.addError(ref.getNamespace() + "." + ref.getEntity() + " is not a known entity");
            return;
        }
        if (!referredEntity.isA(typeRef)) {
            collector.addError("Invalid reference " + jsonNode.textValue() + ". " + referredEntity.getName() + " is not a kind of "
                    + typeRef.getTypeName());
            return;
        }

        delayedReferences.add(ref);
    }

    private void validateReferenceIndex(Reference delayedRef) {
        AtomicInteger maxAvailable = instanceCount.get(delayedRef.getNamespace() + "." + delayedRef.getEntity());
        if (maxAvailable == null || delayedRef.getIndex() < 0 || delayedRef.getIndex() + 1 > maxAvailable.get()) {
            collector.addError("Cannot resolve " + delayedRef + ", no instance at position " + (delayedRef.getIndex() + 1));
        }
    }

    private void validateReferenceOrInstance(String currentNamespace, JsonNode jsonNode, TypeRef typeRef) {
        if (jsonNode.isTextual()) {
            validateReference(typeRef, currentNamespace, jsonNode);
        } else if (jsonNode.isObject()) {
            Entity referredEntity = schema.getEntity(typeRef.getEntityNamespace(), typeRef.getTypeName());
            validateInstance(referredEntity, jsonNode);
        } else {
            collector.addError(jsonNode + " expected to be an object or reference");
            return;
        }
    }

    private void validateRelationship(Entity entity, JsonNode relatedInstanceNode, Relationship relationship) {
        if (relationship.isDerived()) {
            collector.addError("Instance " + entity.getName() + "#" + index + " attempted to modify a derived relationship ("
                    + relationship.getName() + ")");
            return;
        }
        if (relationship.isMultiple()) {
            if (!relatedInstanceNode.isArray()) {
                collector.addError("Instance " + entity + "#" + index + " must provide an arry, relationship is multiple ("
                        + relationship.getName() + ")");
                return;
            }
            for (int i = 0; i < relatedInstanceNode.size(); i++)
                validateReferenceOrInstance(entity.getEntityNamespace(), relatedInstanceNode.get(i), relationship.getTypeRef());
        } else
            validateReferenceOrInstance(entity.getEntityNamespace(), relatedInstanceNode, relationship.getTypeRef());
    }
}
