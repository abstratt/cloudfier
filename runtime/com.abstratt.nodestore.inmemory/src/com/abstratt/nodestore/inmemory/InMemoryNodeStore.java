package com.abstratt.nodestore.inmemory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

import com.abstratt.kirra.DataElement;
import com.abstratt.kirra.Entity;
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
import com.abstratt.pluginutils.LogUtils;
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

	private transient boolean dirty;

	private InMemoryNodeStore(TypeRef typeRef) {
        Validate.isTrue(typeRef != null);
        this.entityName = typeRef;
        this.dirty = false;
	}
	
	boolean isDirty() {
		return dirty;
	}

	static InMemoryNodeStore load(InMemoryNodeStoreCatalog catalog, TypeRef typeRef) {
		try {
			File storePath = catalog.getStorePath(typeRef);
			byte[] contentArray = FileUtils.readFileToByteArray(storePath);
			ByteArrayInputStream contents = new ByteArrayInputStream(contentArray);
			InMemoryNodeStore fromJson = getGson().fromJson(new InputStreamReader(contents, StandardCharsets.UTF_8), InMemoryNodeStore.class);
			if (fromJson == null) {
				LogUtils.logError(InMemoryNodeStoreActivator.BUNDLE_NAME, "Could not load JSON object from " + storePath, null);
				throw new IllegalStateException("Could not load store contents for " + typeRef);
			}
			return fromJson;
		} catch (FileNotFoundException e) {
			// no file
			return new InMemoryNodeStore(typeRef);
		} catch (IOException e) {
			throw new NodeStoreException("Error loading " + typeRef, e);
		}
		
	}

	private static Gson getGson() {
		GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting().excludeFieldsWithModifiers(Modifier.STATIC);
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
		gsonBuilder.registerTypeAdapter(INode.class, new BasicNodeSerialization());
		gsonBuilder.registerTypeAdapter(LocalTime.class, new LocalTimeSerialization());
		gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateSerialization());
		gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerialization());
		
		return gsonBuilder.create();
	}
	
	public File getStoreFile() {
		TypeRef typeRef = entityName;
		return getCatalog().getStorePath(entityName).getAbsoluteFile();
	}
	
	static void save(InMemoryNodeStore store) {
		TypeRef typeRef = store.entityName;
		try {
			File storeFile = store.getStoreFile();
			FileUtils.forceMkdir(storeFile.getParentFile());
			String asString = getGson().toJson(store);
			FileUtils.write(storeFile, asString);
			System.out.println("Saving data to " + storeFile);
			store.clearDirty();
		} catch (IOException e) {
			throw new NodeStoreException("Error saving " + typeRef, e);
		}
	}

	private void clearDirty() {
		dirty = false;
	}

	@Override
	public boolean containsNode(INodeKey key) {
		return nodes.containsKey(key);
	}

	@Override
	public INodeKey createNode(INode node) {
		INodeKey newKey = node.getKey() == null ? generateKey() : node.getKey();
		Map<String, Object> properties = node.getProperties();
		getEntity().getProperties().stream().filter(it -> it.isUnique() && it.isAutoGenerated()).forEach(it -> properties.computeIfAbsent(it.getName(), key -> newKey.toString()));
		node.setProperties(properties);
		nodes.put(newKey, node.clone());
		makeDirty();
		return newKey;
	}

	private void makeDirty() {
		dirty = true;
	}

	@Override
	public void deleteNode(INodeKey key) {
		INode node = basicGetNode(key);
		if (node == null)
			return;
		makeDirty();
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
		List<INode> toDelete = nodes.values().stream().filter(node -> {
			Map<String, Collection<NodeReference>> related = node.getRelated();
			return relationships.stream().anyMatch(relationship ->
				related.getOrDefault(relationship.getName(), Collections.emptyList()).contains(removedReference)
			);
		}).collect(Collectors.toList());
		toDelete.forEach(it -> deleteNode(it.getKey()));
	}	

	private void updateReferences(List<Relationship> relationships, NodeReference removedReference) {
		makeDirty();
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
	
	private Object generateUniqueValue(DataElement property) {
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
		makeDirty();
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
		if (relationship == null)
			return Collections.emptyList();
		if (relationship.isPrimary()) {
			Collection<NodeReference> related = node.getRelated().get(relationship.getName());
			return related == null ? Collections.emptyList() : related.stream().map(it -> getCatalog().resolve(it)).filter(it -> it != null).collect(Collectors.toList());
		}
		InMemoryNodeStore otherStore = (InMemoryNodeStore) getCatalog().getStore(relatedNodeStoreName);
		Relationship oppositeRel = getCatalog().getMetadata().getOpposite(relationship);
		
		NodeReference thisRef = new NodeReference(getName(), key);
		return otherStore.nodes.values().stream().filter(other -> { 
			Map<String, Collection<NodeReference>> otherRelated = other.getRelated();
			Collection<NodeReference> found = otherRelated.get(oppositeRel.getName());
			if (found == null)
				return false;
			return found.contains(thisRef); 
		}).collect(Collectors.toList());
	}

	@Override
	public void linkMultipleNodes(INodeKey key, String relationshipName, Collection<NodeReference> newRelated, boolean replace) {
		makeDirty();
		INode node = basicGetNode(key);
		Entity entity = getEntity();
		Relationship relationship = entity.getRelationship(relationshipName);
		if (relationship.isPrimary()) {
			Map<String, Collection<NodeReference>> allRelated = node.getRelated();
			Collection<NodeReference> existing = allRelated.get(relationshipName);
			if (existing != null && !replace)
				existing.addAll(newRelated);
			else
				allRelated.put(relationshipName, newRelated);
			node.setRelated(allRelated);
		} else {
			newRelated.forEach(it -> {
				INodeStore otherStore = getCatalog().getStore(it.getStoreName());
				String opposite = relationship.getOpposite();
				boolean primaryIsMultiple = getEntity(relationship.getTypeRef()).getRelationship(opposite).isMultiple();
				if (primaryIsMultiple)
					otherStore.linkMultipleNodes(it.getKey(), opposite, Collections.singleton(new NodeReference(getName(), key)), false);
				else
					otherStore.linkNodes(it.getKey(), opposite, new NodeReference(getName(), key));
			});
		}
	}

	@Override
	public void linkNodes(INodeKey key, String relationshipName, NodeReference related) {
		makeDirty();
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
		makeDirty();
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
		return getEntity(entityName);
	}
	
	private Entity getEntity(TypeRef entityName) {
		return getCatalog().getMetadata().getEntity(entityName);
	}

	@Override
	public Collection<INodeKey> filter(Map<String, Collection<Object>> nodeCriteria, Integer limit) {
		Collection<INode> values = nodes.values();
		return values.stream().filter(node -> {
			Map<String, Object> properties = node.getProperties();
			Map<String, Collection<NodeReference>> relationships = node.getRelated();
			return nodeCriteria.entrySet().stream().allMatch(criteria -> (
    			properties.containsKey(criteria.getKey()) 
    				&& 
				criteria.getValue().contains(properties.get(criteria.getKey()))
			) || (
    			relationships.containsKey(criteria.getKey()) 
    				&& 
				criteria.getValue().containsAll(relationships.get(criteria.getKey()))
			));
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
				Object newValue = values.get(property.getName());
				if (property.isRequired() && !property.isAutoGenerated()) {
					if (newValue == null)
						throw new NodeStoreValidationException("Missing value for " + property.getLabel());
				}
				if (property.isUnique()) {
					if (values.containsKey(property.getName())) {
						Set<Object> existingValues = foundValues.computeIfAbsent(property.getName(), key -> new LinkedHashSet<>());
						if (!existingValues.add(newValue))
							throw new NodeStoreValidationException("Value must be unique: " + property.getLabel() + " (" + newValue + ")");
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

	public static class LocalDateTimeSerialization implements JsonDeserializer<LocalDateTime>, JsonSerializer<LocalDateTime> {
		@Override
		public JsonElement serialize(LocalDateTime arg0, Type arg1, JsonSerializationContext arg2) {
			if (arg0 == null)
				return null;
			return new JsonPrimitive(
					DateTimeFormatter.ISO_DATE.format(arg0));
		}
		
		@Override
		public LocalDateTime deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
				throws JsonParseException {
			if (arg0 == null)
				return null;
			return LocalDateTime.parse(arg0.getAsString(), DateTimeFormatter.ISO_DATE);
		}
	}

	public static class LocalTimeSerialization implements JsonDeserializer<LocalTime>, JsonSerializer<LocalTime> {
		@Override
		public JsonElement serialize(LocalTime arg0, Type arg1, JsonSerializationContext arg2) {
			if (arg0 == null)
				return null;
			return new JsonPrimitive(
					DateTimeFormatter.ISO_TIME.format(arg0));
		}
		
		@Override
		public LocalTime deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
				throws JsonParseException {
			if (arg0 == null)
				return null;
			return LocalTime.parse(arg0.getAsString(), DateTimeFormatter.ISO_TIME);
		}
	}	
	
	public static class LocalDateSerialization implements JsonDeserializer<LocalDate>, JsonSerializer<LocalDate> {
		@Override
		public JsonElement serialize(LocalDate arg0, Type arg1, JsonSerializationContext arg2) {
			if (arg0 == null)
				return null;
			return new JsonPrimitive(
					DateTimeFormatter.ISO_DATE.format(arg0));
		}
		
		@Override
		public LocalDate deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
				throws JsonParseException {
			if (arg0 == null)
				return null;
			return LocalDate.parse(arg0.getAsString(), DateTimeFormatter.ISO_DATE);
		}
	}		
	
	public static class BasicNodeSerialization implements JsonDeserializer<BasicNode>, JsonSerializer<BasicNode> {
		public BasicNode deserialize(JsonElement jsonElement, Type arg1, JsonDeserializationContext context) throws JsonParseException {
			BasicNode node = new BasicNode((INodeKey) null);
			JsonObject asJsonObject = jsonElement.getAsJsonObject();
			node.setKey(context.deserialize(asJsonObject.get("key"), INodeKey.class));
			node.setProperties(context.deserialize(asJsonObject.get("properties"), new TypeToken<Map<String, Object>>() {}.getType()));
			node.setRelated(context.deserialize(asJsonObject.get("related"), new TypeToken<Map<String, Collection<NodeReference>>>() {}.getType()));
			node.setChildren(context.deserialize(asJsonObject.get("children"), new TypeToken<Map<String, Collection<NodeReference>>>() {}.getType()));
			return node;
		}
		public JsonElement serialize(BasicNode node, Type arg1, JsonSerializationContext context) throws JsonParseException {
			JsonObject jsonObject = new JsonObject();
			jsonObject.add("key", context.serialize(node.getKey()));
			jsonObject.add("properties", context.serialize(node.getProperties()));
			jsonObject.add("related", context.serialize(node.getRelated()));
			jsonObject.add("children", context.serialize(node.getChildren()));
			return jsonObject;
		}
	}
}
