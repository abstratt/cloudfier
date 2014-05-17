package com.abstratt.kirra.mdd.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.KirraException.Kind;
import com.abstratt.kirra.NamedElement;
import com.abstratt.kirra.Namespace;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Schema;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.Service;
import com.abstratt.kirra.TopLevelElement;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;

public class SchemaManagementSnapshot implements SchemaManagement {

	private Schema schema;
	
	private void build() {
		if (this.schema != null) {
			return;
		}
		IRepository repository = RepositoryService.DEFAULT.getFeature(IRepository.class);
		SchemaBuilder builder = new SchemaBuilder();
		this.schema = builder.buildSchema(KirraHelper.getApplicationPackages(repository.getTopLevelPackages(null)));
	}
	
	@Override
	public List<Entity> getEntities(String namespaceName) {
		if (namespaceName == null)
			return getAllEntities();
		build();
		Namespace namespace = getNamespace(namespaceName);
		return namespace != null ? namespace.getEntities() : Collections.<Entity>emptyList();
	}
	
	@Override
	public Namespace getNamespace(String namespaceName) {
		build();
		return schema.findNamespace(namespaceName);
	}

	@Override
	public List<Service> getServices(String namespaceName) {
		if (namespaceName == null)
			return getAllServices();
		build();
		Namespace namespace = getNamespace(namespaceName);
		return namespace != null ? namespace.getServices() : Collections.<Service>emptyList();
	}

	@Override
	public List<TupleType> getTupleTypes(String namespaceName) {
		if (namespaceName == null)
			return getAllTupleTypes();
		build();
		Namespace namespace = getNamespace(namespaceName);
		return namespace != null ? namespace.getTupleTypes() : Collections.<TupleType>emptyList();
	}

	@Override
	public Schema getSchema() {
		build();
		return schema;
	}

	@Override
	public Entity getEntity(String namespaceName, String name) {
		build();
		Namespace namespace = getNamespace(namespaceName);
		Entity found = namespace == null ? null : namespace.findEntity(name);
		if (found == null)
			throw new KirraException("Unknown entity: " + name + " on namespace: " + namespaceName, null, Kind.SCHEMA);
		return found;
	}
	
	@Override
	public Entity getEntity(TypeRef typeRef) {
		build();
		return getEntity(typeRef.getEntityNamespace(), typeRef.getTypeName());
	}

	@Override
	public Service getService(String namespaceName, String name) {
		build();
		Namespace namespace = getNamespace(namespaceName);
		Service found = namespace == null ? null : namespace.findService(name);
		if (found == null)
			throw new KirraException("Unknown service: " + name + " on namespace: " + namespaceName, null, Kind.SCHEMA);
		return found;
	}

	@Override
	public Service getService(TypeRef typeRef) {
		build();
		return getService(typeRef.getEntityNamespace(), typeRef.getTypeName());
	}

	@Override
	public TupleType getTupleType(String namespaceName, String name) {
		build();
		Namespace namespace = getNamespace(namespaceName);
		TupleType found = namespace == null ? null : namespace.findTupleType(name);
		if (found == null)
			throw new KirraException("Unknown tuple type: " + name + " on namespace: " + namespaceName, null, Kind.SCHEMA);
		return found;
	}

	@Override
	public TupleType getTupleType(TypeRef typeRef) {
		build();
		return getTupleType(typeRef.getEntityNamespace(), typeRef.getTypeName());
	}

	@Override
	public List<Operation> getEntityOperations(String namespaceName, String name) {
		build();
		Entity entity = getEntity(namespaceName, name);
		return entity.getOperations();
	}

	@Override
	public List<Property> getEntityProperties(String namespaceName, String name) {
		build();
		Entity entity = getEntity(namespaceName, name);
		return entity.getProperties();
	}

	@Override
	public List<Relationship> getEntityRelationships(String namespaceName, String name) {
		build();
		Entity entity = getEntity(namespaceName, name);
		return entity.getRelationships();
	}

	@Override
	public List<String> getNamespaces() {
		build();
		List<Namespace> namespaces = schema.getNamespaces();
		return getNames(namespaces);
	}

	private <NE extends NamedElement<?>> List<String> getNames(List<NE> namedElements) {
		List<String> names = new ArrayList<String>();
		for (NamedElement<?> namespace : namedElements)
			names.add(namespace.getName());
		return names ;
	}
	
	private <TLE extends TopLevelElement> List<TypeRef> getTypeRefs(List<TLE> namedElements) {
		List<TypeRef> typeRefs = new ArrayList<TypeRef>();
		for (TLE topLevelElement : namedElements)
			typeRefs.add(topLevelElement.getTypeRef());
		return typeRefs;
	}

	@Override
	public List<Entity> getTopLevelEntities(String namespace) {
		return getEntities(namespace);
	}

	@Override
	public Collection<TypeRef> getEntityNames() {
		build();
		return getTypeRefs(getAllEntities());
	}

	@Override
	public List<Entity> getAllEntities() {
		build();
		return schema.getAllEntities();
	}
	
	@Override
	public List<Service> getAllServices() {
		build();
		return schema.getAllServices();
	}
	
	@Override
	public List<TupleType> getAllTupleTypes() {
		build();
		return schema.getAllTupleTypes();
	}

	@Override
	public Relationship getOpposite(Relationship relationship) {
		build();
		return SchemaManagementOperations.getOpposite(this, relationship);
	}
}
