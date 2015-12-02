package com.abstratt.kirra.mdd.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.BehavioralFeature;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.MultiplicityElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Reception;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.TypedElement;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.Vertex;

import com.abstratt.kirra.DataElement;
import com.abstratt.kirra.Entity;
import com.abstratt.kirra.ExternalService;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.KirraException.Kind;
import com.abstratt.kirra.Namespace;
import com.abstratt.kirra.Parameter;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.Schema;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.Service;
import com.abstratt.kirra.Tuple;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder;
import com.abstratt.kirra.mdd.schema.SchemaManagementOperations;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.NamedElementLookupCache;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.runtime.ModelExecutionException;
import com.abstratt.mdd.core.runtime.NotFoundException;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.runtime.RuntimeClass;
import com.abstratt.mdd.core.runtime.RuntimeMessageEvent;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.RuntimeRaisedException;
import com.abstratt.mdd.core.runtime.external.ExternalObjectDelegate;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.CollectionType;
import com.abstratt.mdd.core.runtime.types.EnumerationType;
import com.abstratt.mdd.core.runtime.types.PrimitiveType;
import com.abstratt.mdd.core.runtime.types.StateMachineType;
import com.abstratt.mdd.core.runtime.types.ValueConverter;
import com.abstratt.mdd.core.util.AssociationUtils;
import com.abstratt.mdd.core.util.FeatureUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.core.util.ReceptionUtils;
import com.abstratt.mdd.core.util.StateMachineUtils;
import com.abstratt.nodestore.NodeNotFoundException;
import com.abstratt.nodestore.NodeStoreException;
import com.abstratt.nodestore.NodeStoreNotFoundException;
import com.abstratt.nodestore.NodeStoreValidationException;

public class KirraOnMDDRuntime implements KirraMDDConstants, Repository, ExternalObjectDelegate {

    public static KirraOnMDDRuntime create() {
        return new KirraOnMDDRuntime();
    }

    public static Throwable translateException(Throwable toTranslate) {
        if (toTranslate instanceof NodeStoreException) {
            return KirraOnMDDRuntime.convertNodeStoreException((NodeStoreException) toTranslate);
        }
        if (toTranslate instanceof ModelExecutionException)
            return KirraOnMDDRuntime.convertModelExecutionException((ModelExecutionException) toTranslate, KirraException.Kind.VALIDATION);
        return toTranslate;
    }

    static KirraException convertModelExecutionException(ModelExecutionException rre, Kind kind) {
        String symbol = (rre instanceof RuntimeRaisedException) ? ((RuntimeRaisedException) rre).getExceptionType().getName() : null;
        String message = rre.getMessage() == null && symbol != null ? KirraHelper.getLabelFromSymbol(symbol) : rre.getMessage();
        String context = rre.getContext() != null ? rre.getContext().getQualifiedName() : null;
        return new KirraException(message, rre, kind, context, symbol);
    }

    static KirraException convertNodeStoreException(NodeStoreException rre) {
        if (rre instanceof NodeNotFoundException)
            return new KirraException("Object not found", rre, Kind.OBJECT_NOT_FOUND);
        if (rre instanceof NodeStoreNotFoundException)
            return new KirraException("Node store not found", rre, Kind.SCHEMA);
        return new KirraException(rre.getMessage(), rre, Kind.VALIDATION);
    }

    // cache of converted objects - avoids infinite loops when converting a
    // graph of RuntimeObjects into a graph of Instances
    private Map<RuntimeObject, Instance> convertedToInstance = new HashMap<RuntimeObject, Instance>();

    private static Map<Instance, RuntimeObject> convertedToRuntimeObject = new HashMap<Instance, RuntimeObject>();

    private static Log log = LogFactory.getLog(KirraOnMDDRuntime.class);

    private boolean validating = true;

    private boolean open = true;

    private boolean filtering = false;

    private KirraOnMDDRuntime() {
    }

    public BasicType convertToBasicType(Object value, MultiplicityElement targetElement) {
        Classifier targetType = (Classifier) ((TypedElement) targetElement).getType();
        if (value instanceof Collection<?>) {
            Collection<?> asCollection = (Collection<?>) value;
            if (targetElement.isMultivalued()) {
                return convertToCollectionType(asCollection, targetElement);
            }
            return asCollection.isEmpty() ? null : convertSingleToBasicType((TypedElement) targetElement, asCollection.iterator().next(),
                    targetType);
        }
        return convertSingleToBasicType((TypedElement) targetElement, value, targetType);
    }

