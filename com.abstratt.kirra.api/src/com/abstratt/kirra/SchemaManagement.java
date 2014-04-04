package com.abstratt.kirra;

import java.util.Collection;
import java.util.List;

public interface SchemaManagement {
	/**
	 * Returns all entities in the namespace, or across all namespaces.
	 * 
	 * @param namespace the namespace, or <code>null</code> for all
	 * @return the list of entities found
	 */
	public List<Entity> getEntities(String namespace);
	
	public List<Service> getServices(String namespace);
	
	public List<TupleType> getTupleTypes(String namespace);
	
	/**
	 * Returns the entire schema at once.
	 */
	public Schema getSchema();

	/**
	 * Returns the entity with the given name. Returns null a corresponding
	 * class does not exist, or if it is not an entity.
	 * 
	 * @param namespace
	 * @param name
	 * @return the corresponding entity, or <code>null</code>
	 */
	public Entity getEntity(String namespace, String name);
	public Entity getEntity(TypeRef typeRef);
	public Service getService(String namespace, String name);
	public Service getService(TypeRef typeRef);
	public TupleType getTupleType(String namespace, String name);
	public TupleType getTupleType(TypeRef typeRef);

	/**
	 * Returns the operations available for the given entity type.
	 */
	public List<Operation> getEntityOperations(String namespace, String name);

	// ENTITY DATA API

	/**
	 * Returns the properties available for the given entity type.
	 */
	public List<Property> getEntityProperties(String namespace, String name);

	/**
	 * Returns the relationships available for the given entity type.
	 */
	public List<Relationship> getEntityRelationships(String namespace,
			String name);
	public List<String> getNamespaces();
	
	public Namespace getNamespace(String namespaceName);

	public List<Entity> getTopLevelEntities(String namespace);

	Relationship getOpposite(Relationship relationship);

	public Collection<TypeRef> getEntityNames();

	List<Entity> getAllEntities();
	List<Service> getAllServices();
	List<TupleType> getAllTupleTypes();
}
