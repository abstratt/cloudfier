package com.abstratt.mdd.core.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Assert;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.ParameterSet;
import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.StructuredActivityNode;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.Variable;
import org.eclipse.uml2.uml.VisibilityKind;

import com.abstratt.blobstore.IBlobStoreCatalog;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.runtime.external.ExternalMetaClass;
import com.abstratt.mdd.core.runtime.external.ExternalObject;
import com.abstratt.mdd.core.runtime.external.ExternalObjectDelegate;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.BuiltInMetaClass;
import com.abstratt.mdd.core.runtime.types.EnumerationType;
import com.abstratt.mdd.core.runtime.types.StateMachineType;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.ClassifierUtils;
import com.abstratt.mdd.core.util.ConnectorUtils;
import com.abstratt.mdd.core.util.FeatureUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.nodestore.INode;
import com.abstratt.nodestore.INodeKey;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.nodestore.NodeReference;
import com.abstratt.pluginutils.LogUtils;

/**
 * The runtime runs executable UML models.
 */
public class Runtime {

    public static interface Session<S> {
        S run();
    }

    public static Runtime get() {
        if (!RepositoryService.isValidContext())
            return null;
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(Runtime.class);
    }

    public static Runtime getCurrentRuntime() {
        return Runtime.get();
    }

    public final static String ID = "com.abstratt.mdd.runtime.core";

    private INodeStoreCatalog nodeStoreCatalog;
    
    private IBlobStoreCatalog blobStoreCatalog;

    // only one external metaClass instance for the entire runtime
    private ExternalMetaClass externalMetaClass = (ExternalMetaClass) ExternalObject.META_CLASS;

    private IRepository repository;
    
    // a cache of classifier hierarchies
    private Map<Classifier, Collection<Classifier>> hierarchies = new LinkedHashMap<>();

    private ExecutionContext context;

    private ActorSelector actorSelector;

    public Runtime(IRepository repository, INodeStoreCatalog nodeStoreCatalog, IBlobStoreCatalog blobStoreCatalog, ActorSelector actorSelector) {
        this.repository = repository;
        this.nodeStoreCatalog = nodeStoreCatalog;
        this.blobStoreCatalog = blobStoreCatalog;        
        this.actorSelector = actorSelector;
        this.context = new ExecutionContext(this);
    }

    public RuntimeAction createAction(Action instance, CompositeRuntimeAction parent) {
        // default factory
        return RuntimeActionFactory.getInstance().createRuntimeAction(instance, parent);
    }

    public void enter(boolean readOnly) {
        context.enter(readOnly);
    }

    public List<RuntimeObject> getAllInstances(final Classifier baseClass, boolean includeSubclasses) {
        return collectInstancesFromHierarchy((Classifier) baseClass, includeSubclasses, currentClass -> getAllInstances(currentClass));
    }

    private Collection<RuntimeObject> getAllInstances(Classifier currentClass) {
        Collection<RuntimeObject> allInstances = getRuntimeClass(currentClass).getAllInstances();
        return allInstances;
    }
    
    public Collection<RuntimeObject> getParameterDomain(Class baseClass, String externalId, org.eclipse.uml2.uml.Parameter parameter, boolean includeSubclasses) {
    	RuntimeClass runtimeTargetClass = getRuntimeClass(baseClass);
    	return collectInstancesFromHierarchy((Classifier) parameter.getType(), includeSubclasses, currentClass -> runtimeTargetClass.getParameterDomain(externalId, parameter, currentClass));
    }
    
    public Collection<RuntimeObject> getPropertyDomain(Class targetClass, String objectId, org.eclipse.uml2.uml.Property property, boolean includeSubclasses) {
    	RuntimeClass runtimeTargetClass = getRuntimeClass(targetClass);
    	return collectInstancesFromHierarchy((Classifier) property.getType(), includeSubclasses, currentClass -> runtimeTargetClass.getPropertyDomain(objectId, property, currentClass));
    }

