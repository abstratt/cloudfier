package com.abstratt.kirra.populator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.abstratt.kirra.DataElement;
import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.InstanceRef;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Repository;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Produces a JSON representation from a repository's data.
 */
public class DataRenderer {
    static class LazyReference {
        final private InstanceRef reference;
        private Long sequenceNumber;

        public LazyReference(InstanceRef reference) {
            super();
            this.reference = reference;
        }

        public void setSequenceNumber(Long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        @JsonValue
        public String toString() {
            return reference.getEntityName() + '@' + sequenceNumber;
        }
    }

    public static String ID = DataRenderer.class.getPackage().getName();
    private Repository repository;

    private Map<InstanceRef, LazyReference> referenceMap;

    public DataRenderer(Repository repository) {
        this.repository = repository;
    }

    public Map<String, Map<String, ?>> render() {
        referenceMap = new HashMap<InstanceRef, LazyReference>();
        Map<String, Map<String, ?>> namespaceMap = new LinkedHashMap<String, Map<String, ?>>();
        for (String namespace : this.repository.getNamespaces()) {
            Map<String, List<?>> renderedNamespace = renderNamespace(namespace);
            if (!renderedNamespace.isEmpty())
                namespaceMap.put(namespace, renderedNamespace);
        }
        referenceMap = null;
        return namespaceMap;
    }

    private LazyReference registerReference(Instance instance, Long id) {
        LazyReference reference = referenceMap.get(instance.getReference());
        if (reference == null) {
            reference = new LazyReference(instance.getReference());
            referenceMap.put(instance.getReference(), reference);
        }
        if (id != null)
            reference.setSequenceNumber(id);
        return reference;
    }

    /**
     * Renders an instance.
     * 
     * @param instance
     * @param output
     */
    private Map<String, Object> renderInstance(Entity entity, Instance instance) {
        Map<String, Object> instanceMap = new LinkedHashMap<String, Object>();
        for (DataElement property : entity.getProperties())
            if (!property.isDerived() && !property.isMultiple())
                instanceMap.put(property.getName(), instance.getValue(property.getName()));
        for (Relationship relationship : entity.getRelationships())
            if (!relationship.isDerived() && relationship.isPrimary()) {
                if (!relationship.isMultiple()) {
                    Instance related = instance.getSingleRelated(relationship.getName());
                    if (related != null || relationship.isRequired())
                        instanceMap.put(relationship.getName(), related == null ? null : registerReference(related, null));
                } else {
                    List<Instance> relateds = repository.getRelatedInstances(entity.getNamespace(), entity.getName(),
                            instance.getObjectId(), relationship.getName(), false);
                    if (relateds != null) {
                        List<LazyReference> references = new ArrayList<DataRenderer.LazyReference>();
                        for (Instance related : relateds)
                            references.add(registerReference(related, null));
                        instanceMap.put(relationship.getName(), references);
                    }
                }
            }
        return instanceMap;
    }

    /**
     * Renders an entity's instances.
     * 
     * @param entity
     * @param output
     */
    private List<Map<String, ?>> renderInstances(Entity entity) {
        List<Map<String, ?>> renderedInstances = new ArrayList<Map<String, ?>>();
        List<Instance> entityInstances = repository.getInstances(entity.getEntityNamespace(), entity.getName(), true);
        long id = 0;
        for (Instance instance : entityInstances) {
            registerReference(instance, ++id);
            renderedInstances.add(renderInstance(entity, instance));
        }
        return renderedInstances;
    }

    /**
     * Renders the namespace.
     * 
     * @param output
     * @param namespace
     */
    private Map<String, List<?>> renderNamespace(String namespace) {
        List<Entity> entities = repository.getEntities(namespace);
        EntitySorter.sort(entities);
        Map<String, List<?>> entityMap = new LinkedHashMap<String, List<?>>();
        for (Entity entity : entities)
            if (entity.isConcrete()) {
                List<Map<String, ?>> renderInstances = renderInstances(entity);
                if (!renderInstances.isEmpty())
                    entityMap.put(entity.getName(), renderInstances);
            }
        return entityMap;
    }
}
