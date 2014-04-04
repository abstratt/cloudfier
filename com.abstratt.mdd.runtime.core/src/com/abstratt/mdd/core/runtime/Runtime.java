package com.abstratt.mdd.core.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Port;
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
import com.abstratt.mdd.core.util.ClassifierUtils;
import com.abstratt.mdd.core.util.ConnectorUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.nodestore.INodeKey;
import com.abstratt.nodestore.INodeStoreCatalog;

/**
 * The runtime runs executable UML models.
 */
public class Runtime {

	public final static String ID = "com.abstratt.mdd.runtime.core";

	private INodeStoreCatalog nodeStoreCatalog;
	
	// only one external metaClass instance for the entire runtime
	private ExternalMetaClass externalMetaClass = (ExternalMetaClass) ExternalObject.META_CLASS;

	private IRepository repository;

	private ExecutionContext context;

	private ActorSelector actorSelector;

	public static Runtime getCurrentRuntime() {
		return get();
	}
	
	public static Runtime get() {
		if (!RepositoryService.isValidContext())
			return null;
		return RepositoryService.DEFAULT.getCurrentResource().getFeature(Runtime.class);
    }
	
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

	public ExecutionContext[] getContexts() {
		throw new UnsupportedOperationException("No support for multiple lines of execution");
	}
	
	public ExecutionContext getCurrentContext() {
		return context;
	}

	public IRepository getRepository() {
		return repository;
	}

	public RuntimeObject getInstance(Class classifier, INodeKey key) {
		return getRuntimeClass(classifier).getInstance(key);
	}
	
	public BasicType getInstance(Classifier type) {
		if (MDDExtensionUtils.isExternal(type))
			return externalMetaClass.getInstance(type);
		return newInstance(type, false, false);
	}
	
	/**
	 * Returns a runtime class given its static definition. Returns <code>null</code> if one cannot be found.
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

	/**
	 * Returns all instances of the given type. This is different from {@link RuntimeClass#getAllInstances()} because 
	 * that method only includes instances of the exact target type (and not instances of subclasses) and as such 
	 * only works on concrete classes. This method works on any classes, and can return instances of different classes if more 
	 * than one class conforms to the given base class. 
	 * 
	 * @param baseClass
	 * @return
	 */
	public List<BasicType> getAllInstances(final Classifier baseClass) {
		List<Classifier> concreteClasses = ClassifierUtils.findAllSpecifics(getRepository(), baseClass, false);
		List<BasicType> allInstances = new ArrayList<BasicType>();
		for (Classifier concreteClassifier : concreteClasses) {
			RuntimeClass runtimeClass = getRuntimeClass(concreteClassifier);
			allInstances.addAll(runtimeClass.getAllInstances().getBackEnd());
		}
		return allInstances;
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

	public Object runBehavior(RuntimeObject target, String frameName, Activity behavior, Object... arguments) {
		final StructuredActivityNode main = ActivityUtils.getBodyNode(behavior);
		context.newFrame(behavior, target, frameName);
		try {
			int paramIndex = 0;
			for (Variable current : main.getVariables()) {
				context.declareVariable(current);
				if (!"".equals(current.getName()) && current.getVisibility() != VisibilityKind.PRIVATE_LITERAL)
					context.setVariableValue(current, arguments[paramIndex++]);
			}
			// the actual root node (corresponding to the utmost begin...end) is the only node of the main node
			RuntimeAction runtimeBody = createAction(ActivityUtils.getRootAction(behavior), null);
			try {
			     runtimeBody.execute(this.context);
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
	
	public static interface Session<S> {
		S run();
	}
	
	public void enter() {
		context.enter();
	}
	
    public void leave(boolean success) {
    	context.leave(success);
	}
	
	public <S> S runSession(Session<S> session) {
//		enter();
//		boolean operationSucceeded = false;
//		try {
			S result = session.run();
//			operationSucceeded = true;
			return result;
//		} finally {
//	        leave(operationSucceeded);
//		}
	}

	public Object runOperation(BasicType target, Operation operation, Object... arguments) {
		return runOperation(null, target, operation, arguments);
	}
	
	public void sendSignal(final BasicType target, final RuntimeObject runtimeSignal) {
		getCurrentContext().publishEvent(new RuntimeMessageEvent<Signal>((Signal) runtimeSignal.getRuntimeClass().getModelClassifier(), target, runtimeSignal));
	}
	
	/**
	 * Runs an operation against an object.
	 */
	public Object runOperation(ActorSelector currentActor, final BasicType target, final Operation operation, final Object... arguments) {
		Assert.isNotNull(operation);
		return runSession(new Session<Object>() {
			@Override
			public Object run() {
				return basicRunOperation(target, operation,
						arguments);
			}
		});
	}

	private Object basicRunOperation(
			BasicType target, Operation operation, Object... arguments) {
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
		Object result = metaClass.runOperation(context, target, operation, arguments);
		return result;
	}

    public INodeStoreCatalog getNodeStoreCatalog() {
        return nodeStoreCatalog;
    }

	public void zap() {
		nodeStoreCatalog.zap();
	}

	public void prime() {
		nodeStoreCatalog.prime();
	}

	public Collection<BasicType> getRelatedInstances(Class umlClass, String externalId, org.eclipse.uml2.uml.Property property) {
		return getRuntimeClass(umlClass).getRelatedInstances(externalId, property).getBackEnd();
	}
	
	public Collection<BasicType> getParameterDomain(Class umlClass, String externalId, org.eclipse.uml2.uml.Parameter parameter) {
		return getRuntimeClass(umlClass).getParameterDomain(externalId, parameter).getBackEnd();
	}
	
	public Collection<BasicType> getPropertyDomain(Class umlClass, String objectId,
			org.eclipse.uml2.uml.Property property) {
		return getRuntimeClass(umlClass).getPropertyDomain(objectId, property).getBackEnd();
	}

	public boolean isActive() {
		return getRepository().isOpen() && nodeStoreCatalog.isInitialized();
	}

	public void saveContext(boolean preserve) {
		getCurrentContext().saveContext(preserve);
	}

	public void registerExternalDelegate(ExternalObjectDelegate externalDelegate) {
		this.externalMetaClass.register(externalDelegate);
	}

	public BasicType getProviderInstance(Port port) {
		return getInstance(ConnectorUtils.findProvidingClassifier(port));
	}

	public RuntimeObject getCurrentActor() {
		return actorSelector.getCurrentActor(this);
	}


}
