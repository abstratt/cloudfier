package com.abstratt.mdd.core.runtime;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.BehavioralFeature;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.ParameterSet;
import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Reception;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.Trigger;
import org.eclipse.uml2.uml.Vertex;

import com.abstratt.blobstore.BlobMetadata;
import com.abstratt.blobstore.IBlobStore;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.BlobInfo;
import com.abstratt.mdd.core.runtime.types.BlobType;
import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.runtime.types.CollectionType;
import com.abstratt.mdd.core.runtime.types.EnumerationType;
import com.abstratt.mdd.core.runtime.types.PrimitiveType;
import com.abstratt.mdd.core.runtime.types.StateMachineType;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.BasicTypeUtils;
import com.abstratt.mdd.core.util.ClassifierUtils;
import com.abstratt.mdd.core.util.ConstraintUtils;
import com.abstratt.mdd.core.util.DataTypeUtils;
import com.abstratt.mdd.core.util.FeatureUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.core.util.ReceptionUtils;
import com.abstratt.mdd.core.util.StateMachineUtils;
import com.abstratt.mdd.core.util.TypeUtils;
import com.abstratt.nodestore.BasicNode;
import com.abstratt.nodestore.INode;
import com.abstratt.nodestore.INodeKey;
import com.abstratt.nodestore.INodeStore;
import com.abstratt.nodestore.NodeReference;
import com.abstratt.pluginutils.LogUtils;

/**
 */
public class RuntimeObject extends StructuredRuntimeObject {

    private INodeKey key;

    /**
     * Use both for transient data or a buffer to the stored node.
     */
    private INode inMemoryState;

    /**
     * Does this object exist in the node store?
     */
    private boolean isPersisted;

    /**
     * Is this object meant to be stored in a node store? Tuples are not, and so
     * aren't objects created in a read-only context.
     */
    private boolean isPersistable;

    /**
     * Does this persistent object have outstanding changes that need to be
     * persisted?
     */
    private boolean isDirty;

    protected final RuntimeClass runtimeClass;

    /**
     * Creates a persistable runtime object that is not persisted yet.
     * 
     * @param runtimeClass
     * @param transientState
     *            unsaved state
     */
    public RuntimeObject(RuntimeClass runtimeClass, INode transientState) {
        Assert.isLegal(transientState.getKey() != null);
        isPersistable = true;
        isPersisted = false;
        this.runtimeClass = runtimeClass;
        setKey(transientState.getKey());
        this.inMemoryState = transientState;
    }

    /**
     * Constructs a transient runtime object that is not suitable for
     * persistence.
     */
    protected RuntimeObject(RuntimeClass runtimeClass) {
        this.runtimeClass = runtimeClass;
        isPersisted = false;
        isPersistable = false;
        this.inMemoryState = new BasicNode(runtimeClass.getNodeStoreName(), (INodeKey) null);
    }

    /**
     * Creates a handle for an object that has already been persisted to the
     * node store.
     *
     * @param runtimeClass
     * @param key
     */
    protected RuntimeObject(RuntimeClass runtimeClass, INodeKey key) {
        Assert.isLegal(key != null);
        isPersistable = true;
        isPersisted = true;
        this.runtimeClass = runtimeClass;
        setKey(key);
    }

    /**
     * Attaches this object to the current context's working set.
     */
    public void attach() {
        Assert.isTrue(isPersistable);
        getCurrentContext().addToWorkingSet(this);
    }

    public Map<String, BasicType> buildArgumentMap(BehavioralFeature behavioralFeature, ParameterSet parameterSet, BasicType... arguments) {
        Map<String, BasicType> argumentsPerParameter = new HashMap<String, BasicType>();
        List<Parameter> inParameters = FeatureUtils.filterByParameterSet(parameterSet, FeatureUtils.getInputParameters(behavioralFeature.getOwnedParameters()));
        Assert.isLegal(arguments.length == inParameters.size(),
                "parameter and argument counts don't match: " + arguments.length + " != " + inParameters.size());
        for (int i = 0; i < inParameters.size(); i++) {
            Parameter parameter = inParameters.get(i);
            if (arguments[i] == null && parameter.getDefaultValue() != null)
                arguments[i] = RuntimeUtils.extractValueFromSpecification(this, parameter.getDefaultValue());
            argumentsPerParameter.put(parameter.getName(), arguments[i]);
        }
        return argumentsPerParameter;
    }