    @Override
    public Instance createInstance(Instance kirraInstance) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        validateValues(kirraInstance);
        try {
            RuntimeObject asRuntimeObject = convertToRuntimeObject(kirraInstance);
            asRuntimeObject.save();
            return (Instance) convertFromRuntimeObject(asRuntimeObject);
        } catch (NodeStoreException e) {
            throw new KirraException("Could not create '" + kirraInstance.getTypeRef() + "': " + e.getMessage(), e, Kind.VALIDATION);
        } catch (ModelExecutionException rre) {
            throw KirraOnMDDRuntime.convertModelExecutionException(rre, Kind.VALIDATION);
        }
    }

    @Override
    public void deleteInstance(Instance instance) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        deleteInstance(instance.getEntityNamespace(), instance.getEntityName(), instance.getObjectId());
    }

    @Override
    public void deleteInstance(String namespace, String name, String id) {
        RuntimeObject toDelete = findRuntimeObject(namespace, name, id);
        if (toDelete == null)
            throw new KirraException("Object does not exist", null, Kind.OBJECT_NOT_FOUND);
        try {
            toDelete.destroy();
        } catch (NodeStoreException e) {
            throw new KirraException("Could not delete: " + e.getMessage(), e, Kind.VALIDATION);
        } catch (ModelExecutionException rre) {
            throw KirraOnMDDRuntime.convertModelExecutionException(rre, Kind.VALIDATION);
        }
    }

    @Override
    public void dispose() {
        open = false;
        KirraOnMDDRuntime.log.debug("Disposing: " + getRuntime().getRepository().getBaseURI().toString());
        getRuntime().getRepository().dispose();
    }

    @Override
    public List<?> executeOperation(com.abstratt.kirra.Operation operation, String externalId, List<?> arguments) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        TypeRef ownerRef = operation.getOwner();
        String qualifiedName = ownerRef.getEntityNamespace().replace(".", org.eclipse.uml2.uml.NamedElement.SEPARATOR)
                + org.eclipse.uml2.uml.NamedElement.SEPARATOR + ownerRef.getTypeName();
        Classifier umlClass = getRepository().findNamedElement(qualifiedName, Literals.CLASS, null);

        if (KirraHelper.isService(umlClass))
            return executeServiceOperation(umlClass, operation, arguments);

        return executeEntityOperation(umlClass, externalId, operation, arguments);
    }

    public void flush() {
        convertedToInstance.clear();
        KirraOnMDDRuntime.convertedToRuntimeObject.clear();
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
    public Instance getCurrentUser() {
        RuntimeObject currentActor = getRuntime().getCurrentActor();
        return (Instance) (currentActor != null ? convertFromRuntimeObject(currentActor) : null);
    }

    @Override
    public BasicType getData(Classifier classifier, Operation operation, Object... arguments) {
        String namespace = SchemaManagementOperations.getNamespace(classifier);
        List<?> result = getExternalService().executeOperation(namespace, classifier.getName(), operation.getName(),
                convertArgumentsFromBasicType(operation, arguments));
        return convertToBasicType(result, operation.getReturnResult());
    }

    @Override
    public List<com.abstratt.kirra.Operation> getEnabledEntityActions(Entity entity) {
        RuntimeClass runtimeClass = getRuntimeClass(entity.getNamespace(), entity.getName(), UMLPackage.Literals.CLASS);
        RuntimeObject classObject = runtimeClass.getClassObject();
        Set<org.eclipse.uml2.uml.Operation> enabled = classObject.computeEnabledOperations();
        List<org.eclipse.uml2.uml.Operation> allStaticActions = KirraHelper
                .getNonInstanceActions((Class) runtimeClass.getModelClassifier());
        Set<com.abstratt.kirra.Operation> result = new HashSet<com.abstratt.kirra.Operation>();
        for (org.eclipse.uml2.uml.Operation operation : allStaticActions)
            if (enabled.contains(operation))
                result.add(entity.getOperation(operation.getName()));
        return new ArrayList<com.abstratt.kirra.Operation>(result);
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

    @Override
    public Instance getInstance(String namespace, String name, String externalId, boolean full) {
        RuntimeObject found = findRuntimeObject(namespace, name, externalId);
        return (Instance) (found == null ? null : convertFromRuntimeObject(found));
    }

    @Override
    public List<Instance> getInstances(String namespace, String name, boolean full) {
        Class umlClass = (Class) getModelElement(namespace, name, UMLPackage.Literals.CLASS);
        return filterValidInstances(getRuntime().getAllInstances(umlClass, true));
    }
    
    @Override
    public List<Instance> filterInstances(Map<String, List<Object>> kirraCriteria, String namespace, String name, boolean full) {
        Class umlClass = (Class) getModelElement(namespace, name, UMLPackage.Literals.CLASS);
        Map<org.eclipse.uml2.uml.Property, List<BasicType>> runtimeCriteria = new LinkedHashMap<org.eclipse.uml2.uml.Property, List<BasicType>>();
        for (Entry<String, List<Object>> entry : kirraCriteria.entrySet()) {
            org.eclipse.uml2.uml.Property attribute = FeatureUtils.findAttribute(umlClass, entry.getKey(), false, true);
            if (attribute == null)
                throw new KirraException("Attribute " + entry.getKey() + " does not exist", null, Kind.SCHEMA);
            List<BasicType> convertedValues = new ArrayList<BasicType>();
            for (Object kirraValue : entry.getValue())
                convertedValues.add(convertToBasicType(kirraValue, attribute));
            runtimeCriteria.put(attribute, convertedValues);
        }
        return filterValidInstances(getRuntime().findInstances(umlClass, runtimeCriteria));
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
    public List<Instance> getRelationshipDomain(Entity entity, String objectId, Relationship relationship) {
        Class umlClass = (Class) getModelElement(entity.getNamespace(), entity.getName(), UMLPackage.Literals.CLASS);
        org.eclipse.uml2.uml.Property property = AssociationUtils.findMemberEnd(umlClass, relationship.getName());
        if (!KirraHelper.isRelationship(property))
            throw new KirraException(relationship + " is not a relationship", null, Kind.ENTITY);
        if ("_template".equals(objectId))
            return getInstances(relationship.getTypeRef().getNamespace(), relationship.getTypeRef().getTypeName(), false);
        return filterValidInstances(getRuntime().getPropertyDomain(umlClass, objectId, property, true));
    }

    @Override
    public List<Instance> getParameterDomain(Entity entity, String externalId, com.abstratt.kirra.Operation action, Parameter parameter) {
        if (!action.isInstanceOperation())
            return getInstances(parameter.getTypeRef().getNamespace(), parameter.getTypeRef().getTypeName(), false);
        Class umlClass = (Class) getModelElement(entity.getNamespace(), entity.getName(), UMLPackage.Literals.CLASS);
        org.eclipse.uml2.uml.Operation operation = FeatureUtils.findOperation(getRepository(), umlClass, action.getName(), null);
        org.eclipse.uml2.uml.Parameter umlParameter = operation.getOwnedParameter(parameter.getName(), null);
        return filterValidInstances(getRuntime().getParameterDomain(umlClass, externalId, umlParameter, true));
    }

    @Override
    public Properties getProperties() {
        return getRepository().getProperties();
    }

    @Override
    public List<Instance> getRelatedInstances(String namespace, String name, String externalId, String relationship, boolean full) {
        Class umlClass = (Class) getModelElement(namespace, name, UMLPackage.Literals.CLASS);
        org.eclipse.uml2.uml.Property property = AssociationUtils.findMemberEnd(umlClass, relationship);
        if (!KirraHelper.isRelationship(property))
            throw new KirraException(relationship + " is not a relationship", null, Kind.ENTITY);
        return filterValidInstances(getRuntime().getRelatedInstances(umlClass, externalId, property));
    }

    @Override
    public java.net.URI getRepositoryURI() {
        return MDDUtil.fromEMFToJava(getRuntime().getRepository().getBaseURI());
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

    @Override
    public void initialize() {
        getRuntime().prime();
    }

    @Override
    public boolean isFiltering() {
        return filtering;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isValidating() {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        return validating;
    }

    @Override
    public void linkInstances(Relationship relationship, String sourceId, String destinationId) {
    	RuntimeObject source = findRuntimeObject(relationship.getOwner().getEntityNamespace(), relationship.getOwner().getTypeName(), sourceId);
    	RuntimeObject target = findRuntimeObject(relationship.getTypeRef().getEntityNamespace(), relationship.getTypeRef().getTypeName(), destinationId);
        org.eclipse.uml2.uml.Property attribute = AssociationUtils.findMemberEnd(source.getRuntimeClass().getModelClassifier(), relationship.getName());
        if (attribute == null)
            throw new KirraException("Attribute " + relationship.getName() + " does not exist", null, Kind.SCHEMA);
        org.eclipse.uml2.uml.Property otherEnd = attribute.getOtherEnd();
        if (attribute.getAssociation() == null)
            throw new KirraException("Attribute " + attribute.getQualifiedName() + " is not an association end", null, Kind.SCHEMA);
        if (KirraHelper.isDerived(attribute)) 
        	throw new KirraException("Relationship " + attribute.getQualifiedName() + " cannot be modified, it is derived", null, Kind.SCHEMA);
        if (KirraHelper.isReadOnly(attribute)) 
        	throw new KirraException("Relationship " + attribute.getQualifiedName() + " cannot be modified, it is read-only", null, Kind.SCHEMA);
        if (otherEnd != null && KirraHelper.isReadOnly(otherEnd)) 
        	throw new KirraException("Relationship " + attribute.getQualifiedName() + " cannot be modified, the other end ("+ otherEnd.getQualifiedName() + ") is read-only", null, Kind.SCHEMA);
        if (!attribute.isNavigable()) 
        	throw new KirraException("Relationship " + attribute.getQualifiedName() + " cannot be modified, it is not navigable", null, Kind.SCHEMA);
    	source.link(attribute, target);
    }

    @Override
    public Instance newInstance(String namespace, String name) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        RuntimeObject newRuntimeObject = convertToRuntimeObject(new Instance(namespace, name));
        Instance newInstance = (Instance) convertFromRuntimeObject(newRuntimeObject);
        newInstance.setObjectId(null);
        return newInstance;
    }

    @Override
    public void receiveSignal(Classifier classifier, Signal signal, Object... arguments) {
        String namespace = SchemaManagementOperations.getNamespace(classifier);
        Reception reception = ReceptionUtils.findBySignal(classifier, signal);
        getExternalService().executeOperation(namespace, classifier.getName(), reception.getName(),
                convertArgumentsFromBasicType(reception, arguments));
    }

    @Override
    public void saveContext() {
        try {
            getRuntime().saveContext(false);
        } catch (NodeStoreValidationException e) {
            throw new KirraException(e.getMessage(), null, Kind.VALIDATION);
        } catch (ModelExecutionException rre) {
            throw KirraOnMDDRuntime.convertModelExecutionException(rre, Kind.VALIDATION);
        }
    }

    public void setClientType(String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFiltering(boolean filtering) {
        this.filtering = filtering;
    }

    @Override
    public void setRepositoryURI(java.net.URI repositoryURI) {
        throw new UnsupportedOperationException(
                "This repository implementation does not allow setting a repository location after creation");
    }

    @Override
    public void setValidating(boolean isValidating) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        this.validating = isValidating;
    }

    @Override
    public void unlinkInstances(Relationship relationship, String sourceId, String destinationId) {
        Entity sourceEntity = getEntity(relationship.getOwner());
        Entity targetEntity = getEntity(relationship.getTypeRef());
        RuntimeObject source = findRuntimeObject(sourceEntity.getEntityNamespace(), sourceEntity.getName(), sourceId);
        if (source == null)
            throw new KirraException("Source object does not exist", null, Kind.OBJECT_NOT_FOUND);
        RuntimeObject destination = findRuntimeObject(targetEntity.getEntityNamespace(), targetEntity.getName(), destinationId);
        if (destination == null)
            throw new KirraException("Destination object does not exist", null, Kind.OBJECT_NOT_FOUND);
        org.eclipse.uml2.uml.Property end = AssociationUtils.findMemberEnd(source.getRuntimeClass().getModelClassifier(),
                relationship.getName());
        if (end == null)
            throw new KirraException("Unknown end: " + relationship.getName(), null, Kind.SCHEMA);
        if (KirraHelper.isRequired(end))
        	throw new KirraException("Cannot unlink: " + relationship.getName() + ", it is required", null, Kind.SCHEMA);
        if (KirraHelper.isReadOnly(end))
        	throw new KirraException("Cannot unlink: " + relationship.getName() + ", it is read-only", null, Kind.SCHEMA);        
        org.eclipse.uml2.uml.Property otherEnd = end.getOtherEnd();
        if (otherEnd != null) {
        	if (KirraHelper.isEditable(otherEnd))
        		throw new KirraException("Cannot unlink: " + relationship.getName() + ", the other end (" + otherEnd.getQualifiedName() + ") is read-only", null, Kind.SCHEMA);
        	if (KirraHelper.isRequired(otherEnd) && !KirraHelper.isMultiple(otherEnd))
        		throw new KirraException("Cannot unlink: " + relationship.getName() + ", the other end (" + otherEnd.getQualifiedName() + ") is required", null, Kind.SCHEMA);
        }
        source.unlink(end, destination);
    }

    @Override
    public Instance updateInstance(Instance kirraInstance) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        if (kirraInstance.isNew())
            throw new KirraException("Cannot update an unsaved instance", null, Kind.ENTITY);
        try {
            validateValues(kirraInstance);
            return (Instance) convertFromRuntimeObject(convertToRuntimeObject(kirraInstance));
        } catch (NodeStoreException e) {
            throw new KirraException("Could not update: " + e.getMessage(), e, Kind.VALIDATION);
        } catch (ModelExecutionException e) {
            throw KirraOnMDDRuntime.convertModelExecutionException(e, Kind.VALIDATION);
        }
    }

    @Override
    public void validateInstance(Instance toValidate) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        validateValues(toValidate);
    }

    @Override
    public void zap() {
        getRuntime().zap();
    }

    protected Map<String, String> computeDisabledActions(RuntimeObject source, List<org.eclipse.uml2.uml.Operation> operations) {
        Map<String, String> result = new HashMap<String, String>();
        Set<Operation> enabled = source.computeEnabledOperations();
        for (org.eclipse.uml2.uml.Operation operation : operations)
            if (KirraHelper.isAction(operation))
                if (!enabled.contains(operation))
                    result.put(KirraHelper.getName(operation), "");
        return result;
    }

    protected List<Instance> filterValidInstances(Collection<BasicType> allRuntimeObjects) {
        List<Instance> allInstances = new ArrayList<Instance>(allRuntimeObjects.size());
        for (BasicType current : allRuntimeObjects) {
            RuntimeObject currentRuntimeObject = (RuntimeObject) current;
            if (!filtering || currentRuntimeObject.checkConstraints(MDDExtensionUtils.ACCESS_STEREOTYPE) == null)
                allInstances.add((Instance) convertFromRuntimeObject(currentRuntimeObject));
        }
        return allInstances;
    }

    protected IRepository getRepository() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(IRepository.class);
    }

    private List<? extends Object> asList(Object convertedResult) {
        return convertedResult instanceof List<?> ? (List<?>) convertedResult : Arrays.asList(convertedResult);
    }

    /**
     * This method can be used to execute any kind of operation.
     * 
     * @param target
     *            the target object, or null for a static operation
     * @param operation
     *            the operation to execute
     * @param arguments
     *            arguments to pass
     * @return the resulting value
     * @see com.abstratt.kirra.Operation.OperationKind
     */
    private List<?> basicExecuteOperation(BasicType target, Operation operation, List<?> arguments) {
        List<org.eclipse.uml2.uml.Parameter> inParameters = FeatureUtils.filterParameters(operation.getOwnedParameters(),
                ParameterDirectionKind.IN_LITERAL);
        if (arguments.size() != inParameters.size())
            throw new KirraException("Operation '" + operation.getName() + "' requires " + inParameters.size() + " arguments, "
                    + arguments.size() + " were provided", null, Kind.ENTITY);
        Map<String, Object> argumentsPerParameter = new HashMap<String, Object>();
        Object[] convertedArguments = new Object[inParameters.size()];
        for (int i = 0; i < inParameters.size(); i++)
            if (arguments.get(i) != null) {
                convertedArguments[i] = convertToBasicType(arguments.get(i), inParameters.get(i));
                argumentsPerParameter.put(KirraHelper.getName(inParameters.get(i)), convertedArguments[i]);
            }

        if (target instanceof RuntimeObject && !((RuntimeObject) target).isEnabledOperation(operation, argumentsPerParameter))
            throw new KirraException("Operation '" + operation.getName() + "' is not valid at this time", null, Kind.VALIDATION);

        try {
            BasicType result = (BasicType) getRuntime().runOperation(null, target, operation, convertedArguments);
            if (result == null)
                return Collections.emptyList();
            if (KirraHelper.isFinder(operation))
                return filterValidInstances(((CollectionType) result).getBackEnd());
            Classifier operationType = (Classifier) operation.getType();
            return asList(convertFromBasicType(result, operationType));
        } catch (ModelExecutionException rre) {
            throw KirraOnMDDRuntime.convertModelExecutionException(rre, Kind.ENTITY);
        }
    }

    private Map<String, Object> convertArgumentsFromBasicType(BehavioralFeature behavioralFeature, Object... arguments) {
        List<org.eclipse.uml2.uml.Parameter> parameters = FeatureUtils.filterParameters(behavioralFeature.getOwnedParameters(),
                ParameterDirectionKind.IN_LITERAL, ParameterDirectionKind.INOUT_LITERAL);
        int required = parameters.size();
        if (required != arguments.length)
            throw new KirraException("Operation " + behavioralFeature.getName() + " requires " + required + " parameters, got "
                    + arguments.length, null, KirraException.Kind.EXTERNAL);
        Map<String, Object> argumentMap = new LinkedHashMap<String, Object>();
        for (int i = 0; i < arguments.length; i++)
            argumentMap.put(parameters.get(i).getName(),
                    convertFromBasicType((BasicType) arguments[i], (Classifier) parameters.get(i).getType()));
        return argumentMap;
    }

    private Object convertFromBasicType(BasicType value, Classifier sourceType) {
        if (value == null)
            return null;
        if (value instanceof CollectionType)
            return convertFromCollectionType((CollectionType) value, sourceType);
        if (value instanceof RuntimeObject)
            return convertFromRuntimeObject((RuntimeObject) value);
        if (value instanceof EnumerationType)
            return ((EnumerationType) value).getValue().getName();
        if (value instanceof StateMachineType)
            return value.toString();
        if (value instanceof PrimitiveType)
            return convertFromPrimitive((PrimitiveType<?>) value);
        throw new IllegalArgumentException(value + " is of an unexpected type: " + sourceType.getQualifiedName());
    }

    private List<?> convertFromCollectionType(CollectionType value, Classifier sourceType) {
        if (sourceType instanceof Class || sourceType instanceof DataType) {
            List<Tuple> result = new ArrayList<Tuple>(value.getBackEnd().size());
            for (BasicType currentRuntimeObject : value.getBackEnd())
                result.add(convertFromRuntimeObject((RuntimeObject) currentRuntimeObject));
            return result;
        }
        List<Object> result = new ArrayList<Object>(value.getBackEnd().size());
        for (BasicType currentPrimitive : value.getBackEnd())
            result.add(convertFromPrimitive((PrimitiveType<?>) currentPrimitive));
        return result;
    }

    private Object convertFromPrimitive(PrimitiveType<?> value) {
        return value.primitiveValue();
    }

    private Tuple convertFromRuntimeObject(RuntimeObject source) {
        if (source == null)
            return null;
        if (source.isTuple())
            return convertToTuple(source);
        final boolean first = convertedToInstance.isEmpty();
        try {
            Instance alreadyConverted = convertedToInstance.get(source);
            if (alreadyConverted != null)
                return alreadyConverted;
            RuntimeClass class_ = source.getRuntimeClass();
            Instance kirraInstance = new Instance();
            convertedToInstance.put(source, kirraInstance);
            kirraInstance.setFull(true);
            Classifier modelClassifier = class_.getModelClassifier();
            kirraInstance.setEntityName(modelClassifier.getName());
            kirraInstance.setEntityNamespace(modelClassifier.getNamespace().getQualifiedName());
            kirraInstance.setObjectId(getObjectId(source));
            EList<org.eclipse.uml2.uml.Property> allAttributes = modelClassifier.getAllAttributes();
            for (org.eclipse.uml2.uml.Property property : allAttributes) {
                if (KirraHelper.isEntity(property.getType())) {
                    if (property.isMultivalued()) {
                        // do nothing: only non-multiple arcs need to be
                        // hydrated
                    } else {
                        BasicType value = source.getValue(property);
                        if (value == null)
                            continue;
                        Object converted = convertFromBasicType(value, (Classifier) property.getType());
                        kirraInstance.setRelated(KirraHelper.getName(property), Arrays.asList((Instance) converted));
                    }
                } else if (KirraHelper.isProperty(property)) {
                    BasicType value = source.getValue(property);
                    if (value == null)
                        continue;
                    // convert to client format
                    Object converted = convertFromBasicType(value, (Classifier) property.getType());
                    kirraInstance.setValue(KirraHelper.getName(property), converted);
                }
            }
            org.eclipse.uml2.uml.Property mnemonic = KirraHelper.getMnemonic(modelClassifier);
            if (mnemonic != null) {
                Object shorthand = kirraInstance.getValue(KirraHelper.getName(mnemonic));
                kirraInstance.setShorthand(shorthand == null ? null : shorthand.toString());
            }
            if (source.isPersisted()) {
                List<Association> allAssociations = AssociationUtils.allAssociations(modelClassifier);
                for (Association association : allAssociations) {
                    for (org.eclipse.uml2.uml.Property property : association.getMemberEnds()) {
                        if (modelClassifier.conformsTo(property.getType())) {
                            org.eclipse.uml2.uml.Property forwardReference = property.getOtherEnd();
                            if (forwardReference != null && forwardReference.getOwningAssociation() == association
                                    && forwardReference.isNavigable()) {
                                if (KirraHelper.isEntity(forwardReference.getType())) {
                                    if (forwardReference.isMultivalued()) {
                                        // do nothing: only non-multiple arcs
                                        // need to be hydrated
                                    } else {
                                        BasicType value = source.getValue(forwardReference);
                                        if (value == null)
                                            continue;
                                        Object converted = convertFromBasicType(value, (Classifier) forwardReference.getType());
                                        kirraInstance
                                                .setRelated(KirraHelper.getName(forwardReference), Arrays.asList((Instance) converted));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            kirraInstance.setDisabledActions(computeDisabledActions(source, modelClassifier.getAllOperations()));
            return kirraInstance;
        } finally {
            if (first)
                convertedToInstance.clear();
        }
    }

    private BasicType convertSingleToBasicType(org.eclipse.uml2.uml.NamedElement targetElement, Object value, Classifier targetType) {
        if (value == null)
            return null;
        if (value instanceof Instance)
            return convertToRuntimeObject((Instance) value);
        if (value instanceof Tuple)
            return convertToValueObject((Tuple) value);
        if (targetType instanceof Enumeration) {
            EnumerationLiteral ownedLiteral = ((Enumeration) targetType).getOwnedLiteral((String) value);
            return ownedLiteral == null ? null : new EnumerationType(ownedLiteral);
        }
        if (targetType instanceof StateMachine) {
            Vertex vertex = StateMachineUtils.getVertex((StateMachine) targetType, (String) value);
            return vertex == null ? null : new StateMachineType(vertex);
        }
        try {
            return PrimitiveType.convertToBasicType(targetType, value);
        } catch (ValueConverter.ConversionException e) {
            throw new KirraException(e.toString(), e, Kind.VALIDATION, targetElement.getName(), null);
        }
    }

    private CollectionType convertToCollectionType(Collection<?> values, MultiplicityElement targetElement) {
        Classifier targetType = (Classifier) ((TypedElement) targetElement).getType();
        CollectionType targetCollection = CollectionType.createCollectionFor(targetElement);
        for (Object value : values)
            targetCollection.add(convertToBasicType(value, (org.eclipse.uml2.uml.MultiplicityElement) targetElement));
        return targetCollection;
    }

    private RuntimeObject convertToRuntimeObject(Instance instance) {
        final boolean first = KirraOnMDDRuntime.convertedToRuntimeObject.isEmpty();
        Entity entity = getEntity(instance.getEntityNamespace(), instance.getEntityName());
        try {
            RuntimeObject alreadyConverted = KirraOnMDDRuntime.convertedToRuntimeObject.get(instance);
            if (alreadyConverted != null)
                return alreadyConverted;
            final RuntimeClass runtimeClass = getRuntimeClass(instance);
            RuntimeObject target;
            if (instance.isNew())
            	// assign defaults as not all values may be provided
                target = runtimeClass.newInstance(true, true);
            else {
                target = findRuntimeObject(runtimeClass, instance.getObjectId());
                if (target == null)
                    throw new KirraException(instance.getEntityName() + " id " + instance.getObjectId() + " not found", null, Kind.ENTITY);
            }
            KirraOnMDDRuntime.convertedToRuntimeObject.put(instance, target);
            
            // note that below we ignore attempts to modify read only properties/links
            // but allow them to be set on creation to support data snapshot loading 
            Classifier clazz = target.getRuntimeClass().getModelClassifier();
            for (Entry<String, Object> entry : instance.getValues().entrySet()) {
                org.eclipse.uml2.uml.Property attribute = FeatureUtils.findAttribute(clazz, entry.getKey(), false, true);
                if (attribute == null)
                    throw new KirraException("Attribute " + entry.getKey() + " does not exist", null, Kind.SCHEMA);
                if (KirraHelper.isDerived(attribute) || !instance.isNew()
                        && (KirraHelper.isReadOnly(attribute) || attribute.getType() instanceof StateMachine))
                    continue;
                target.setValue(attribute, convertToBasicType(entry.getValue(), attribute));
            }
            for (Entry<String, List<Instance>> entry : instance.getLinks().entrySet()) {
                Relationship relationship = entity.getRelationship(entry.getKey());
                if (relationship == null)
                    throw new KirraException("Relationship " + entry.getKey() + " does not exist", null, Kind.SCHEMA);
                if (relationship.isDerived())
                    continue;
                org.eclipse.uml2.uml.Property attribute = AssociationUtils.findMemberEnd(clazz, entry.getKey());
                if (attribute == null)
                    throw new KirraException("Attribute " + entry.getKey() + " does not exist", null, Kind.SCHEMA);
                org.eclipse.uml2.uml.Property otherEnd = attribute.getOtherEnd();
                if (attribute.getAssociation() == null)
                    throw new KirraException("Attribute " + attribute.getQualifiedName() + " is not an association end", null, Kind.SCHEMA);
                if (KirraHelper.isDerived(attribute) || !instance.isNew() && (KirraHelper.isReadOnly(attribute) || (otherEnd != null && KirraHelper.isReadOnly(otherEnd)) || !attribute.isNavigable())) 
                	// just ignore
                	continue;
                target.setValue(attribute, convertToBasicType(entry.getValue(), attribute));
            }
            if (instance.isNew())
                target.initDefaults();
            else
                target.save();
            return target;
        } finally {
            if (first)
                KirraOnMDDRuntime.convertedToRuntimeObject.clear();
        }
    }

    private Tuple convertToTuple(RuntimeObject source) {
        Classifier modelClassifier = source.getRuntimeClass().getModelClassifier();
        Tuple tuple = new Tuple(KirraHelper.convertType(modelClassifier));
        EList<org.eclipse.uml2.uml.Property> allAttributes = modelClassifier.getAllAttributes();
        for (org.eclipse.uml2.uml.Property property : allAttributes) {
            BasicType value = source.getValue(property);
            if (value == null)
                continue;
            // convert to client format
            Object converted = convertFromBasicType(value, (Classifier) property.getType());
            tuple.setValue(KirraHelper.getName(property), converted);
        }
        return tuple;
    }

    private RuntimeObject convertToValueObject(Tuple instance) {
        getTupleType(instance.getScopeNamespace(), instance.getScopeName());
        final RuntimeClass runtimeClass = getRuntimeClass(instance);
        RuntimeObject target = runtimeClass.newInstance(false, false);
        Classifier clazz = target.getRuntimeClass().getModelClassifier();
        for (Entry<String, Object> entry : instance.getValues().entrySet()) {
            org.eclipse.uml2.uml.Property attribute = FeatureUtils.findAttribute(clazz, entry.getKey(), false, true);
            if (attribute == null)
                throw new KirraException("Attribute " + entry.getKey() + " does not exist", null, Kind.SCHEMA);
            target.setValue(attribute, convertToBasicType(entry.getValue(), attribute));
        }
        return target;
    }

    private List<?> executeEntityOperation(Classifier umlClass, String externalId, com.abstratt.kirra.Operation kirraOperation,
            List<?> arguments) {
        org.eclipse.uml2.uml.Operation operation = mapOperation(umlClass, kirraOperation);

        BasicType target = null;
        if (externalId == null) {
            if (!operation.isStatic())
                throw new KirraException("Operation " + operation.getQualifiedName() + " is not static", null, Kind.SCHEMA);
        } else {
            if (operation.isStatic())
                throw new KirraException("Operation " + operation.getQualifiedName() + " is static", null, Kind.SCHEMA);
            target = findRuntimeObject(getRuntime().getRuntimeClass(umlClass), externalId);
            if (target == null)
                throw new KirraException("Object does not exist", null, Kind.ENTITY);
        }

        return basicExecuteOperation(target, operation, arguments);
    }

    private List<?> executeServiceOperation(Classifier classifier, com.abstratt.kirra.Operation operation, List<?> arguments) {
        BasicType providingInstance = getRuntime().getInstance(classifier);
        switch (operation.getKind()) {
        case Retriever:
            return basicExecuteOperation(providingInstance, mapOperation(classifier, operation), arguments);
        case Event:
            TypeRef tupleType = operation.getParameters().get(0).getTypeRef();
            RuntimeObject event = convertToValueObject((Tuple) arguments.get(0));
            Signal signal = getModelElement(tupleType.getEntityNamespace(), tupleType.getTypeName(), UMLPackage.Literals.SIGNAL);
            providingInstance.getMetaClass().handleEvent(RuntimeMessageEvent.build(signal, providingInstance, event));
            return null;
        default:
            Assert.isTrue(false, "Unexpected operation: " + operation.getKind());
        }
        // should never run
        throw new Error();
    }

    private RuntimeObject findRuntimeObject(RuntimeClass runtimeClass, String objectId) {
        try {
            return runtimeClass.getInstance(objectId);
        } catch (NotFoundException nfe) {
            return null;
        }
    }

    private RuntimeObject findRuntimeObject(String namespace, String name, String objectId) {
        RuntimeClass runtimeClass = getRuntimeClass(namespace, name, UMLPackage.Literals.CLASS);
        return findRuntimeObject(runtimeClass, objectId);
    }

    private ExternalService getExternalService() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(ExternalService.class);
    }

    private NamedElementLookupCache getLookup() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(NamedElementLookupCache.class);
    }

    private String getObjectId(RuntimeObject source) {
        return source.getObjectId();
    }

    private Runtime getRuntime() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(Runtime.class);
    }

    private RuntimeClass getRuntimeClass(String namespace, String name, EClass eClass) {
        Classifier class_ = getModelElement(namespace, name, eClass);
        return getRuntime().getRuntimeClass(class_);
    }

    private RuntimeClass getRuntimeClass(Tuple kirraInstance) {
        String name = kirraInstance.getScopeName();
        String namespace = kirraInstance.getScopeNamespace();
        return getRuntimeClass(namespace, name, UMLPackage.Literals.CLASSIFIER);
    }

    private SchemaManagement getSchemaManagement() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(SchemaManagement.class);
    }

    private org.eclipse.uml2.uml.Operation mapOperation(Classifier umlClass, com.abstratt.kirra.Operation kirraOperation) {
        org.eclipse.uml2.uml.Operation operation = FeatureUtils.findOperation(getRepository(), umlClass, kirraOperation.getName(), null);
        if (operation == null)
            throw new KirraException("Operation '" + kirraOperation.getName() + "' does not exist in " + umlClass.getQualifiedName(), null,
                    Kind.ENTITY);
        return operation;
    }

    private void validateValues(Instance kirraInstance) {
        validateValues(kirraInstance, new HashSet<Instance>());
    }

    private void validateValues(Instance kirraInstance, Set<Instance> visited) {
        if (!validating || visited.contains(kirraInstance))
            return;
        visited.add(kirraInstance);
        Classifier modelClass = getModelElement(kirraInstance.getEntityNamespace(), kirraInstance.getEntityName(),
                UMLPackage.Literals.CLASSIFIER);
        if (modelClass == null)
            throw new IllegalArgumentException("Class not found for " + kirraInstance.getEntityName() + " ("
                    + kirraInstance.getEntityNamespace() + ")");
        Entity entity = getSchemaManagement().getEntity(kirraInstance.getTypeRef());
        for (org.eclipse.uml2.uml.Property property : modelClass.getAllAttributes()) {
            if (property.getAssociation() == null) {
                if (!KirraHelper.isProperty(property) || KirraHelper.isReadOnly(property) || !KirraHelper.isRequired(property))
                    continue;
                DataElement entityProperty = entity.getProperty(property.getName());
                BasicType value = this.convertToBasicType(kirraInstance.getValue(KirraHelper.getName(property)), property);
                if (value == null || value.isEmpty())
                    throw new KirraException("A value is required for " + entityProperty.getLabel(), null, Kind.VALIDATION);
            } else {
                // ensure provided links are to preexisting instances
                Relationship entityRelationship = entity.getRelationship(property.getName());
                List<Instance> linksProvided = kirraInstance.getRelated(KirraHelper.getName(property));
                if (linksProvided != null)
                    for (Instance linkedInstance : linksProvided)
                        if (linkedInstance.isNew()) 
                            throw new KirraException("Only previously persisted instances can be provided as links for " + entityRelationship.getLabel(), null, Kind.VALIDATION);
                // TODO relationship validation disabled temporarily for release
                // List<Instance> related =
                // kirraInstance.getRelated(KirraHelper.getName(property));
                // if (related == null || related.isEmpty()) {
                // if (property.getLower() > 0)
                // throw new
                // KirraException("An associated object is required for " +
                // entityProperty.getLabel(), null, Kind.VALIDATION);
                // } else {
                // for (Instance instance : related)
                // validateValues(instance, visited);
                // }
            }
        }
    }
}