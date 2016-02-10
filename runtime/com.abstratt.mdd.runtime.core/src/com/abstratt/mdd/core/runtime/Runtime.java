package com.abstratt.mdd.core.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.core.runtime.Assert;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.StructuredActivityNode;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.Variable;
import org.eclipse.uml2.uml.VisibilityKind;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.runtime.external.ExternalMetaClass;
import com.abstratt.mdd.core.runtime.external.ExternalObject;
import com.abstratt.mdd.core.runtime.external.ExternalObjectDelegate;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.BuiltInMetaClass;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.ConnectorUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.nodestore.INodeKey;
import com.abstratt.nodestore.INodeStoreCatalog;
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

    // only one external metaClass instance for the entire runtime
    private ExternalMetaClass externalMetaClass = (ExternalMetaClass) ExternalObject.META_CLASS;

    private IRepository repository;

    private ExecutionContext context;

    private ActorSelector actorSelector;

    public Runtime(IRepository repository, INodeStoreCatalog catalog, ActorSelector actorSelector) {
        this.repository = repository;
        this.nodeStoreCatalog = catalog;
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
        return collectInstancesFromHierarchy((Classifier) baseClass, includeSubclasses, currentClass -> getRuntimeClass(currentClass).getAllInstances());
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
    
    public List<RuntimeObject> findInstances(final Classifier baseClass, Map<Property, List<BasicType>> criteria) {
    	return findInstances(baseClass, criteria, false);
    }

    public List<RuntimeObject> findInstances(final Classifier baseClass, Map<Property, List<BasicType>> criteria, boolean includeSubclasses) {
        if (criteria.isEmpty())
            return getAllInstances(baseClass, includeSubclasses);
        return collectInstancesFromHierarchy(baseClass, includeSubclasses, currentClass -> getRuntimeClass(currentClass).filterInstances(criteria));
    }

    private List<RuntimeObject> collectInstancesFromHierarchy(Classifier baseClass, boolean includeSubclasses, Function<Classifier, Collection<RuntimeObject>> collector) {
    	return RuntimeUtils.collectInstancesFromHierarchy(getRepository(), new ArrayList<RuntimeObject>(), baseClass, includeSubclasses, collector);
    }

    public RuntimeObject findOneInstance(Class baseClass, Map<Property, List<BasicType>> criteria) {
        RuntimeClass runtimeClass = getRuntimeClass(baseClass);
        return runtimeClass.findOneInstance(criteria);
    }

    public ExecutionContext[] getContexts() {
        throw new UnsupportedOperationException("No support for multiple lines of execution");
    }

    public RuntimeObject getCurrentActor() {
        return actorSelector.getCurrentActor(this);
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

    public void registerExternalDelegate(ExternalObjectDelegate externalDelegate) {
        this.externalMetaClass.register(externalDelegate);
    }

    public BasicType runBehavior(RuntimeObject target, String frameName, Activity behavior, BasicType... arguments) {
        final StructuredActivityNode main = ActivityUtils.getBodyNode(behavior);
        context.newFrame(behavior, target, frameName);
        try {
            int paramIndex = 0;
            for (Variable current : main.getVariables()) {
                context.declareVariable(current);
                if (!"".equals(current.getName()) && current.getVisibility() != VisibilityKind.PRIVATE_LITERAL)
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

    /**
     * Runs an operation against an object.
     */
    public BasicType runOperation(ActorSelector currentActor, final BasicType target, final Operation operation, final BasicType... arguments) {
        Assert.isNotNull(operation);
        return runSession(new Session<BasicType>() {
            @Override
            public BasicType run() {
                return basicRunOperation(target, operation, arguments);
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
        nodeStoreCatalog.zap();
    }

    private BasicType basicRunOperation(BasicType target, Operation operation, BasicType... arguments) {
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
        BasicType result = metaClass.runOperation(context, target, operation, arguments);
        return result;
    }

}
