package com.abstratt.nodestore.inmemory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Relationship.Style;
import com.abstratt.kirra.TypeRef;
import com.abstratt.nodestore.BasicNode;
import com.abstratt.nodestore.INode;
import com.abstratt.nodestore.INodeKey;
import com.abstratt.nodestore.INodeStore;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.nodestore.IntegerKey;
import com.abstratt.nodestore.NodeReference;
import com.abstratt.nodestore.NodeStoreException;
import com.abstratt.nodestore.NodeStoreValidationException;
import com.abstratt.resman.ResourceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class InMemoryNodeStore implements INodeStore, Cloneable {
	
	private Map<INodeKey, INode> nodes = new LinkedHashMap<>();
	
	private AtomicLong keys = new AtomicLong();

	private TypeRef entityName;

	private InMemoryNodeStore(TypeRef typeRef) {
        Validate.isTrue(typeRef != null);
        this.entityName = typeRef;
	}

	static InMemoryNodeStore load(InMemoryNodeStoreCatalog catalog, TypeRef typeRef) {
		try {
			byte[] contentArray = FileUtils.readFileToByteArray(catalog.getStorePath(typeRef));
			ByteArrayInputStream contents = new ByteArrayInputStream(contentArray);
			return getGson().fromJson(new InputStreamReader(contents, StandardCharsets.UTF_8), InMemoryNodeStore.class);
		} catch (FileNotFoundException e) {
			// no file
			return new InMemoryNodeStore(typeRef);
		} catch (IOException e) {
			throw new NodeStoreException("Error loading " + typeRef, e);
		}
		
	}

	private static Gson getGson() {
		GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
		gsonBuilder.registerTypeAdapter(INodeKey.class, new JsonDeserializer<INodeKey>() {
			@Override
			public INodeKey deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
					throws JsonParseException {
				return arg0 == null ? null : new IntegerKey(arg0.getAsLong());
			}
		});
		gsonBuilder.registerTypeAdapter(IntegerKey.class, new JsonSerializer<IntegerKey>() {
			@Override
			public JsonElement serialize(IntegerKey arg0, Type arg1, JsonSerializationContext context) {
				return arg0 == null ? null : context.serialize(((IntegerKey)arg0).getValue());
			}
		});
		gsonBuilder.registerTypeAdapter(INode.class, new JsonDeserializer<INode>() {
			public INode deserialize(JsonElement jsonElement, Type arg1, JsonDeserializationContext context) throws JsonParseException {
				BasicNode node = new BasicNode((INodeKey) null);
				JsonObject asJsonObject = jsonElement.getAsJsonObject();
				node.setKey(context.deserialize(asJsonObject.get("key"), INodeKey.class));
				node.setProperties(context.deserialize(asJsonObject.get("properties"), new TypeToken<Map<String, Object>>() {}.getType()));
				node.setRelated(context.deserialize(asJsonObject.get("related"), new TypeToken<Map<String, Collection<NodeReference>>>() {}.getType()));
				node.setChildren(context.deserialize(asJsonObject.get("children"), new TypeToken<Map<String, Collection<NodeReference>>>() {}.getType()));
				return node;
			}
		});
		gsonBuilder.registerTypeAdapter(BasicNode.class, new JsonSerializer<BasicNode>() {
			public JsonElement serialize(BasicNode node, Type arg1, JsonSerializationContext context) throws JsonParseException {
				JsonObject jsonObject = new JsonObject();
				jsonObject.add("key", context.serialize(node.getKey()));
				jsonObject.add("properties", context.serialize(node.getProperties()));
				jsonObject.add("related", context.serialize(node.getRelated()));
				jsonObject.add("children", context.serialize(node.getChildren()));
				return jsonObject;
			}
		});
		gsonBuilder.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {

			@Override
			public JsonElement serialize(Date arg0, Type arg1, JsonSerializationContext arg2) {
				if (arg0 == null)
					return null;
				return new JsonPrimitive(
						DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.ofInstant(arg0.toInstant(), ZoneOffset.UTC)));
			}
			
		});
		gsonBuilder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
			@Override
			public Date deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
					throws JsonParseException {
				if (arg0 == null)
					return null;
				return new Date(OffsetDateTime.parse(arg0.getAsString(), DateTimeFormatter.ISO_DATE_TIME).toInstant().toEpochMilli());
			}
		});
		
		return gsonBuilder.create();
	}
	
	static void save(InMemoryNodeStore store) {
		TypeRef typeRef = store.entityName;
		try {
			File storeFile = store.getCatalog().getStorePath(typeRef);
			FileUtils.forceMkdir(storeFile.getParentFile());
			String asString = getGson().toJson(store);
			System.out.println(storeFile);
			System.out.println(asString);
			FileUtils.write(storeFile, asString);
		} catch (IOException e) {
			throw new NodeStoreException("Error saving " + typeRef, e);
		}
	}

	@Override
	public boolean containsNode(INodeKey key) {
		return nodes.containsKey(key);
	}

	@Override
	public INodeKey createNode(INode node) {
		INodeKey newKey = node.getKey() == null ? generateKey() : node.getKey();
		Map<String, Object> properties = node.getProperties();
		getEntity().getProperties().stream().filter(it -> it.isUnique() && it.isAutoGenerated()).forEach(it -> properties.computeIfAbsent(it.getName(), key -> generateUniqueValue(it)));
		node.setProperties(properties);
		nodes.put(newKey, node.clone());
		return newKey;
	}

	@Override
	public void deleteNode(INodeKey key) {
		INode node = basicGetNode(key);
		if (node == null)
			return;
		NodeReference thisRef = new NodeReference(getName(), key);
		
		TypeRef thisType = getEntity().getTypeRef();
		Collection<TypeRef> superTypes = getEntity().getSuperTypes();
		getCatalog().getMetadata().getAllEntities().forEach(entity -> {
			List<Relationship> allRelationships = entity.getRelationships();
			
			// remove any children
			List<Relationship> incomingChildRelationships = allRelationships.stream().filter(it -> it.getStyle() == Style.PARENT && it.isPrimary() && (it.getTypeRef().equals(thisType) || superTypes.contains(it.getTypeRef()))).collect(Collectors.toList());
			if (!incomingChildRelationships.isEmpty()) 
				getCatalog().getStore(entity.getTypeRef()).removeOrphans(incomingChildRelationships, thisRef);
			
			// update incoming references
			List<Relationship> incomingRelationships = allRelationships.stream().filter(it -> it.getStyle() != Style.PARENT && it.isPrimary() && (it.getTypeRef().equals(thisType) || superTypes.contains(it.getTypeRef()))).collect(Collectors.toList());
			if (!incomingRelationships.isEmpty())
				getCatalog().getStore(entity.getTypeRef()).updateReferences(incomingRelationships, thisRef);
		});
		nodes.remove(key);
	}
	
	private void removeOrphans(List<Relationship> relationships, NodeReference removedReference) {
		Stream<INode> toDelete = nodes.values().stream().filter(node -> {
			Map<String, Collection<NodeReference>> related = node.getRelated();
			return relationships.stream().anyMatch(relationship ->
				related.getOrDefault(relationship.getName(), Collections.emptyList()).contains(removedReference)
			);
		});
		toDelete.forEach(it -> deleteNode(it.getKey()));
	}	

	private void updateReferences(List<Relationship> relationships, NodeReference removedReference) {
		nodes.values().forEach(node -> {
			Map<String, Collection<NodeReference>> related = node.getRelated();
			relationships.forEach(relationship -> {
				Collection<NodeReference> links = related.get(relationship.getName());
				if (links != null)
					links.remove(removedReference);
			});
			node.setRelated(related);
		});
	}

	private void deleteChildren(NodeReference thisRef, Relationship oppositeRel, InMemoryNodeStore oppositeStore) {
		Stream<INode> toDelete = oppositeStore.nodes.values().stream().filter((candidate) ->
			candidate.getRelated().getOrDefault(oppositeRel.getName(), Collections.emptyList()).contains(thisRef)
		);
		toDelete.forEach(it -> {
			// remove reference from child to avoid infinite loop
		    it.getRelated().remove(oppositeRel.getName());
			oppositeStore.deleteNode(it.getKey());
		});
	}

	@Override
	public INodeKey generateKey() {
		return new IntegerKey(generateUniqueValue());
	}

	private long generateUniqueValue() {
		return keys.incrementAndGet();
	}
	
	private Object generateUniqueValue(Property property) {
		if (property.getType().equals("String"))
			return "" + generateUniqueValue();
		if (property.getType().equals("Integer"))
			return generateUniqueValue();
		throw new IllegalArgumentException(property.getType());
	}

	public InMemoryNodeStoreCatalog getCatalog() {
		return (InMemoryNodeStoreCatalog) ResourceManager.getCurrentResourceManager().getCurrentResource().getFeature(INodeStoreCatalog.class);
	}

	@Override
	public String getName() {
		return entityName.toString();
	}

	@Override
	public INode getNode(INodeKey key) {
		return Optional.ofNullable(nodes.get(key)).map(it -> it.clone()).orElse(null);
	}

	@Override
	public Collection<INodeKey> getNodeKeys() {
		return nodes.keySet();
	}

	@Override
	public Collection<INode> getNodes() {
		return nodes.values().stream().map(it -> it.clone()).collect(Collectors.toList());
	}
	
	@Override
	public void updateNode(INode node) {
		nodes.put(node.getKey(), node.clone());
	}

	INode basicGetNode(INodeKey key) {
		return nodes.get(key);
	}
	
	@Override
	public Collection<INodeKey> getRelatedNodeKeys(INodeKey key, String relationshipName, String relatedNodeStoreName) {
		Collection<INode> relatedNodes = getRelatedNodes(key, relationshipName, relatedNodeStoreName);
		return relatedNodes.stream().map(it -> it.getKey()).collect(Collectors.toList());
	}

	@Override
	public Collection<INode> getRelatedNodes(INodeKey key, String relationshipName, String relatedNodeStoreName) {
		INode node = basicGetNode(key);
		if (node == null)
			return Collections.emptyList();
		Entity entity = getEntity();
		Relationship relationship = entity.getRelationship(relationshipName);
		if (relationship.isPrimary()) {
			Collection<NodeReference> related = node.getRelated().get(relationship.getName());
			return related == null ? Collections.emptyList() : related.stream().map(it -> getCatalog().resolve(it)).filter(it -> it != null).collect(Collectors.toList());
		}
		InMemoryNodeStore otherStore = (InMemoryNodeStore) getCatalog().getStore(relatedNodeStoreName);
		Relationship oppositeRel = getCatalog().getMetadata().getOpposite(relationship);
		
		NodeReference thisRef = new NodeReference(getName(), key);
		return otherStore.nodes.values().stream().filter(other -> other.getRelated().getOrDefault(oppositeRel.getName(), Collections.emptyList()).contains(thisRef)).collect(Collectors.toList());
	}

	@Override
	public void linkMultipleNodes(INodeKey key, String relationshipName, Collection<NodeReference> related) {
		INode node = basicGetNode(key);
		Entity entity = getEntity();
		Relationship relationship = entity.getRelationship(relationshipName);
		if (relationship.isPrimary()) {
			Map<String, Collection<NodeReference>> allRelated = node.getRelated();
			allRelated.put(relationshipName, related);
			node.setRelated(allRelated);
		} else {
			related.forEach(it -> {
				getCatalog().getStore(it.getStoreName()).linkNodes(it.getKey(), relationship.getOpposite(), new NodeReference(getName(), key));
			});
		}
	}

	@Override
	public void linkNodes(INodeKey key, String relationshipName, NodeReference related) {
		INode node = basicGetNode(key);
		Entity entity = getEntity();
		Relationship relationship = entity.getRelationship(relationshipName);
		if (relationship.isPrimary()) {
			Map<String, Collection<NodeReference>> allRelated = node.getRelated();
			allRelated.put(relationshipName, Arrays.asList(related));
			node.setRelated(allRelated);
		} else {
			getCatalog().getStore(related.getStoreName()).linkNodes(related.getKey(), relationship.getOpposite(), new NodeReference(getName(), key));
		}
	}

	@Override
	public void unlinkNodes(INodeKey key, String relationshipName, NodeReference toRemove) {
		INode node = basicGetNode(key);
		Entity entity = getEntity();
		Relationship relationship = entity.getRelationship(relationshipName);
		INodeStore otherStore = getCatalog().getStore(toRemove.getStoreName());
		if (relationship.isPrimary()) {
			Map<String, Collection<NodeReference>> allRelated = node.getRelated();
			Collection<NodeReference> existing = allRelated.get(relationshipName);
			if (existing.contains(toRemove)) {
				existing = new LinkedList<>(existing);
				existing.remove(toRemove);
			}
			allRelated.put(relationshipName, existing);
			if (relationship.getStyle() == Relationship.Style.CHILD)
				otherStore.deleteNode(toRemove.getKey());
			node.setRelated(allRelated);
		} else {
			otherStore.unlinkNodes(toRemove.getKey(), relationship.getOpposite(), new NodeReference(getName(), key));
		}		
	}

	private Entity getEntity() {
		return getCatalog().getMetadata().getEntity(entityName);
	}

	@Override
	public Collection<INodeKey> filter(Map<String, Collection<Object>> nodeCriteria, Integer limit) {
		return nodes.values().stream().filter(node -> {
			Map<String, Object> properties = node.getProperties();
			return nodeCriteria.entrySet().stream().allMatch(criteria -> criteria.getValue().contains(properties.get(criteria.getKey())));
		}).map(it -> it.getKey()).collect(Collectors.toList());
	}
	
	@Override
	public InMemoryNodeStore clone() {
		InMemoryNodeStore clone;
		try {
			clone = (InMemoryNodeStore) super.clone();
			clone.keys = new AtomicLong(keys.get());
			clone.nodes = nodes.values().stream().map(it -> it.clone()).collect(Collectors.toMap(it -> it.getKey(), it -> it));
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}

	public void validateConstraints() {
		Entity entity = getEntity();
		Map<String, Set<Object>> foundValues = new LinkedHashMap<>();
		nodes.values().forEach(it -> {
			Map<String, Object> values = it.getProperties();
			Map<String, Collection<NodeReference>> links = it.getRelated();
			entity.getProperties().forEach(property -> {
				if (property.isRequired()) {
					if (values.get(property.getName()) == null)
						throw new NodeStoreValidationException("Missing value for " + property.getLabel());
				}
				if (property.isUnique()) {
					if (values.containsKey(property.getName())) {
						if (!foundValues.computeIfAbsent(property.getName(), key -> new LinkedHashSet<>()).add(values.get(property.getName())))
							throw new NodeStoreValidationException("Value must be unique: " + property.getLabel());
					}
				}
			});
			entity.getRelationships().forEach(relationship -> {
				if (relationship.isRequired() && relationship.isPrimary()) {
					if (links.getOrDefault(relationship.getName(), Collections.emptyList()).isEmpty())
						throw new NodeStoreValidationException("Missing link for " + relationship.getLabel());
				}
			});
		});
	}
}