    public Collection<RuntimeObject> getRelatedInstances(Class umlClass, String objectId, org.eclipse.uml2.uml.Property property) {
    	RuntimeClass targetClass = getRuntimeClass(umlClass);
    	return targetClass.getRelatedInstances(objectId, property);
    }
    
    public List<RuntimeObject> findInstances(final Classifier baseClass, Map<Property, List<BasicType>> criteria, boolean includeSubclasses) {
        if (criteria.isEmpty())
            return getAllInstances(baseClass, includeSubclasses);
        return collectInstancesFromHierarchy(baseClass, includeSubclasses,
                currentClass -> getRuntimeClass(currentClass).findInstances(criteria, null));
    }

    protected List<RuntimeObject> collectInstancesFromHierarchy(Classifier baseClass, boolean includeSubclasses,
            Function<Classifier, Collection<RuntimeObject>> collector) {
        Stream<Classifier> classes = streamHierarchy(baseClass, includeSubclasses);
        return RuntimeUtils.collectInstancesFromClasses(new ArrayList<RuntimeObject>(), classes, collector);
    }

    private Stream<Classifier> streamHierarchy(Classifier baseClass, boolean includeSubclasses) {
        if (!includeSubclasses)
            return Stream.of(baseClass);
        // cache hierarchy as they are expensive to compute
        Collection<Classifier> subclasses = this.hierarchies.computeIfAbsent(baseClass, it -> ClassifierUtils.findAllSpecifics(repository, baseClass));
        return Stream.concat(Stream.of(baseClass), subclasses.stream());
    }

    public RuntimeObject findOneInstance(Class baseClass, Map<Property, List<BasicType>> criteria) {
        RuntimeClass runtimeClass = getRuntimeClass(baseClass);
        return runtimeClass.findOneInstance(criteria);
    }

    public ExecutionContext[] getContexts() {
        throw new UnsupportedOperationException("No support for multiple lines of execution");
    }

    public RuntimeObject getCurrentActor() {
        RuntimeObject currentActor = actorSelector.getCurrentActor(this);
		return currentActor;
    }
    
    public List<RuntimeObject> getRolesForActor(RuntimeObject actor) {
    	if (actor == null)
    		return Collections.emptyList();
    	Classifier actorClass = actor.getRuntimeClass().getModelClassifier();
    	
    	// role classes are those 
    	Collection<Class> roleClasses = actorClass.getAssociations().stream()
    			.flatMap(association -> association.getOwnedEnds().stream())
				.filter(end -> MDDExtensionUtils.isRoleClass(end.getType()))
				.map(end -> (Classifier) end.getType())
				.flatMap(roleClass -> Stream.concat(Stream.of(roleClass), ClassifierUtils.findAllSpecifics(repository, roleClass).stream()))
				.map(it -> (Class) it)
				.collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
    	
    	Collection<RuntimeObject> roles = roleClasses.stream().map(roleClass -> getRoleForActor(actor, roleClass)).filter(it -> it != null).collect(Collectors.toList());
    	List<RuntimeObject> result = roles.stream().collect(Collectors.toList());
		return result;
    }
    
    /**
     * Returns the role matching the specified role class for the current actor.
     */
    public RuntimeObject getRoleForActor(RuntimeObject actor, Classifier roleClass) {
    	Property userProperty = FeatureUtils.findAttribute(roleClass, "userProfile", false, true);
    	Map<Property, List<BasicType>> criteria = Collections.singletonMap(userProperty, Arrays.asList(actor));
    	RuntimeClass runtimeRoleClass = context.getRuntime().getRuntimeClass(roleClass);
		RuntimeObject role = runtimeRoleClass.findOneInstance(criteria);
		return role;
    }
    
    public RuntimeObject getActorForRole(RuntimeObject role) {
        Property userProfileProperty = FeatureUtils.findAttribute(role.getRuntimeClass().getModelClassifier(), "userProfile", false, true);
        return (RuntimeObject) role.traverse(userProfileProperty);
    }


    public ActorSelector getActorSelector() {
        return actorSelector;
    }

