package com.abstratt.kirra.mdd.runtime;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.BehavioralFeature;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.MultiplicityElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.ParameterSet;
import org.eclipse.uml2.uml.Reception;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.TypedElement;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.Vertex;

import com.abstratt.kirra.Blob;
import com.abstratt.kirra.DataElement;
import com.abstratt.kirra.Entity;
import com.abstratt.kirra.EntityCapabilities;
import com.abstratt.kirra.ExternalService;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.InstanceCapabilities;
import com.abstratt.kirra.InstanceRef;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.KirraException.Kind;
import com.abstratt.kirra.Namespace;
import com.abstratt.kirra.Parameter;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Relationship.Style;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.Schema;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.Service;
import com.abstratt.kirra.Tuple;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.kirra.mdd.core.KirraMDDConstants;
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
import com.abstratt.mdd.core.runtime.types.BlobInfo;
import com.abstratt.mdd.core.runtime.types.BlobType;
import com.abstratt.mdd.core.runtime.types.CollectionType;
import com.abstratt.mdd.core.runtime.types.EnumerationType;
import com.abstratt.mdd.core.runtime.types.PrimitiveType;
import com.abstratt.mdd.core.runtime.types.StateMachineType;
import com.abstratt.mdd.core.runtime.types.ValueConverter;
import com.abstratt.mdd.core.util.AccessCapability;
import com.abstratt.mdd.core.util.AccessControlUtils;
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
    private Map<RuntimeObject, Tuple> convertedToInstance = new HashMap<RuntimeObject, Tuple>();

    private Map<Instance, RuntimeObject> convertedToRuntimeObject = new HashMap<Instance, RuntimeObject>();
    
    private boolean convertingTuple = false;

    private static Log log = LogFactory.getLog(KirraOnMDDRuntime.class);

    private boolean validating = true;
    
    private boolean populating = false;

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
        BasicType converted = convertSingleToBasicType((TypedElement) targetElement, value, targetType);
        return converted;
    }

    @Override
    public Instance createInstance(Instance kirraInstance) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        validateValues(kirraInstance);
        try {
            RuntimeObject asRuntimeObject = convertToRuntimeObject(kirraInstance);
            asRuntimeObject.save();

            // execute default constructor if found
            // TODO-RC this is not the right place for it - it should be handled by the runtime itself, 
            // or by the model compiler
            if (!isPopulating()) {
                Classifier modelClassifier = asRuntimeObject.getRuntimeClass().getModelClassifier();
                Optional<? extends Operation> defaultConstructor = FeatureUtils.getBehavioralFeatures(modelClassifier).stream().filter(it -> it instanceof Operation).map(it -> (Operation) it).filter(it -> KirraHelper.isConstructor((Operation) it) && FeatureUtils.getInputParameters(it.getOwnedParameters()).isEmpty()).findFirst();
                defaultConstructor.ifPresent(it -> getRuntime().runOperation(asRuntimeObject, it));
            }
            
            return (Instance) convertFromRuntimeObject(asRuntimeObject, DataProfile.Full);
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
    public void deleteBlob(TypeRef entityRef, String objectId, String blobPropertyName, String blobToken) {
    	RuntimeObject toDelete = findRuntimeObject(entityRef.getEntityNamespace(), entityRef.getTypeName(), objectId);
        if (toDelete == null)
            throw new KirraException("Object does not exist", null, Kind.OBJECT_NOT_FOUND);
        Classifier modelClass = toDelete.getRuntimeClass().getModelClassifier();
        org.eclipse.uml2.uml.Property property = FeatureUtils.findAttribute(modelClass, blobPropertyName, false, true);
        if (property == null)
        	throw new KirraException("Attribute " + blobPropertyName + " does not exist", null, Kind.SCHEMA);
        toDelete.deleteBlob(property, blobToken);
        toDelete.save();
    }
    
    @Override
    public Blob writeBlob(TypeRef entityRef, String objectId, String blobPropertyName, String token, InputStream contents) {
    	RuntimeObject toUpdate = findRuntimeObject(entityRef.getEntityNamespace(), entityRef.getTypeName(), objectId);
        if (toUpdate == null)
            throw new KirraException("Object does not exist", null, Kind.OBJECT_NOT_FOUND);
        Classifier modelClass = toUpdate.getRuntimeClass().getModelClassifier();
        org.eclipse.uml2.uml.Property property = FeatureUtils.findAttribute(modelClass, blobPropertyName, false, true);
        if (property == null)
        	throw new KirraException("Attribute " + blobPropertyName + " does not exist", null, Kind.SCHEMA);
        toUpdate.writeBlob(property, token, contents);
        toUpdate.save();
        Blob result = (Blob) convertFromBasicType(toUpdate.getValue(property), (Classifier) property.getType(), DataProfile.Empty);
        return result;
    }

    @Override
    public Blob createBlob(TypeRef entityRef, String objectId, String blobPropertyName, String contentType, String originalName) {
        RuntimeObject instanceFound = findRuntimeObject(entityRef.getEntityNamespace(), entityRef.getTypeName(), objectId);
        if (instanceFound == null)
            throw new KirraException("Object does not exist", null, Kind.OBJECT_NOT_FOUND);
        Classifier modelClass = instanceFound.getRuntimeClass().getModelClassifier();
        org.eclipse.uml2.uml.Property property = FeatureUtils.findAttribute(modelClass, blobPropertyName, false, true);
        if (property == null)
            throw new KirraException("Attribute " + blobPropertyName + " does not exist", null, Kind.SCHEMA);
        if (!KirraHelper.isBlob(property.getType()))
            throw new KirraException("Attribute " + blobPropertyName + " is not a blob type", null, Kind.SCHEMA);
        Blob kirraValue = new Blob(null, 0L, contentType, originalName);
        BlobType asBlobType = (BlobType) convertToPrimitive(property, kirraValue, (Classifier) property.getType());
        BlobType result = instanceFound.createBlob(property, asBlobType);
        return toBlob(result.primitiveValue());
    }
    
    @Override
    public Blob getBlob(TypeRef entityRef, String objectId, String blobPropertyName, String token) {
        RuntimeObject instanceFound = findRuntimeObject(entityRef.getEntityNamespace(), entityRef.getTypeName(), objectId);
        if (instanceFound == null)
            throw new KirraException("Object does not exist", null, Kind.OBJECT_NOT_FOUND);
        Classifier modelClass = instanceFound.getRuntimeClass().getModelClassifier();
        org.eclipse.uml2.uml.Property property = FeatureUtils.findAttribute(modelClass, blobPropertyName, false, true);
        if (property == null)
            throw new KirraException("Attribute " + blobPropertyName + " does not exist", null, Kind.SCHEMA);
        if (!KirraHelper.isBlob(property.getType()))
            throw new KirraException("Attribute " + blobPropertyName + " is not a blob type", null, Kind.SCHEMA);
        return (Blob) convertFromBasicType(instanceFound.getValue(property), (Classifier) property.getType(), DataProfile.Empty);
    }

    private Blob toBlob(BlobInfo value) {
        return new Blob(value.getToken(), value.getContentLength(), value.getContentType(), value.getOriginalName());
    }
    
    private BlobInfo fromBlob(Blob value) {
        return new BlobInfo(value.getToken(), value.getContentType(), value.getOriginalName(), value.getContentLength());
    }
    
    @Override
    public InputStream readBlob(TypeRef entityRef, String objectId, String blobPropertyName, String token) {
        RuntimeObject instanceFound = findRuntimeObject(entityRef.getEntityNamespace(), entityRef.getTypeName(), objectId);
        if (instanceFound == null)
            throw new KirraException("Object does not exist", null, Kind.OBJECT_NOT_FOUND);
        Classifier modelClass = instanceFound.getRuntimeClass().getModelClassifier();
        org.eclipse.uml2.uml.Property property = FeatureUtils.findAttribute(modelClass, blobPropertyName, false, true);
        if (property == null)
            throw new KirraException("Attribute " + blobPropertyName + " does not exist", null, Kind.SCHEMA);
        if (!KirraHelper.isBlob(property.getType()))
            throw new KirraException("Attribute " + blobPropertyName + " is not a blob type", null, Kind.SCHEMA);
        return instanceFound.readBlob(property, token);
    }

    @Override
    public void dispose() {
        open = false;
        KirraOnMDDRuntime.log.debug("Disposing: " + getRuntime().getRepository().getBaseURI().toString());
        getRuntime().getRepository().dispose();
    }

    @Override
    public List<?> executeOperation(com.abstratt.kirra.Operation operation, String externalId, List<?> arguments) {
        return executeOperation(operation, externalId, arguments, null);
    }
    
    @Override
    public List<?> executeOperation(com.abstratt.kirra.Operation operation, String externalId, List<?> arguments, String parameterSet) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        Classifier umlClass = getModelType(operation.getOwner(), Literals.CLASS);

        if (KirraHelper.isService(umlClass))
            return executeServiceOperation(umlClass, operation, arguments);

        return executeEntityOperation(umlClass, externalId, operation, arguments, parameterSet, DataProfile.Full);
    }

    @Override
    public List<?> executeQuery(com.abstratt.kirra.Operation query, String externalId, List<?> arguments,
            PageRequest pageRequest) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        Classifier umlClass = getModelType(query.getOwner(), Literals.CLASS);
        List<?> allResults = executeEntityOperation(umlClass, externalId, query, arguments, null, pageRequest.getDataProfile());
        List<?> page = allResults.stream().skip(pageRequest.getFirst()).limit(pageRequest.getMaximum()).collect(Collectors.toList());
        return page;
    }
    
    public void flush() {
        convertedToInstance.clear();
        convertedToRuntimeObject.clear();
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
    public String getApplicationLabel() {
    	return getSchemaManagement().getApplicationLabel();
    }
    
    @Override
    public String getApplicationLogo() {
    	return getSchemaManagement().getApplicationLogo();
    }

    @Override
    public String getBuild() {
        return getSchemaManagement().getBuild();
    }
    
    @Override
    public Instance getCurrentUser() {
        RuntimeObject currentActor = getRuntime().getCurrentActor();
        return (Instance) (currentActor != null ? convertFromRuntimeObject(currentActor, DataProfile.Full) : null);
    }
    
    @Override
    public List<Instance> getCurrentUserRoles() {
    	RuntimeObject currentActor = getRuntime().getCurrentActor();
    	List<RuntimeObject> roles = getRuntime().getRolesForActor(currentActor);
    	return roles.stream().map(ro -> (Instance) convertFromRuntimeObject(ro, DataProfile.Slim)).collect(Collectors.toList());
    }

    @Override
    public BasicType getData(Classifier classifier, Operation operation, BasicType... arguments) {
        String namespace = SchemaManagementOperations.getNamespace(classifier);
        List<?> result = getExternalService().executeOperation(namespace, classifier.getName(), operation.getName(),
                convertArgumentsFromBasicType(operation, arguments));
        return convertToBasicType(result, operation.getReturnResult());
    }

    @Override
    public List<com.abstratt.kirra.Operation> getEnabledEntityActions(Entity entity) {
        RuntimeClass runtimeClass = getRuntimeClass(entity.getNamespace(), entity.getName(), Literals.CLASS);
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
    public EntityCapabilities getEntityCapabilities(TypeRef entityRef) {
    	RuntimeClass runtimeClass = getRuntime().getRuntimeClass((Class) getModelType(entityRef, Literals.CLASS));
    	List<AccessCapability> allCapabilities = asList(AccessCapability.values());
    	EntityCapabilities capabilities = new EntityCapabilities();
    	
    	RuntimeObject currentActor = getRuntime().getCurrentActor();
    	List<Class> allRoleClasses = KirraHelper.getRoleEntities(KirraHelper.getApplicationPackages(getRepository().getTopLevelPackages(null)));
		List<RuntimeObject> actualRoles = getRuntime().getRolesForActor(currentActor);
    	List<Class> actualRoleClasses = actualRoles.stream().map(role -> (Class) role.getRuntimeClass().getModelClassifier()).collect(Collectors.toList());
    	
    	List<Operation> allStaticActions = KirraHelper.getEntityActions((Class) runtimeClass.getModelClassifier());
    	List<Operation> allStaticQueries = KirraHelper.getQueries((Class) runtimeClass.getModelClassifier());
    	
    	Function<Collection<Operation>, Map<String, List<String>>> computeOperationCapabilities = staticOperations -> staticOperations.stream().collect(toMap(action -> action.getName(), action -> {
			Map<Classifier, Map<AccessCapability, Constraint>> actionConstraintsPerRole = AccessControlUtils.computeConstraintsPerRoleClass(
					allRoleClasses, 
					allCapabilities,
					asList(runtimeClass.getModelClassifier(), action)
				);
    		boolean isCallAvailable = isCapabilityAvailable(runtimeClass.getClassObject(), AccessCapability.Call, actualRoleClasses, actionConstraintsPerRole);
    		return isCallAvailable ? asList(AccessCapability.Call.name()) : emptyList();
    	})); 
    	
    	capabilities.setActions(computeOperationCapabilities.apply(allStaticActions));
    	capabilities.setQueries(computeOperationCapabilities.apply(allStaticQueries));
		
		Map<Classifier, Map<AccessCapability, Constraint>> explicitConstraintsPerRole = AccessControlUtils.computeConstraintsPerRoleClass(allRoleClasses, allCapabilities, asList(runtimeClass.getModelClassifier()));
		
		Set<AccessCapability> enabledEntityCapabilities = computeActualCapabilities(runtimeClass.getClassObject(),
				actualRoleClasses, asList(AccessCapability.Create, AccessCapability.List), explicitConstraintsPerRole);
		capabilities.setEntity(enabledEntityCapabilities.stream().map(it -> it.name()).collect(toList()));
    	
		return capabilities;
    }
    
    @Override
    public InstanceCapabilities getInstanceCapabilities(TypeRef entity, String objectId) {
    	RuntimeClass runtimeClass = getRuntimeClass(entity.getNamespace(), entity.getTypeName(), Literals.CLASS);
    	RuntimeObject targetObject = findRuntimeObject(runtimeClass, objectId);
    	if (targetObject == null)
    		return null;
    	List<RuntimeObject> actualRoles = getRuntime().getRolesForActor(getRuntime().getCurrentActor());
    	List<Class> actualRoleClasses = actualRoles.stream().map(role -> (Class) role.getRuntimeClass().getModelClassifier()).collect(Collectors.toList());
    	List<Class> allRoleClasses = KirraHelper.getRoleEntities(KirraHelper.getApplicationPackages(getRepository().getTopLevelPackages(null)));    	

    	InstanceCapabilities capabilities = new InstanceCapabilities();
    	
    	// which actions can the current user perform?
    	List<Operation> allInstanceActions = KirraHelper.getInstanceActions((Class) runtimeClass.getModelClassifier());
    	
    	List<AccessCapability> instanceCallCapabilities = asList(AccessCapability.Call);
    	Map<String, List<String>> availableActions = allInstanceActions.stream().collect(toMap(action -> action.getName(), action -> { 
			Map<Classifier, Map<AccessCapability, Constraint>> actionConstraintsPerRole = AccessControlUtils.computeConstraintsPerRoleClass(
				allRoleClasses, 
				instanceCallCapabilities,
				asList(runtimeClass.getModelClassifier(), action)
			);
    		boolean isCallAvailable = isCapabilityAvailable(targetObject, AccessCapability.Call, actualRoleClasses, actionConstraintsPerRole);
    		return isCallAvailable ? asList(AccessCapability.Call.name()) : emptyList();
    	}));
    	capabilities.setActions(availableActions);
    	
    	List<AccessCapability> instanceCrudCapabilities = asList(AccessCapability.Delete, AccessCapability.Update, AccessCapability.Read);
		Map<Classifier, Map<AccessCapability, Constraint>> explicitConstraintsPerRole = AccessControlUtils.computeConstraintsPerRoleClass(
    			allRoleClasses,
    			instanceCrudCapabilities,
    			asList(runtimeClass.getModelClassifier())
			);
    	Set<AccessCapability> enabledInstanceCapabilities = computeActualCapabilities(targetObject,
				actualRoleClasses, instanceCrudCapabilities, explicitConstraintsPerRole);
		capabilities.setInstance(enabledInstanceCapabilities.stream().map(it -> it.name()).collect(toList()));
    	
		return capabilities;
    }

	private Set<AccessCapability> computeActualCapabilities(RuntimeObject targetObject,
			List<Class> actualRoleClasses, List<AccessCapability> consideredCapabilities, Map<Classifier, Map<AccessCapability, Constraint>> explicitConstraintsPerRole) {
		
    	Set<AccessCapability> actualCapabilities = consideredCapabilities.stream().filter(capability -> 
    	    isCapabilityAvailable(targetObject, capability, actualRoleClasses, explicitConstraintsPerRole)
		).collect(toSet());
    	
		return actualCapabilities;
	}

	private boolean isCapabilityAvailable(RuntimeObject targetObject, AccessCapability capability,
			List<Class> actualRoleClasses,
			Map<Classifier, Map<AccessCapability, Constraint>> explicitConstraintsPerRole) {
		boolean areConstraintsDefinedForAnyRole = !explicitConstraintsPerRole.isEmpty() && explicitConstraintsPerRole.values().stream().anyMatch(it -> !it.isEmpty());
		boolean defaultValue = !areConstraintsDefinedForAnyRole;
		if (explicitConstraintsPerRole.isEmpty())
			return true;
		if (actualRoleClasses.isEmpty())
			actualRoleClasses = Arrays.asList((Class) null);
		boolean isAvailable = actualRoleClasses.stream().anyMatch(roleClass ->
			isConstraintSatisfied(targetObject, explicitConstraintsPerRole
				.getOrDefault(roleClass, Collections.emptyMap()
			).get(capability), defaultValue)
		);
		return isAvailable;
	}

	private boolean isConstraintSatisfied(RuntimeObject targetObject, Constraint constraint, boolean defaultValue) {
		if (constraint == null)
			return defaultValue;
		boolean result = targetObject.isConstraintSatisfied(constraint);
		return result;
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
    public Instance getInstance(String namespace, String name, String externalId, DataProfile dataProfile) {
        RuntimeObject found = findRuntimeObject(namespace, name, externalId);
        return (Instance) (found == null ? null : convertFromRuntimeObject(found, dataProfile));
    }

    @Override
    public List<Instance> getInstances(String namespace, String name, DataProfile dataProfile, boolean includeSubclasses) {
        return getInstances(namespace, name, new PageRequest(null, null, dataProfile, includeSubclasses));
    }
    
    @Override
    public List<Instance> getInstances(String namespace, String name, PageRequest pageRequest) {
        Class umlClass = (Class) getModelElement(namespace, name, Literals.CLASS);
        List<RuntimeObject> allInstances = getRuntime().getAllInstances(umlClass, pageRequest.getIncludeSubtypes());
        return filterValidInstances(allInstances, pageRequest.getDataProfile());
    }

    @Override
    public List<Instance> filterInstances(Map<String, List<Object>> kirraCriteria, String namespace, String name, DataProfile dataProfile, boolean includeSubClasses) {
    	Class umlClass = (Class) getModelElement(namespace, name, Literals.CLASS);
    	
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
        return filterValidInstances(getRuntime().findInstances(umlClass, runtimeCriteria, includeSubClasses), dataProfile);
    }

    public <NE extends org.eclipse.uml2.uml.NamedElement> NE getModelElement(String namespace, String name, EClass elementClass) {
        return getLookup().find(SchemaManagementOperations.getQualifiedName(namespace, name), elementClass);
    }
    
    public <NE extends org.eclipse.uml2.uml.NamedElement> NE getModelType(TypeRef typeRef, EClass elementClass) {
        return getLookup().find(SchemaManagementOperations.getQualifiedName(typeRef.getNamespace(), typeRef.getTypeName()), elementClass);
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
        Class umlClass = (Class) getModelElement(entity.getNamespace(), entity.getName(), Literals.CLASS);
        org.eclipse.uml2.uml.Property property = AssociationUtils.findMemberEnd(umlClass, relationship.getName());
        if (!KirraHelper.isRelationship(property))
            throw new KirraException(relationship + " is not a relationship", null, Kind.ENTITY);
        if ("_template".equals(objectId))
            return getInstances(relationship.getTypeRef().getNamespace(), relationship.getTypeRef().getTypeName(), false);
        return filterValidInstances(getRuntime().getPropertyDomain(umlClass, objectId, property, true), DataProfile.Empty);
    }

    @Override
    public List<Instance> getParameterDomain(Entity entity, String externalId, com.abstratt.kirra.Operation action, Parameter parameter) {
        Class umlClass = (Class) getModelElement(entity.getNamespace(), entity.getName(), Literals.CLASS);
        org.eclipse.uml2.uml.Operation operation = FeatureUtils.findOperation(getRepository(), umlClass, action.getName(), null);
        org.eclipse.uml2.uml.Parameter umlParameter = operation.getOwnedParameter(parameter.getName(), null);
        return filterValidInstances(getRuntime().getParameterDomain(umlClass, externalId, umlParameter, true), DataProfile.Empty);
    }

    @Override
    public Properties getProperties() {
        return getRepository().getProperties();
    }

    @Override
    public List<Instance> getRelatedInstances(String namespace, String name, String externalId, String relationship, DataProfile dataProfile) {
        Class umlClass = (Class) getModelElement(namespace, name, Literals.CLASS);
        org.eclipse.uml2.uml.Property property = AssociationUtils.findMemberEnd(umlClass, relationship);
        if (!KirraHelper.isRelationship(property))
            throw new KirraException(relationship + " is not a relationship", null, Kind.ENTITY);
        return filterValidInstances(getRuntime().getRelatedInstances(umlClass, externalId, property), dataProfile);
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
        return getService(typeRef.getEntityNamespace(), typeRef.getTypeName());
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
    public void linkInstances(Relationship relationship, String sourceId, InstanceRef destinationRef) {
        Entity sourceEntity = getEntity(relationship.getOwner());
        Entity targetEntity = getEntity(destinationRef.getEntityNamespace(), destinationRef.getEntityName());
        RuntimeObject source = findRuntimeObject(sourceEntity.getEntityNamespace(), sourceEntity.getName(), sourceId);
        if (source == null)
            throw new KirraException("Source object does not exist", null, Kind.OBJECT_NOT_FOUND);
        RuntimeObject destination = findRuntimeObject(targetEntity.getEntityNamespace(), targetEntity.getName(), destinationRef.getObjectId());
        if (destination == null)
            throw new KirraException("Destination object does not exist", null,
    	 Kind.OBJECT_NOT_FOUND);
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
    	source.link(attribute, destination);
    }

    @Override
    public Instance newInstance(String namespace, String name) {
        KirraOnMDDRuntime.log.debug(getRuntime().getRepository().getBaseURI().toString());
        RuntimeObject newRuntimeObject = convertToRuntimeObject(new Instance(namespace, name));
        Instance newInstance = (Instance) convertFromRuntimeObject(newRuntimeObject, DataProfile.Full);
        newInstance.setObjectId(null);
        return newInstance;
    }

    @Override
    public void receiveSignal(Classifier classifier, Signal signal, BasicType... arguments) {
        String namespace = SchemaManagementOperations.getNamespace(classifier);
        Reception reception = ReceptionUtils.findBySignal(classifier, signal);
        ExternalService externalService = getExternalService();
		externalService.executeOperation(namespace, classifier.getName(), reception.getName(),
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
    public void unlinkInstances(Relationship relationship, String sourceId, InstanceRef destinationRef) {
        Entity sourceEntity = getEntity(relationship.getOwner());
        Entity targetEntity = getEntity(destinationRef.getEntityNamespace(), destinationRef.getEntityName());
        RuntimeObject source = findRuntimeObject(sourceEntity.getEntityNamespace(), sourceEntity.getName(), sourceId);
        if (source == null)
            throw new KirraException("Source object does not exist", null, Kind.OBJECT_NOT_FOUND);
        RuntimeObject destination = findRuntimeObject(targetEntity.getEntityNamespace(), targetEntity.getName(), destinationRef.getObjectId());
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
        	if (!KirraHelper.isEditable(otherEnd))
        		throw new KirraException("Cannot unlink: " + relationship.getName() + ", the other end (" + otherEnd.getName() + ") is read-only", null, Kind.SCHEMA);
        	if (KirraHelper.isRequired(otherEnd) && !KirraHelper.isMultiple(otherEnd) && KirraHelper.getRelationshipStyle(otherEnd) != Style.PARENT)
        		throw new KirraException("Cannot unlink: " + relationship.getName() + ", the other end (" + otherEnd.getName() + ") is required", null, Kind.SCHEMA);
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
            return (Instance) convertFromRuntimeObject(convertToRuntimeObject(kirraInstance), DataProfile.Full);
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

    protected List<Instance> filterValidInstances(Collection<? extends BasicType> allRuntimeObjects, DataProfile dataProfile) {
        List<Instance> allInstances = new ArrayList<Instance>(allRuntimeObjects.size());
        for (BasicType current : allRuntimeObjects) {
            RuntimeObject currentRuntimeObject = (RuntimeObject) current;
            if (!filtering || currentRuntimeObject.checkConstraints(MDDExtensionUtils.ACCESS_STEREOTYPE) == null)
                allInstances.add((Instance) convertFromRuntimeObject(currentRuntimeObject, dataProfile));
        }
        return allInstances;
    }

    protected IRepository getRepository() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(IRepository.class);
    }

	private List<? extends Object> fromObjectToList(Object convertedResult) {
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
    private List<?> basicExecuteOperation(BasicType target, Operation operation, List<?> arguments, String parameterSetName, DataProfile resultDataProfile) {
        ParameterSet parameterSet = parameterSetName == null ? null : operation.getOwnedParameterSet(parameterSetName);
        List<org.eclipse.uml2.uml.Parameter> inParameters = FeatureUtils.filterByParameterSet(parameterSet, FeatureUtils.getInputParameters(operation.getOwnedParameters()));
        if (arguments.size() != inParameters.size())
            throw new KirraException("Operation '" + operation.getName() + "' requires " + inParameters.size() + " arguments, "
                    + arguments.size() + " were provided", null, Kind.ENTITY);
        Map<String, BasicType> argumentsPerParameter = new LinkedHashMap<String, BasicType>();
        BasicType[] convertedArguments = new BasicType[inParameters.size()];
        for (int i = 0; i < inParameters.size(); i++)
            if (arguments.get(i) != null) {
                convertedArguments[i] = convertToBasicType(arguments.get(i), inParameters.get(i));
                argumentsPerParameter.put(KirraHelper.getName(inParameters.get(i)), convertedArguments[i]);
            }

        if (target instanceof RuntimeObject && !((RuntimeObject) target).isEnabledOperation(operation, argumentsPerParameter))
            throw new KirraException("Operation '" + operation.getName() + "' is not available at this time or for this user", null, Kind.VALIDATION);

        try {
            BasicType result = getRuntime().runOperation(null, target, operation, parameterSet, convertedArguments);
            if (result == null)
                return Collections.emptyList();
            if (KirraHelper.isFinder(operation)) {
            	Classifier resultType = (Classifier) operation.getReturnResult().getType();
            	if (KirraHelper.isMultiple(operation.getReturnResult()))
        			return ((CollectionType) result).getBackEnd().stream().map(it -> convertFromBasicType(it, resultType, resultDataProfile)).collect(toList());
        		return Arrays.asList(convertFromBasicType(result, resultType, resultDataProfile));
            }
            Classifier operationType = (Classifier) operation.getType();
            return fromObjectToList(convertFromBasicType(result, operationType, resultDataProfile));
        } catch (ModelExecutionException rre) {
            throw KirraOnMDDRuntime.convertModelExecutionException(rre, Kind.ENTITY);
        }
    }

    private Map<String, Object> convertArgumentsFromBasicType(BehavioralFeature behavioralFeature, BasicType... arguments) {
        List<org.eclipse.uml2.uml.Parameter> parameters = FeatureUtils.getInputParameters(behavioralFeature.getOwnedParameters());
        int required = parameters.size();
        if (required != arguments.length)
            throw new KirraException("Operation " + behavioralFeature.getName() + " requires " + required + " parameters, got "
                    + arguments.length, null, KirraException.Kind.EXTERNAL);
        Map<String, Object> argumentMap = new LinkedHashMap<String, Object>();
        for (int i = 0; i < arguments.length; i++)
            argumentMap.put(parameters.get(i).getName(),
                    convertFromBasicType(arguments[i], (Classifier) parameters.get(i).getType(), DataProfile.Full));
        return argumentMap;
    }

    private Object convertFromBasicType(BasicType value, Classifier sourceType, DataProfile dataProfile) {
        if (value == null)
            return null;
        if (value instanceof CollectionType)
            return convertFromCollectionType((CollectionType) value, sourceType, dataProfile);
        if (value instanceof RuntimeObject)
            return convertFromRuntimeObject((RuntimeObject) value, dataProfile);
        if (value instanceof EnumerationType)
            return ((EnumerationType) value).getValue().getName();
        if (value instanceof StateMachineType)
            return value.toString();
        if (value instanceof PrimitiveType)
            return convertFromPrimitive((PrimitiveType<?>) value);
        throw new IllegalArgumentException(value + " is of an unexpected type: " + sourceType.getQualifiedName());
    }

    private List<?> convertFromCollectionType(CollectionType value, Classifier sourceType, DataProfile dataProfile) {
        if (sourceType instanceof Class || sourceType instanceof DataType) {
            List<Object> result = new ArrayList<Object>(value.getBackEnd().size());
            for (BasicType currentRuntimeObject : value.getBackEnd())
                result.add(convertFromBasicType((BasicType) currentRuntimeObject, sourceType, dataProfile));
            return result;
        }
        List<Object> result = new ArrayList<Object>(value.getBackEnd().size());
        for (BasicType currentPrimitive : value.getBackEnd())
            result.add(convertFromPrimitive((PrimitiveType<?>) currentPrimitive));
        return result;
    }

    private Object convertFromPrimitive(PrimitiveType<?> value) {
        if (value instanceof BlobType) {
            return toBlob(((BlobType) value).primitiveValue());
        }
        Object primitiveValue = value.primitiveValue();
		return primitiveValue;
    }

    private Tuple convertFromRuntimeObject(RuntimeObject source, DataProfile dataProfile) {
        if (source == null)
            return null;
        final boolean first = convertedToInstance.isEmpty();
        Tuple alreadyConverted = convertedToInstance.get(source);
        if (alreadyConverted != null)
        	return alreadyConverted;
        if (convertingTuple || source.isTuple())
            return convertToTuple(source);
        try {
            RuntimeClass class_ = source.getRuntimeClass();
            Instance kirraInstance = new Instance();
            convertedToInstance.put(source, kirraInstance);
            kirraInstance.setProfile(dataProfile);
            Classifier modelClassifier = class_.getModelClassifier();
            kirraInstance.setEntityName(modelClassifier.getName());
            kirraInstance.setEntityNamespace(modelClassifier.getNamespace().getQualifiedName());
            kirraInstance.setObjectId(getObjectId(source));
            List<org.eclipse.uml2.uml.Property> allAttributes = KirraHelper.getPropertiesAndRelationships(modelClassifier);
            org.eclipse.uml2.uml.Property mnemonic = KirraHelper.getMnemonic(modelClassifier);
            for (org.eclipse.uml2.uml.Property property : allAttributes) {
                if (dataProfile == DataProfile.Empty) {
                    if (mnemonic == null || mnemonic != property) {
                        continue;
                    }
                }
                if (property.isDerived() && isPopulating())
                    continue;
                if (KirraHelper.isEntity(property.getType())) {
                    if (!EnumSet.of(DataProfile.Slim, DataProfile.Full).contains(dataProfile)) {
                    	// skip
                    } else if (property.isMultivalued()) {
                        // do nothing: only non-multiple arcs need to be
                        // hydrated
                    } else {
                        BasicType value = source.getValue(property);
                        if (value == null)
                            continue;
                        Object converted = convertFromBasicType(value, (Classifier) property.getType(), dataProfile.lighter());
                        kirraInstance.setRelated(KirraHelper.getName(property), (Instance) converted);
                    }
                } else if (KirraHelper.isTupleType(property.getType())) {
                	BasicType value = source.getValue(property);
                    if (value == null)
                        continue;
                	Object converted = convertFromBasicType(value, (Classifier) property.getType(), DataProfile.Full);
                	kirraInstance.setValue(KirraHelper.getName(property), converted);
                } else if (KirraHelper.isProperty(property)) {
                    BasicType value = source.getValue(property);
                    if (value == null)
                        continue;
                    // convert to client format
                    Object converted = convertFromBasicType(value, (Classifier) property.getType(), DataProfile.Slim);
                    kirraInstance.setValue(KirraHelper.getName(property), converted);
                }
            }
            if (EnumSet.of(DataProfile.Slim, DataProfile.Full).contains(dataProfile)) {
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
	                                        Object converted = convertFromBasicType(value, (Classifier) forwardReference.getType(), dataProfile.lighter());
	                                        kirraInstance
	                                                .setRelated(KirraHelper.getName(forwardReference), (Instance) converted);
	                                    }
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	            kirraInstance.setDisabledActions(computeDisabledActions(source, modelClassifier.getAllOperations()));
            }
            Object shorthand = extractShorthand(kirraInstance, modelClassifier);
            kirraInstance.setShorthand(shorthand == null ? null : shorthand.toString());
            return kirraInstance;
        } finally {
            if (first)
                convertedToInstance.clear();
        }
    }

	private Object extractShorthand(Instance kirraInstance, Classifier modelClassifier) {
		org.eclipse.uml2.uml.Property mnemonic = KirraHelper.getMnemonic(modelClassifier);
		if (mnemonic != null) {
			Object shorthand; 
			if (KirraHelper.isRelationship(mnemonic)) {
				Instance related = kirraInstance.getRelated(KirraHelper.getName(mnemonic));
				shorthand = related == null ? null : related.getShorthand();
			} else
		        shorthand = kirraInstance.getValue(KirraHelper.getName(mnemonic));
		    return shorthand;
		}
		return null;
	}
	private Object extractShorthand(RuntimeObject object, Classifier modelClassifier) {
		org.eclipse.uml2.uml.Property mnemonic = KirraHelper.getMnemonic(modelClassifier);
		if (mnemonic != null) {
			Object shorthand; 
			BasicType mnemonicValue = object.getValue(mnemonic);
			if (mnemonicValue == null)
				return null;
			if (KirraHelper.isRelationship(mnemonic)) {
				RuntimeObject related = (RuntimeObject) mnemonicValue;
				shorthand = extractShorthand(related, related.getRuntimeClass().getModelClassifier());
			} else
		        shorthand = mnemonicValue.toString();
		    return shorthand;
		}
		return null;
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
            Vertex vertex = StateMachineUtils.getState((StateMachine) targetType, (String) value);
            return vertex == null ? null : new StateMachineType(vertex);
        }
        return convertToPrimitive(targetElement, value, targetType);
    }

    private BasicType convertToPrimitive(org.eclipse.uml2.uml.NamedElement targetElement, Object value, Classifier targetType) {
        if (KirraHelper.isBlob(targetType)) {
            return new BlobType(fromBlob((Blob) value));
        }
        try {
            return PrimitiveType.convertToBasicType(targetType, value);
        } catch (ValueConverter.ConversionException e) {
            log.debug("Error converting value: " + value, e);
            throw new KirraException(e.getMessage(), null, Kind.VALIDATION, targetElement.getName(), null);
        }
    }

    private CollectionType convertToCollectionType(Collection<?> values, MultiplicityElement targetElement) {
        CollectionType targetCollection = CollectionType.createCollectionFor(targetElement);
        for (Object value : values)
            targetCollection.add(convertToBasicType(value, (org.eclipse.uml2.uml.MultiplicityElement) targetElement));
        return targetCollection;
    }

    private RuntimeObject convertToRuntimeObject(Instance instance) {
        final boolean first = convertedToRuntimeObject.isEmpty();
        Entity entity = getEntity(instance.getEntityNamespace(), instance.getEntityName());
        try {
            RuntimeObject alreadyConverted = convertedToRuntimeObject.get(instance);
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
            convertedToRuntimeObject.put(instance, target);
            
            // note that below we ignore attempts to modify read only properties/links
            // but allow them to be set on creation to support data snapshot loading 
            Classifier clazz = target.getRuntimeClass().getModelClassifier();
            applyPropertyValues(instance.getValues(), target, clazz);
            for (Entry<String, Instance> entry : instance.getLinks().entrySet()) {
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
                if (KirraHelper.isDerived(attribute) || !populating && !instance.isNew() && (KirraHelper.isReadOnly(attribute) || (otherEnd != null && KirraHelper.isReadOnly(otherEnd)) || !attribute.isNavigable())) {
                	continue;
                }
                target.setValue(attribute, convertToBasicType(entry.getValue(), attribute));
            }
            if (instance.isNew())
                target.initDefaults();
            else
                target.save();
            return target;
        } finally {
            if (first)
                convertedToRuntimeObject.clear();
        }
    }

    private void applyPropertyValues(Map<String, ? extends Object> instanceValues, RuntimeObject target, Classifier clazz) {
        for (Entry<String, ? extends Object> entry : instanceValues.entrySet()) {
            org.eclipse.uml2.uml.Property attribute = FeatureUtils.findAttribute(clazz, entry.getKey(), false, true);
            if (attribute == null)
                throw new KirraException("Attribute " + entry.getKey() + " does not exist", null, Kind.SCHEMA);
            if (KirraHelper.isDerived(attribute) || !populating && target.isPersisted()
                    && (KirraHelper.isReadOnly(attribute) || attribute.getType() instanceof StateMachine))
                continue;
            target.setValue(attribute, convertToBasicType(entry.getValue(), attribute));
        }
    }

    private Tuple convertToTuple(RuntimeObject source) {
    	boolean wasConvertingTuple = convertingTuple;
    	convertingTuple = true;
    	try {
	        Classifier modelClassifier = source.getRuntimeClass().getModelClassifier();
	        Tuple tuple = new Tuple(KirraHelper.convertType(modelClassifier));
	        convertedToInstance.put(source, tuple);
	        EList<org.eclipse.uml2.uml.Property> allAttributes = modelClassifier.getAllAttributes();
	        for (org.eclipse.uml2.uml.Property property : allAttributes) {
	        	if (!KirraHelper.isDerived(property)) {
		            BasicType value = source.getValue(property);
		            if (value == null)
		                continue;
		            Object converted;
		            if (value instanceof RuntimeObject && !((RuntimeObject) value).isTuple())
		            	converted = extractShorthand((RuntimeObject) value, (Classifier) property.getType());
		            else
		            	converted = convertFromBasicType(value, (Classifier) property.getType(), DataProfile.Slim);
		            tuple.setValue(KirraHelper.getName(property), converted);
	        	}
	        }
	        return tuple;
    	} finally {
			convertingTuple = wasConvertingTuple;
    	}
    }

    private RuntimeObject convertToValueObject(Tuple instance) {
        final RuntimeClass runtimeClass = getRuntimeClass(instance);
        RuntimeObject target = runtimeClass.newInstance(false, false);
        Classifier clazz = target.getRuntimeClass().getModelClassifier();
        applyPropertyValues(instance.getValues(), target, clazz);
        return target;
    }

    private List<?> executeEntityOperation(Classifier umlClass, String externalId, com.abstratt.kirra.Operation kirraOperation,
            List<?> arguments, String parameterSet, DataProfile resultProfile) {
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

        return basicExecuteOperation(target, operation, arguments, parameterSet, resultProfile);
    }

    private List<?> executeServiceOperation(Classifier classifier, com.abstratt.kirra.Operation operation, List<?> arguments) {
        BasicType providingInstance = getRuntime().getInstance(classifier);
        switch (operation.getKind()) {
        case Retriever:
            return basicExecuteOperation(providingInstance, mapOperation(classifier, operation), arguments, null, DataProfile.Slim);
        case Event:
            TypeRef tupleType = operation.getParameters().get(0).getTypeRef();
            RuntimeObject event = convertToValueObject((Tuple) arguments.get(0));
            Signal signal = getModelElement(tupleType.getEntityNamespace(), tupleType.getTypeName(), Literals.SIGNAL);
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
        RuntimeClass runtimeClass = getRuntimeClass(namespace, name, Literals.CLASS);
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
        return getRuntimeClass(namespace, name, Literals.CLASSIFIER);
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
        validateValues(kirraInstance, new LinkedHashSet<Instance>());
    }

    private void validateValues(Instance kirraInstance, Set<Instance> visited) {
        if (!validating || visited.contains(kirraInstance))
            return;
        visited.add(kirraInstance);
        Classifier modelClass = getModelElement(kirraInstance.getEntityNamespace(), kirraInstance.getEntityName(),
                Literals.CLASSIFIER);
        if (modelClass == null)
            throw new IllegalArgumentException("Class not found for " + kirraInstance.getEntityName() + " ("
                    + kirraInstance.getEntityNamespace() + ")");
        Entity entity = getSchemaManagement().getEntity(kirraInstance.getTypeRef());
        for (org.eclipse.uml2.uml.Property property : KirraHelper.getPropertiesAndRelationships(modelClass)) {
            if (property.getAssociation() == null) {
                if (!KirraHelper.isProperty(property) || KirraHelper.isReadOnly(property) || !KirraHelper.isRequired(property))
                    continue;
                DataElement entityProperty = entity.getProperty(property.getName());
                BasicType value = this.convertToBasicType(kirraInstance.getValue(KirraHelper.getName(property)), property);
                if ((value == null || value.isEmpty()) && !KirraHelper.hasDefault(property))
                    throw new KirraException("A value is required for " + entityProperty.getLabel(), null, Kind.VALIDATION);
            } else {
                // ensure provided links are to preexisting instances
                Relationship entityRelationship = entity.getRelationship(property.getName());
                Instance linkedInstance = kirraInstance.getRelated(KirraHelper.getName(property));
                if (linkedInstance != null)
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
    
    @Override
    public boolean isPopulating() {
    	return populating;
    }
    
    @Override
    public void setPopulating(boolean isPopulating) {
    	this.populating = isPopulating;
    }
}