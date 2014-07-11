package com.abstratt.kirra.mdd.runtime;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EClass;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.KirraException.Kind;
import com.abstratt.kirra.Namespace;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Schema;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.Service;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.TypeRef;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.NamedElementLookupCache;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.runtime.ModelExecutionException;

public class SchemaManagementOnUML implements SchemaManagement {

    public static SchemaManagementOnUML create() {
        return new SchemaManagementOnUML();
    }

    static KirraException convertModelExecutionException(ModelExecutionException rre, Kind kind) {
        return KirraOnMDDRuntime.convertModelExecutionException(rre, kind);
    }

    private SchemaManagementOnUML() {
    }

    @Override
    public List<Entity> getAllEntities() {
        return getSchemaManagement().getAllEntities();
    }

    @Override
    public List<Service> getAllServices() {
        return getSchemaManagement().getAllServices();
    }

    @Override
    public List<TupleType> getAllTupleTypes() {
        return getSchemaManagement().getAllTupleTypes();
    }

    @Override
    public String getApplicationName() {
        return getSchemaManagement().getApplicationName();
    }

    @Override
    public String getBuild() {
        return getSchemaManagement().getBuild();
    }

    @Override
    public List<Entity> getEntities(String namespace) {
        return getSchemaManagement().getEntities(namespace);
    }

    @Override
    public Entity getEntity(String namespace, String name) {
        return getSchemaManagement().getEntity(namespace, name);
    }

    @Override
    public Entity getEntity(TypeRef typeRef) {
        return getSchemaManagement().getEntity(typeRef);
    }

    @Override
    public Collection<TypeRef> getEntityNames() {
        return getSchemaManagement().getEntityNames();
    }

    @Override
    public List<com.abstratt.kirra.Operation> getEntityOperations(String namespace, String name) {
        return getSchemaManagement().getEntityOperations(namespace, name);
    }

    @Override
    public List<Property> getEntityProperties(String namespace, String name) {
        return getSchemaManagement().getEntityProperties(namespace, name);
    }

    @Override
    public List<Relationship> getEntityRelationships(String namespace, String name) {
        return getSchemaManagement().getEntityRelationships(namespace, name);
    }

    public <NE extends org.eclipse.uml2.uml.NamedElement> NE getModelElement(String namespace, String name, EClass elementClass) {
        return getLookup().find(SchemaManagementOperations.getQualifiedName(namespace, name), elementClass);
    }

    @Override
    public Namespace getNamespace(String namespaceName) {
        return getSchemaManagement().getNamespace(namespaceName);
    }

    @Override
    public List<String> getNamespaces() {
        return getSchemaManagement().getNamespaces();
    }

    @Override
    public Relationship getOpposite(Relationship relationship) {
        return getSchemaManagement().getOpposite(relationship);
    }

    @Override
    public Schema getSchema() {
        return getSchemaManagement().getSchema();
    }

    @Override
    public Service getService(String namespace, String name) {
        return getSchemaManagement().getService(namespace, name);
    }

    @Override
    public Service getService(TypeRef typeRef) {
        return getService(typeRef);
    }

    @Override
    public List<Service> getServices(String namespace) {
        return getSchemaManagement().getServices(namespace);
    }

    @Override
    public List<Entity> getTopLevelEntities(String namespace) {
        return getTopLevelEntities(namespace);
    }

    @Override
    public TupleType getTupleType(String namespace, String name) {
        return getSchemaManagement().getTupleType(namespace, name);
    }

    @Override
    public TupleType getTupleType(TypeRef typeRef) {
        return getSchemaManagement().getTupleType(typeRef);
    }

    @Override
    public List<TupleType> getTupleTypes(String namespace) {
        return getSchemaManagement().getTupleTypes(namespace);
    }

    protected IRepository getRepository() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(IRepository.class);
    }

    private NamedElementLookupCache getLookup() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(NamedElementLookupCache.class);
    }

    private SchemaManagement getSchemaManagement() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(SchemaManagement.class);
    }
}