    /**
     * For testing only.
     */
    public void setActorSelector(ActorSelector newSelector) {
		actorSelector = newSelector;
	}

    public ExecutionContext getCurrentContext() {
        return context;
    }

    public RuntimeObject getInstance(Class classifier, INodeKey key) {
        return getRuntimeClass(classifier).getInstance(key);
    }

    public BasicType getInstance(Classifier type) {
        if (MDDExtensionUtils.isExternal(type))
            return externalMetaClass.getInstance(type);
        return newInstance(type, false, false);
    }

    public INodeStoreCatalog getNodeStoreCatalog() {
        return nodeStoreCatalog;
    }
    
    public IBlobStoreCatalog getBlobStoreCatalog() {
        return blobStoreCatalog;
    }

    public BasicType getProviderInstance(Port port) {
        return getInstance(ConnectorUtils.findProvidingClassifier(port));
    }

    public IRepository getRepository() {
        return repository;
    }

    /**
     * Returns a runtime class given its static definition. Returns
     * <code>null</code> if one cannot be found.
     */
    public RuntimeClass getRuntimeClass(Classifier classifier) {
        Assert.isNotNull(classifier);
        return RuntimeClass.newClass(classifier, this);
    }

    public RuntimeClass getRuntimeClass(String className) {
        Classifier classifier = this.getRepository().findNamedElement(className, UMLPackage.Literals.CLASSIFIER, null);
        Assert.isLegal(classifier != null, "Classifier not found: " + className);
        return getRuntimeClass(classifier);
    }

    public boolean isActive() {
        return getRepository().isOpen() && nodeStoreCatalog.isInitialized();
    }

    public void leave(boolean success) {
        context.leave(success);
    }

    public RuntimeObject newInstance(Classifier classifier) {
        return newInstance(classifier, true);
    }

    public RuntimeObject newInstance(Classifier classifier, boolean persistent) {
        RuntimeClass runtimeClass = getRuntimeClass(classifier);
        return runtimeClass.newInstance(persistent);
    }

    public RuntimeObject newInstance(Classifier classifier, boolean persistent, boolean initDefaults) {
        RuntimeClass runtimeClass = getRuntimeClass(classifier);
        return runtimeClass.newInstance(persistent, initDefaults);
    }

    public void prime() {
        nodeStoreCatalog.prime();
    }
    
    public MetaClass<?> getMetaClass(Classifier classifier) {
    	if (classifier instanceof Enumeration)
    		return EnumerationType.META_CLASS;
    	if (classifier instanceof StateMachine)
    		return StateMachineType.META_CLASS;
    	if (BuiltInMetaClass.isBuiltIn(classifier.getQualifiedName()))
			return BuiltInMetaClass.findBuiltInClass(classifier.getQualifiedName());
    	return getRuntimeClass(classifier);
    }

    public void registerExternalDelegate(ExternalObjectDelegate externalDelegate) {
        this.externalMetaClass.register(externalDelegate);
    }

    public BasicType runBehavior(RuntimeObject target, String frameName, Activity behavior, ParameterSet parameterSet, BasicType... arguments) {
        final StructuredActivityNode main = ActivityUtils.getBodyNode(behavior);
        context.newFrame(behavior, target, frameName);
        try {
            int paramIndex = 0;
            for (Variable current : main.getVariables()) {
                context.declareVariable(current);
                if (!"".equals(current.getName()) && current.getVisibility() != VisibilityKind.PRIVATE_LITERAL)
                    if (parameterSet == null || parameterSet.getParameter(current.getName(), null) != null)
                        context.setVariableValue(current, arguments[paramIndex++]);
            }
            // the actual root node (corresponding to the utmost begin...end) is
            // the only node of the main node
            RuntimeAction runtimeBody = createAction(ActivityUtils.getRootAction(behavior), null);
            try {
                runtimeBody.execute(this.context);
            } catch (NullPointerException npe) {
                LogUtils.logError(Runtime.ID, null, npe);
                throw new ModelExecutionException("Null was dereferenced", behavior.getSpecification(), runtimeBody, context.getCallSites());
            } catch (ActivityFinishedException e) {
                // activity execution finished
            }
            // return value
            Variable returnVariable = main.getVariable("", null);
            return returnVariable != null ? context.getVariableValue(returnVariable) : null;
        } finally {
            context.dropFrame();
        }
    }