    public Constraint checkConstraints(Classifier scope, String kind) {
        List<Constraint> invariants = ConstraintUtils.findConstraints(scope, kind);
        Constraint partial = checkConstraints(invariants, null,  Collections.<String, BasicType>emptyMap());
        if (partial != null)
            return partial;
        for (Classifier general : scope.getGenerals()) {
            partial = checkConstraints(general, kind);
            if (partial != null)
                return partial;
        }
        return null;
    }

    public Constraint checkConstraints(NamedElement scope, String kind) {
        List<Constraint> constraints = ConstraintUtils.findConstraints(scope, kind);
        return checkConstraints(constraints, null, Collections.<String, BasicType>emptyMap());
    }

    public Constraint checkConstraints(String kind) {
        return checkConstraints(getRuntimeClass().getModelClassifier(), kind);
    }

    public Set<Operation> computeEnabledOperations() {
        Set<Operation> result = new HashSet<Operation>();
        for (org.eclipse.uml2.uml.Operation operation : getRuntimeClass().getModelClassifier().getAllOperations())
            if (isEnabledOperation(operation, null))
                result.add(operation);
        return result;
    }

    public void destroy() {
        ensureActive();
        if (isPersisted)
            getNodeStore().deleteNode(this.getKey());
        getCurrentContext().removeFromWorkingSet(this);
        this.setKey(null);
        this.inMemoryState = null;
        markClear();
    }

    @Override
    public void ensureActive() throws ObjectNotActiveException {
        if (!isActive())
            throw new ObjectNotActiveException(this + " is not active", null);
    }