    public BasicType runOperation(ActorSelector currentActor, final BasicType target, final Operation operation, final BasicType... arguments) {
        return runOperation(currentActor, target, operation, (ParameterSet) null, arguments); 
    }

    
    /**
     * Runs an operation against an object.
     */
    public BasicType runOperation(ActorSelector currentActor, final BasicType target, final Operation operation, ParameterSet parameterSet, final BasicType... arguments) {
        Assert.isNotNull(operation);
        return runSession(new Session<BasicType>() {
            @Override
            public BasicType run() {
                return basicRunOperation(target, operation, parameterSet, arguments);
            }
        });
    }

    public BasicType runOperation(BasicType target, Operation operation, BasicType... arguments) {
        return runOperation(null, target, operation, arguments);
    }

    public <S> S runSession(Session<S> session) {
        // enter();
        // boolean operationSucceeded = false;
        // try {
        S result = session.run();
        // operationSucceeded = true;
        return result;
        // } finally {
        // leave(operationSucceeded);
        // }
    }

    public void saveContext(boolean preserve) {
        getCurrentContext().saveContext(preserve);
    }

    public void sendSignal(final BasicType target, final RuntimeObject runtimeSignal) {
        getCurrentContext().publishEvent(
                new RuntimeMessageEvent<Signal>((Signal) runtimeSignal.getRuntimeClass().getModelClassifier(), target, runtimeSignal));
    }

    public void zap() {
    	getCurrentContext().clearWorkingSet();
        nodeStoreCatalog.zap();
        blobStoreCatalog.zap();
    }

    private BasicType basicRunOperation(BasicType target, Operation operation, ParameterSet parameterSet, BasicType... arguments) {
        MetaClass<?> metaClass;
        String className = target == null ? operation.getClass_().getQualifiedName() : target.getClassifierName();
        if (target != null)
            metaClass = target.getMetaClass();
        else if (BuiltInMetaClass.isBuiltIn(className))
            metaClass = BuiltInMetaClass.findBuiltInClass(className);
        else
            metaClass = getRuntimeClass(className);
        if (operation.isStatic())
            Assert.isLegal(target == null, "operation '" + operation.getQualifiedName() + "' is static, wrong target");
        else
            Assert.isLegal(target != null, "operation '" + operation.getQualifiedName() + "' is not static, wrong target");
        BasicType result = metaClass.runOperation(context, target, operation, parameterSet, arguments);
        return result;
    }

	public boolean isClassInstance(Classifier classifier, BasicType toTest) {
		if (!(toTest instanceof RuntimeObject)) {
			return false;
		}
		RuntimeObject asRuntimeObject = (RuntimeObject) toTest;
		if (MDDExtensionUtils.isRoleClass(classifier)
				&& MDDExtensionUtils.isSystemUserClass(asRuntimeObject.getRuntimeClass().getModelClassifier())) {
			// special case: casting user to role
			RuntimeObject userAsRole = context.getRuntime().getRoleForActor((RuntimeObject) asRuntimeObject,
					classifier);
			asRuntimeObject = userAsRole;
		}
		boolean result = asRuntimeObject != null && asRuntimeObject.getRuntimeClass().getModelClassifier().conformsTo(classifier);
		return result;
	}

	public Collection<RuntimeObject> getInstances(Collection<NodeReference> relatedNodeRefs) {
		return relatedNodeRefs.stream().map(this::getRuntimeObject).collect(Collectors.toList());
	}

	private RuntimeObject getRuntimeObject(NodeReference ref) {
		return getRuntimeClass(RuntimeClass.fromStoreNameToClassifierName(ref.getStoreName())).getInstance(ref.getKey());
	}
}