    /**
     * Ensures this objects invariants are all valid. Throws an exception if any
     * isn't.
     */
    public void ensureValid() {
        if (!isActive())
            return;
        List<Constraint> invariants = MDDExtensionUtils
                .findOwnedInvariantConstraints(getRuntimeClass().getModelClassifier());
        Constraint violated = checkConstraints(invariants, null, Collections.<String, BasicType>emptyMap());
        if (violated != null)
            constraintViolated(ConstraintUtils.getConstraintScope(violated), violated);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RuntimeObject other = (RuntimeObject) obj;
        if (!this.getClassifierName().equals(other.getClassifierName()))
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
            if (!isActive() || !other.isActive())
                return false;
            boolean equalValues = this.getNode().getProperties(true).equals(other.getNode().getProperties(true));
            return equalValues;
        }
        if (!key.equals(other.key))
            return false;
        return true;
    }

    @Override
    public String getClassifierName() {
        return runtimeClass.getModelClassifier().getQualifiedName();
    }

    public INodeKey getKey() {
        return key;
    }

    @Override
    public MetaClass<RuntimeObject> getMetaClass() {
        return runtimeClass;
    }

    public INodeStore getNodeStore() {
        return runtimeClass.getNodeStore();
    }
    
    public IBlobStore getBlobStoreStore() {
        return runtimeClass.getBlobStore();
    }

    public String getObjectId() {
        return key == null ? null : key.toString();
    }

    /**
     * Returns all instances that satisfy this parameter's constraints.
     * 
     * @param parameter
     * @param parameterType
     *            the type of instances we are interested in (could be the
     *            parameter type or a subclass)
     * @return the matching instances
     */
    public Collection<RuntimeObject> getParameterDomain(Parameter parameter, Classifier parameterType) {
        RuntimeClass parameterRuntimeClass = getRuntime().getRuntimeClass(parameterType);
        Collection<RuntimeObject> result = new LinkedHashSet<RuntimeObject>();
        List<Constraint> parameterConstraints = ConstraintUtils.getParameterConstraints(parameter);
        Collection<RuntimeObject> candidates = parameterRuntimeClass.getAllInstances();
        candidateLoop: for (BasicType candidate : candidates) {
            for (Constraint constraint : parameterConstraints) {
                Map<String, BasicType> argumentsPerParameter = Collections.singletonMap(parameter.getName(), candidate);
                if (!isConstraintSatisfied(constraint, null, argumentsPerParameter))
                    continue candidateLoop;
            }
            result.add((RuntimeObject) candidate);
        }
        return result;
    }

    public Collection<RuntimeObject> getPropertyDomain(Property property, Classifier propertyType) {
        RuntimeClass propertyRuntimeClass = getRuntime().getRuntimeClass((Classifier) propertyType);
        Collection<RuntimeObject> result = new LinkedHashSet<RuntimeObject>();
        List<Constraint> constraints = MDDExtensionUtils.findInvariantConstraints(property);
        Collection<RuntimeObject> candidates = propertyRuntimeClass.getAllInstances();
        candidateLoop: for (BasicType candidate : candidates) {
            if (!property.isMultivalued())
                for (Constraint constraint : constraints) {
                    // simulate what the object would look like with the
                    // proposed value
                    setValue(property, candidate);
                    if (!isConstraintSatisfied(constraint))
                        continue candidateLoop;
                }
            result.add((RuntimeObject) candidate);
        }
        return result;
    }

    public Collection<RuntimeObject> getRelated(Property property) {
        return getRelated(property, (Classifier) property.getType());
    }

    /**
     * Returns the objects related via the given property.
     * 
     * @param property
     * @return
     */
    public Collection<RuntimeObject> getRelated(Property property, Classifier relatedType) {
        prepareForLinking();
        ensureValidEnd(property);
        if (property.isDerived())
            return deriveRelated(property);
        if (property.isMultivalued())
            return getMultipleRelated(property, relatedType);
        return getSingleRelated(property, relatedType);
    }

    public RuntimeClass getRuntimeClass() {
        return runtimeClass;
    }
    
    public InputStream readBlob(Property property, String token) {
        return getBlobStoreStore().getContents(token);
    }
    
    public BlobType createBlob(Property property, BlobType blob) {
        if (!BasicTypeUtils.isBlobType(property.getType()))
            throw new IllegalArgumentException();
        BlobMetadata newBlob = getBlobStoreStore().addBlob(blob.primitiveValue().getOriginalName(), blob.primitiveValue().getContentType());
        BlobType result = toBlobType(newBlob);
        setValue(property, result);
        return result;
    }

    private BlobType toBlobType(BlobMetadata newBlob) {
        return new BlobType(new BlobInfo(newBlob.getToken(), newBlob.getContentType(), newBlob.getOriginalName(), newBlob.getContentLength()));
    }
    
    public void writeBlob(Property property, String token, InputStream inputStream) {
        if (!BasicTypeUtils.isBlobType(property.getType()))
            throw new IllegalArgumentException();
        BlobMetadata updatedBlob = getBlobStoreStore().setContents(token, inputStream);
        setValue(property, toBlobType(updatedBlob));
    }
    
    public void deleteBlob(Property property, String token) {
        if (!BasicTypeUtils.isBlobType(property.getType()))
            throw new IllegalArgumentException();
        getBlobStoreStore().deleteBlob(token);
        setValue(property, null);
    }

    @Override
    public BasicType getValue(Property property) {
        if (property instanceof Port)
            return readPort((Port) property);
        ensureActive();
        INode node = getNode();
        if (property.isDerived() && property.getDefaultValue() != null)
            return derivedValue(property);
        if (isAssociationEnd(property))
            return isPersistable ? traverse(property)
                    : (BasicType) node.getProperties(true).get(nodeProperty(property));
        return getSlotValue(property, node.getProperties());
    }

    public void handleEvent(RuntimeEvent runtimeEvent) {
        INode node = getNode();
        Map<String, Object> nodeProperties = node.getProperties();
        if (runtimeEvent instanceof RuntimeMessageEvent<?>
                && ((RuntimeMessageEvent<?>) runtimeEvent).getMessage() instanceof Signal) {
            RuntimeMessageEvent<Signal> runtimeMessageEvent = (RuntimeMessageEvent<Signal>) runtimeEvent;
            Signal signal = runtimeMessageEvent.getMessage();
            Reception reception = ReceptionUtils.findBySignal(this.getRuntimeClass().getModelClassifier(), signal);
            if (reception != null)
                runBehavioralFeature(reception, null, runtimeMessageEvent.getArguments());
        }
        for (Property stateProperty : StateMachineUtils.findStateProperties(getRuntimeClass().getModelClassifier())) {
            StateMachine stateMachine = (StateMachine) stateProperty.getType();
            StateMachineType currentState = getStateMachineValue(stateMachine,
                    nodeProperties.get(stateProperty.getName()));
            handleEventForState(runtimeEvent, stateProperty, currentState.getValue());
        }
    }

    @Override
    public int hashCode() {
        if (key == null)
            return isActive() ? this.getNode().getProperties(true).hashCode() : super.hashCode();
        final int prime = 31;
        int result = 1;
        result = prime * result + key.hashCode();
        return result;
    }

    public void initDefaults() {
        List<Property> allAttributes = runtimeClass.getModelClassifier().getAllAttributes();
        for (Property property : allAttributes)
            if (!property.isStatic() && !property.isDerived() && !isInitialized(property)) {
                if (property.getType() instanceof StateMachine)
                    this.setPropertyValue(property, new StateMachineType(
                            StateMachineUtils.getInitialVertex((StateMachine) property.getType())));
                if (property.getDefaultValue() != null) {
                    BasicType computedValue = RuntimeUtils.extractValueFromSpecification(this,
                            property.getDefaultValue());
                    if (computedValue != null)
                        // this is important for association ends (which may not
                        // admit being set to null)
                        this.setValue(property, computedValue);
                }
            }
    }

    public boolean isActive() {
        return key != null || !isPersisted();
    }

    public boolean isClassObject() {
        return false;
    }

    public boolean isConstraintSatisfied(Constraint constraint) {
        return isConstraintSatisfied(constraint, null, null);
    }

    public boolean isConstraintSatisfied(Constraint constraint, ParameterSet parameterSet, Map<String, BasicType> argumentsPerParameter) {
        Activity toExecute = (Activity) ActivityUtils.resolveBehaviorReference(constraint.getSpecification());

        List<BasicType> argumentValues = new ArrayList<BasicType>();
        List<Parameter> constraintParameters = FeatureUtils.getInputParameters(toExecute.getOwnedParameters());
        if (!constraintParameters.isEmpty()) {
            if (argumentsPerParameter == null)
                throw new IllegalArgumentException();
            for (Parameter inputParameter : constraintParameters) {
                if (parameterSet != null && !parameterSet.getParameters().contains(inputParameter))
                    return true;
                argumentValues.add(argumentsPerParameter.get(inputParameter.getName()));
            }
        }
        Object behaviorResult = getRuntime().runBehavior(this, constraint.getName(), toExecute, parameterSet, 
                argumentValues.toArray(new BasicType[0]));
        boolean result = behaviorResult != null && ((BooleanType) behaviorResult).isTrue();
        return result;
    }

    public boolean isEnabledOperation(Operation operation, Map<String, BasicType> argumentsPerParameter) {
        Map<Operation, List<Vertex>> stateSpecificOperations = getRuntimeClass().findStateSpecificOperations();
        if (stateSpecificOperations.containsKey(operation))
            if (!isInState(stateSpecificOperations.get(operation)))
                return false;
        for (Constraint precondition : operation.getPreconditions())
            if (!FeatureUtils.isParametrizedConstraint(precondition)
                    && !isConstraintSatisfied(precondition, null, argumentsPerParameter))
                return false;
        return true;
    }

    public boolean isPersisted() {
        return isPersisted;
    }

    public boolean isTopLevel() {
        return getNode().isTopLevel();
    }

    public boolean isTuple() {
        return runtimeClass.getModelClassifier() instanceof DataType;
    }

    /**
     * Performs a mass update to the linking for the given end fed. Unlinks this
     * object from any existing peers through the given association, and relinks
     * the fed object to the new peer(s).
     * 
     * If the new peers are already linked to another object and their end of
     * the association is singular, this will also unlink the peer from their
     * current respective peer.
     * 
     * @param end
     *            the member end representing the other side
     * @param peers
     */
    public void link(final Property end, final Collection<RuntimeObject> peers) {
        attach();
        for (RuntimeObject peer : peers)
            peer.attach();
        prepareForLinking();
        getNodeStore().linkMultipleNodes(RuntimeObject.this.getKey(), end.getName(),
                RuntimeObject.this.nodeReferences(peers), true);
    }

    /**
     * Establishes a link between two objects.
     * 
     * The UML spec does not specify what happens when the multiplicity for one
     * of the ends would be violated. This implementation will gracefully handle
     * that by undoing any existing links the peer objects may be participating
     * in before establishing the new link.
     * 
     * @param endObjects
     */
    public void link(Property end, RuntimeObject other) {
        this.attach();
        other.attach();
        prepareForLinking();
        if (end.isMultivalued()) {
            getNodeStore().linkMultipleNodes(getKey(), end.getName(), Arrays.asList(other.nodeReference()), false);
        } else {
            getNodeStore().linkNodes(getKey(), end.getName(), other.nodeReference());
        }
    }

    public void load() {
        Assert.isTrue(isPersisted);
        getNode();
        markClear();
    }

    public BasicType runBehavioralFeature(BehavioralFeature behavioralFeature, ParameterSet parameterSet, BasicType... arguments) {
        Operation asOperation = (Operation) (behavioralFeature instanceof Operation ? behavioralFeature : null);
        // try to run behavior defined for operation (if any)
        ensureActive();
        if (asOperation != null) {
            Map<String, BasicType> argumentsPerParameter = buildArgumentMap(behavioralFeature, parameterSet, arguments);
            Constraint violated = this.checkConstraints(asOperation.getPreconditions(), parameterSet, argumentsPerParameter);
            if (violated != null)
                constraintViolated(behavioralFeature, violated);

            if (!TypeUtils.isCompatible(getRuntime().getRepository(), getRuntimeClass().getModelClassifier(),
                    asOperation.getClass_(), null))
                throw new IllegalArgumentException(
                        "Operation " + behavioralFeature.getQualifiedName() + " defined by a class not belonging to "
                                + getRuntimeClass().getModelClassifier().getQualifiedName() + "'s hierarchy");
            if (!isClassObject() && !asOperation.isQuery())
                // only non-query operations generate events
                publishEvent(asOperation, arguments);
        }
        return runBehavioralFeatureBehavior(behavioralFeature, parameterSet, arguments);
    }

    @Override
    public BooleanType same(ExecutionContext context, BasicType other) {
        if (other instanceof RuntimeObject)
            return BooleanType.fromValue(this.nodeReference().equals(((RuntimeObject) other).nodeReference()));
        return super.same(context, other);
    }

    /**
     * Saves the runtime object state to the node store. An unpersisted object
     * results in a node creation. Otherwise, this updates the corresponding
     * node (if any outstanding changes exist).
     * 
     * @return
     */
    public boolean save() {
        Assert.isTrue(isPersistable);
        // persistent object are created with keys
        Assert.isTrue(nodeKey() != null);
        if (inMemoryState == null || isPersisted() && !isDirty())
            // already committed
            return false;
        if (isPersisted()) {
            getNodeStore().updateNode(inMemoryState);
        } else {
            // persist the object for the first time
            getNodeStore().createNode(inMemoryState);
            isPersisted = true;
            attach();
        }
        inMemoryState = null;
        markClear();
        return true;
    }

    public void setKey(INodeKey key) {
        Assert.isTrue(isPersistable);
        this.key = key;
    }

    /**
     * 
     * @param property
     *            this objects property
     * @param values
     */
    public void setValue(Property property, BasicType value) {
        Assert.isTrue(isActive());
        if (isAssociationEnd(property) && isPersistable) {
            Collection<RuntimeObject> newPeers;
            if (property.isMultivalued()) {
                newPeers = new LinkedHashSet<RuntimeObject>();
                for (BasicType newPeer : ((CollectionType) value).getBackEnd())
                    newPeers.add((RuntimeObject) newPeer);
            } else {
                markDirty();
                newPeers = value == null ? Collections.<RuntimeObject>emptySet()
                        : Collections.singleton((RuntimeObject) value);
            }
            this.link(property, newPeers);
        } else 
            setPropertyValue(property, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return runtimeClass.getModelClassifier().getQualifiedName() + '#' + getKey() + " = {" + inMemoryState + "}";
    }

    public void unlink(Property end, RuntimeObject other) {
        prepareForLinking();
        getNodeStore().unlinkNodes(getKey(), end.getName(), other.nodeReference());
    }

    protected Constraint checkConstraints(List<Constraint> constraints, ParameterSet parameterSet, Map<String, BasicType> arguments) {
        try {
            getNode();
        } catch (NotFoundException e) {
            // object doesn't exist
            return null;
        }
        for (Constraint constraint : constraints)
            if (!isConstraintSatisfied(constraint, parameterSet, arguments))
                return constraint;
        return null;
    }

    protected BasicType derivedValue(Property property) {
        try {
            return RuntimeUtils.extractValueFromSpecification(this, property.getDefaultValue());
        } catch (ModelExecutionException e) {
            // derivations may fail for all sorts of reasons, fall back to null
            // or empty collection if it can't obtained
            // Example: traversing the values of a new instance with a derived
            // property that is based on
            // another property that may potentially be null (even if required)
            LogUtils.logWarning(Runtime.ID, "Could not compute " + property.getName() + " due to " + e.toString(), e);
            return null;
        }
    }

    protected Collection<RuntimeObject> deriveRelated(Property property) {
        Assert.isLegal(property.isDerived());
        if (!property.isMultivalued()) {
            RuntimeObject singleReference = (RuntimeObject) derivedValue(property);
            return Collections.singleton(singleReference);
        }
        CollectionType derivedCollection = (CollectionType) derivedValue(property);
        Collection<BasicType> existing = derivedCollection.getBackEnd();
        return CollectionType.createCollectionBackEndFor(property, existing);
    }

    protected ExecutionContext getCurrentContext() {
        Runtime runtime = getRuntimeClass().getRuntime();
        ExecutionContext ctx = runtime.getCurrentContext();
        return ctx;
    }

    protected EnumerationType getEnumerationValue(Enumeration enumeration, Object value) {
        if (value == null)
            return null;
        EnumerationLiteral literal = enumeration.getOwnedLiteral(value.toString());
        if (literal == null)
            // could mask an invalid state due to refactor/rename of literals
            // (and what if the property does not admit nulls?)
            return null;
        return new EnumerationType(literal);
    }

    protected Collection<RuntimeObject> getSingleRelated(Property property, Classifier relatedType) {
        Assert.isLegal(!property.isMultivalued());
        Assert.isLegal(!property.isDerived());
        return getMultipleRelated(property, relatedType);
    }

    protected EAnnotation getSourceInfo(NamedElement scope) {
        EAnnotation sourceInfo;
        NamedElement sourceInfoScope = scope;
        while ((sourceInfo = sourceInfoScope.getEAnnotation(MDDUtil.UNIT)) == null)
            sourceInfoScope = sourceInfoScope.getNamespace();
        return sourceInfo;
    }

    protected StateMachineType getStateMachineValue(StateMachine stateMachine, Object value) {
        Vertex vertex = null;
        if (value != null)
            vertex = StateMachineUtils.getState(stateMachine, value.toString());
        if (vertex == null)
            vertex = StateMachineUtils.getInitialVertex(stateMachine);
        return vertex == null ? null : new StateMachineType(vertex);
    }

    protected void markClear() {
        isDirty = false;
    }

    protected void markDirty() {
        if (isPersistable) {
            isDirty = true;
            getCurrentContext().markDirty();
        }
    }

    protected void publishEvent(Operation operation, BasicType... arguments) {
        getCurrentContext().publishEvent(RuntimeMessageEvent.build(operation, this, arguments));
    }

    protected NodeReference runtimeObjectToNodeReference() {
        return new NodeReference(this.storeName(), this.nodeKey());
    }

    /**
     * A generic traversal operation that will return a single RuntimeObject or
     * a collection of runtime objects depending on the multiplicity of the
     * property.
     */
    protected BasicType traverse(Property property) {
        Classifier relatedType = (Classifier) property.getType();
        if (property.isMultivalued()) {
            Assert.isTrue(property.isMultivalued());
            return CollectionType.createCollectionFor(property, this.getRelated(property, relatedType));
        }
        Collection<RuntimeObject> related = getRuntime().collectInstancesFromHierarchy(relatedType, true,
                ((Classifier specificRelatedType) -> getRelated(property, specificRelatedType)));
        return related.isEmpty() ? null : related.iterator().next();
    }

    INode getNode() {
        ensureActive();
        if (inMemoryState == null) {
            Assert.isTrue(isPersisted());
            inMemoryState = getNodeStore().getNode(key);
            if (inMemoryState == null)
                throw new NotFoundException(key.toString());
            attach();
        }
        return inMemoryState;
    }

    public INodeKey nodeKey() {
        return this.key;
    }

    public NodeReference nodeReference() {
        return runtimeObjectToNodeReference();
    }

    BasicType runBehavior(ExecutionContext context, Behavior behavior, ParameterSet parameterSet, BasicType... arguments) {
        return context.getRuntime().runBehavior(this, "", (Activity) behavior, parameterSet, arguments);
    }

    BasicType runBehavioralFeatureBehavior(BehavioralFeature operation, ParameterSet parameterSet, BasicType... arguments) {
        final Runtime runtime = getRuntime();
        if (operation instanceof Operation && ((Operation) operation).getInterface() != null)
            operation = FeatureUtils.findCompatibleOperation(runtime.getRepository(),
                    this.getRuntimeClass().getModelClassifier(), (Operation) operation);
        if (operation.getMethods().isEmpty())
            // a behavior-less operation or reception does nothing
            return null;
        String frameName = operation.getQualifiedName();
        Activity behavior = (Activity) operation.getMethods().get(0);
        BasicType result = runtime.runBehavior(this, frameName, behavior, parameterSet, arguments);
        return result;
    }

    private void commitAll() {
        getCurrentContext().commitWorkingSet();
    }

    private void constraintViolated(NamedElement scope, Constraint violated) {
        // EAnnotation lineInfo = violated.getEAnnotation(MDDUtil.LINE_NUMBER);
        // String lineNumber = lineInfo.getDetails().get("lineNumber");
        // EAnnotation sourceInfo = getSourceInfo(scope);

        NamedElement anchorNameScope = scope;
        while (anchorNameScope.getQualifiedName() == null)
            anchorNameScope = anchorNameScope.getNamespace();

        // String sourceName = sourceInfo != null ?
        // sourceInfo.getDetails().get("name") : "<source name unavailable>";
        String preconditionName = violated.getName() == null ? "" : " - " + violated.getName();
        Classifier violationClass = MDDExtensionUtils.getRuleViolationClass(violated);
        if (violationClass == null)
            violationClass = ClassifierUtils.findClassifier(Runtime.getCurrentRuntime().getRepository(),
                    "mdd_types::Violation");
        BasicType exceptionObject = Runtime.getCurrentRuntime().newInstance(violationClass, false);
        String message = MDDUtil.getDescription(violated);
        if (StringUtils.isBlank(message))
            message = "Constraint violated " + anchorNameScope.getQualifiedName() + preconditionName + " - object: "
                    + this;
        RuntimeRaisedException runtimeRaisedException = new RuntimeRaisedException(exceptionObject, message,
                anchorNameScope);
        runtimeRaisedException.setConstraint(violated);
        throw runtimeRaisedException;
    }

    private void ensureValidEnd(Property property) {
        Assert.isLegal(isAssociationEnd(property));
        Assert.isLegal(getRuntimeClass().getModelClassifier().isCompatibleWith(property.getOtherEnd().getType()),
                "Expected: " + getRuntimeClass().getModelClassifier().getQualifiedName() + " - Actual: "
                        + property.getOtherEnd().getType().getQualifiedName());
    }

    private Collection<RuntimeObject> getMultipleRelated(Property property, Classifier relatedInstanceType) {
        Assert.isLegal(!property.isDerived());
        RuntimeClass relatedEntity = getRuntime().getRuntimeClass(relatedInstanceType);
        String relatedStoreNodeName = relatedEntity.getNodeStore().getName();
        Collection<NodeReference> relatedNodeRefs = getNodeStore().getRelatedNodeReferences(getKey(), property.getName(),
                relatedStoreNodeName);
//        List<INodeKey> nodeKeys = relatedNodeRefs.stream().map(it -> it.getKey()).collect(Collectors.toList());
		return getCurrentContext().getRuntime().getInstances(relatedNodeRefs);
    }

    private Runtime getRuntime() {
        return Runtime.get();
    }

    private BasicType getSlotValue(Property attribute, Map<String, Object> properties) {
        attribute = mapToActualAttribute(attribute);
        String nodeProperty = nodeProperty(attribute);
        Object value = properties.get(nodeProperty);
        if (attribute.getType() instanceof StateMachine)
            return getStateMachineValue((StateMachine) attribute.getType(), value);
        if (attribute.getType() instanceof Enumeration)
            return getEnumerationValue((Enumeration) attribute.getType(), value);
        if (value == null) {
            boolean valueFound = properties.containsKey(nodeProperty);
            return valueFound || attribute.getDefaultValue() == null ? 
                    null :
                    RuntimeUtils.extractValueFromSpecification(this, attribute.getDefaultValue());
        }
        if (value instanceof BasicType)
            // seen this when dealing with anonymously typed objects (object
            // literals) with a slot that is a full Class instance
            return (BasicType) value;
        return PrimitiveType.fromValue(attribute.getType(), value);
    }

    private Property mapToActualAttribute(Property attribute) {
        Classifier thisClassifier = this.runtimeClass.getModelClassifier();
        if (DataTypeUtils.isAnonymousDataType(thisClassifier)) {
            // for anonymous data types, property access is positional
            int position = FeatureUtils.getOwningClassifier(attribute).getAllAttributes().indexOf(attribute);
            return thisClassifier.getAllAttributes().get(position);
        }
        return attribute;
    }

    private void handleEventForState(RuntimeEvent runtimeEvent, Property stateProperty, Vertex currentState) {
        for (Transition transition : currentState.getOutgoings()) {
            if (transition.getGuard() != null) {
                boolean enabled = isConstraintSatisfied(transition.getGuard());
                if (!enabled)
                    continue;
            }
            for (Trigger trigger : transition.getTriggers())
                if (runtimeEvent.isMatchedBy(trigger)) {
                    /*
                     * The semantics of entering a State depend on the type of
                     * State and the manner in which it is entered. However, in
                     * all cases, the entry Behavior of the State is executed
                     * (if defined) upon entry, but only after any effect
                     * Behavior associated with the incoming Transition is
                     * completed. Also, if a doActivity Behavior is defined for
                     * the State, this Behavior commences execution immediately
                     * after the entry Behavior is executed. It executes
                     * concurrently with any subsequent Behaviors associated
                     * with entering the State, such as the entry Behaviors of
                     * substates entered as part of the same compound
                     * transition. [...] Regardless of how a State is entered,
                     * the StateMachine is deemed to be “in” that State even
                     * before any effect Behavior (if defined) of that State
                     * start executing.
                     */
                    if (currentState instanceof State && ((State) currentState).getExit() != null)
                        runBehavior(getCurrentContext(), ((State) currentState).getExit(), null);
                    if (transition.getEffect() != null)
                        runBehavior(getCurrentContext(), transition.getEffect(), null);
                    Vertex newState = transition.getTarget();
                    setPropertyValue(stateProperty, new StateMachineType(newState));
                    if (newState instanceof State && ((State) newState).getEntry() != null)
                        runBehavior(getCurrentContext(), ((State) newState).getEntry(), null);
                    if (newState instanceof State && ((State) newState).getDoActivity() != null)
                        runBehavior(getCurrentContext(), ((State) newState).getDoActivity(), null);
                    return;
                }
        }
    }

    private boolean isAssociationEnd(Property property) {
        Association association = property.getAssociation();
        return association != null && !(property.getType() instanceof Enumeration);
    }

    private boolean isDirty() {
        return isDirty;
    }

    private boolean isInitialized(Property property) {
        String nodeProperty = nodeProperty(property);
        return getNode().isPropertySet(nodeProperty) || getNode().isRelatedSet(nodeProperty);
    }

    private boolean isInState(List<Vertex> states) {
        ensureActive();
        INode node = getNode();
        List<Property> stateProperties = StateMachineUtils.findStateProperties(getRuntimeClass().getModelClassifier());
        for (Vertex vertex : states) {
            StateMachine stateMachine = vertex.getContainer().getStateMachine();
            for (Property property : stateProperties)
                if (property.getType() == stateMachine) {
                    Object value = node.getProperties().get(nodeProperty(property));
                    if (getStateMachineValue(stateMachine, value).getValue() == vertex)
                        return true;
                }
        }
        return false;
    }

    private String nodeProperty(Property feature) {
        return feature.getName();
    }

    private Collection<NodeReference> nodeReferences(Collection<RuntimeObject> peers) {
        Collection<NodeReference> result = new ArrayList<NodeReference>(peers.size());
        for (RuntimeObject peer : peers)
            result.add(peer.nodeReference());
        return result;
    }

    private void prepareForLinking() {
        commitAll();
    }

    protected BasicType readPort(Port port) {
        return runtimeClass.getRuntime().getProviderInstance(port);
    }

    private void setPropertyValue(Property property, BasicType value) {
        INode node = getNode();
        markDirty();
        Map<String, Object> properties = node.getProperties();
        properties.put(nodeProperty(property), value == null ? null : toExternalValue(value));
        node.setProperties(properties);
    }

    private String storeName() {
        return this.getRuntimeClass().getModelClassifier().getQualifiedName().replaceAll(NamedElement.SEPARATOR, ".");
    }

    static Object toExternalValue(BasicType value) {
        if (value instanceof EnumerationType)
            return ((EnumerationType) value).getValue().getName();
        if (value instanceof StateMachineType)
            return ((StateMachineType) value).toString();
        if (value instanceof PrimitiveType)
            return ((PrimitiveType<?>) value).primitiveValue();
        return value;
    }

    // public Map<String> getActionCapabilities() {
    // return Collections.
    // }
}
