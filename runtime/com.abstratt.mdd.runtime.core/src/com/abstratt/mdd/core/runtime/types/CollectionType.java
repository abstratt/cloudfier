package com.abstratt.mdd.core.runtime.types;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.MultiplicityElement;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.TypedElement;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.ClassifierUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;

public abstract class CollectionType extends BuiltInClass implements Serializable {
    public static CollectionType createCollection(Type baseType, boolean unique, boolean ordered) {
        return CollectionType.createCollection(baseType, unique, ordered, new HashSet<BasicType>());
    }

    public static <E extends BasicType, T extends Collection<E>> CollectionType createCollection(Type baseType, boolean unique,
            boolean ordered, T backEnd) {
        assert baseType != null;
        return ordered ? unique ? new OrderedSetType(baseType, backEnd) : new SequenceType(baseType, backEnd) : unique ? new SetType(
                baseType, backEnd) : new BagType(baseType, backEnd);
    }

    public static CollectionType createCollectionFor(MultiplicityElement element) {
        Assert.isTrue(element.isMultivalued(), element.toString());
        Assert.isTrue(element instanceof TypedElement);
        return CollectionType.createCollection(((TypedElement) element).getType(), element.isUnique(), element.isOrdered());
    }

    public static <E extends BasicType, T extends Collection<E>> CollectionType createCollectionFor(MultiplicityElement element, T backEnd) {
        Assert.isTrue(element.isMultivalued());
        Assert.isTrue(element instanceof TypedElement);
        return CollectionType.createCollection(((TypedElement) element).getType(), element.isUnique(), element.isOrdered(), backEnd);
    }

    protected static Object runClosureBehavior(ExecutionContext context, ElementReferenceType reference, Object... arguments) {
        Activity closure = (Activity) reference.getElement();
        Assert.isTrue(MDDExtensionUtils.isClosure(closure));
        return context.getRuntime()
                .runBehavior(context.currentFrame().getSelf(), "[closure]", closure, arguments);
    }

    private static final long serialVersionUID = 1L;

    protected final Collection<BasicType> backEnd;

    private final Type baseType;

    protected CollectionType(Type baseType, Collection<BasicType> backEnd) {
        assert backEnd != null;
        this.backEnd = backEnd;
        this.baseType = baseType;
    }

    public void add(BasicType value) {
        backEnd.add(value);
    }

    public CollectionType add(ExecutionContext context, BasicType toAdd) {
        CollectionType result = CollectionType.createCollection(baseType, isUnique(), isOrdered());
        result.backEnd.addAll(this.backEnd);
        result.backEnd.add(toAdd);
        return result;
    }

    public BasicType any(ExecutionContext context, ElementReferenceType reference) {
        return internalAny(context, reference, true);
    }
    
    public BasicType one(ExecutionContext context) {
        return backEnd.isEmpty() ? null : backEnd.iterator().next(); 
    }

    public BagType asBag(ExecutionContext context) {
        return new BagType(baseType, backEnd);
    }

    public OrderedSetType asOrderedSet(ExecutionContext context) {
        return new OrderedSetType(baseType, backEnd);
    }

    public SequenceType asSequence(ExecutionContext context) {
        return new SequenceType(baseType, backEnd);
    }

    public SetType asSet(ExecutionContext context) {
        return new SetType(baseType, backEnd);
    }
    
    public BasicType max(ExecutionContext context, ElementReferenceType reference) {
        BasicType max = null;
        NumberType<?> maxValue = null;
        for (BasicType current : backEnd) {
            NumberType<?> currentValue = (NumberType<?>) CollectionType.runClosureBehavior(context, reference, current);
            if (max == null || maxValue.lowerThan(context, currentValue).isTrue()) {
                max = current;
                maxValue = currentValue;
            }
        }
        return max;
    }
    
    public BasicType min(ExecutionContext context, ElementReferenceType reference) {
        BasicType min = null;
        NumberType<?> minValue = null;
        for (BasicType current : backEnd) {
            NumberType<?> currentValue = (NumberType<?>) CollectionType.runClosureBehavior(context, reference, current);
            if (min == null || minValue.greaterThan(context, currentValue).isTrue()) {
                min = current;
                minValue = currentValue;
            }
        }
        return min;
    }

    public CollectionType collect(ExecutionContext context, ElementReferenceType reference) {
        Parameter closureReturnParameter = ActivityUtils.getClosureReturnParameter((Activity) reference.getElement());
        CollectionType result = CollectionType.createCollection(closureReturnParameter.getType(), isUnique(), isOrdered());
        for (BasicType current : backEnd) {
            BasicType mapped = (BasicType) CollectionType.runClosureBehavior(context, reference, current);
            result.add(mapped);
        }
        return result;
    }

    public CollectionType collectMany(ExecutionContext context, ElementReferenceType reference) {
        Parameter closureReturnParameter = ActivityUtils.getClosureReturnParameter((Activity) reference.getElement());
        CollectionType result = CollectionType.createCollection(closureReturnParameter.getType(), isUnique(), isOrdered());
        for (BasicType current : backEnd) {
            CollectionType mapped = (CollectionType) CollectionType.runClosureBehavior(context, reference, current);
            result.getBackEnd().addAll(mapped.getBackEnd());
        }
        return result;
    }

    public boolean contains(BasicType value) {
        return backEnd.contains(value);
    }

    public BasicType exists(ExecutionContext context, ElementReferenceType reference) {
        return BooleanType.fromValue(internalAny(context, reference, true) != null);
    }

    public BooleanType forAll(ExecutionContext context, ElementReferenceType reference) {
        return BooleanType.fromValue(internalAny(context, reference, false) == null);
    }

    public void forEach(ExecutionContext context, ElementReferenceType reference) {
        for (BasicType current : backEnd)
            CollectionType.runClosureBehavior(context, reference, current);
    }

    public Collection<BasicType> getBackEnd() {
        return backEnd;
    }

    public Type getBaseType() {
        return baseType;
    }

    public GroupingType groupBy(ExecutionContext context, ElementReferenceType reference) {
        Map<BasicType, CollectionType> groups = new HashMap<BasicType, CollectionType>();
        Parameter closureReturnParameter = ActivityUtils.getClosureReturnParameter((Activity) reference.getElement());
        for (BasicType current : backEnd) {
            BasicType groupKey = (BasicType) CollectionType.runClosureBehavior(context, reference, current);
            CollectionType group = groups.get(groupKey);
            if (group == null)
                groups.put(groupKey, group = CollectionType.createCollection(closureReturnParameter.getType(), isUnique(), isOrdered()));
            group.add(current);
        }
        return new GroupingType(baseType, groups);
    }

    public BooleanType includes(ExecutionContext context, BasicType toTest) {
        return BooleanType.fromValue(contains(toTest));
    }

    @Override
    public boolean isCollection() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return this.backEnd.isEmpty();
    }

    public BooleanType isEmpty(ExecutionContext context) {
        return BooleanType.fromValue(backEnd.isEmpty());
    }

    public BooleanType isEqualTo(BasicType another) {
        if (getClass() != another.getClass())
            return BooleanType.FALSE;
        return BooleanType.fromValue(backEnd.equals(((CollectionType) another).backEnd));
    }

    public abstract boolean isOrdered();

    public abstract boolean isUnique();

    public BasicType reduce(ExecutionContext context, ElementReferenceType reference, BasicType initial) {
        BasicType partial = initial;
        for (BasicType current : backEnd)
            partial = (BasicType) CollectionType.runClosureBehavior(context, reference, current, partial);
        return partial;
    }
    
    public void remove(BasicType value) {
        backEnd.remove(value);
    }

    public CollectionType select(ExecutionContext context, ElementReferenceType reference) {
        CollectionType result = CollectionType.createCollection(baseType, isUnique(), isOrdered());
        for (BasicType current : backEnd) {
            BooleanType predicateOutcome = (BooleanType) CollectionType.runClosureBehavior(context, reference, current);
            if (predicateOutcome.isTrue())
                result.add(current);
        }
        return result;
    }

    public IntegerType size(ExecutionContext context) {
        return IntegerType.fromValue(backEnd.size());
    }

    public NumberType<?> sum(ExecutionContext context, ElementReferenceType reference) {
        NumberType<?> sum = null;
        for (BasicType current : backEnd) {
            NumberType<?> mapped = (NumberType<?>) CollectionType.runClosureBehavior(context, reference, current);
            sum = sum == null ? mapped : sum.add(context, mapped);
        }
        return sum;
    }
    
    @Override
    public String toString() {
        return getBackEnd().toString();
    }

    public CollectionType union(ExecutionContext context, CollectionType another) {
        CollectionType result = CollectionType.createCollection(getBaseType(), isUnique(), isOrdered(), this.getBackEnd());
        result.getBackEnd().addAll(another.backEnd);
        return result;
    }

    private BasicType internalAny(ExecutionContext context, ElementReferenceType reference, boolean expected) {
        for (BasicType current : backEnd) {
            BooleanType predicateOutcome = (BooleanType) CollectionType.runClosureBehavior(context, reference, current);
            if (expected == predicateOutcome.primitiveValue())
                return current;
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CollectionType))
            return false;
        CollectionType other = (CollectionType) obj;
        if (this.backEnd.isEmpty() && other.backEnd.isEmpty())
            return true;
        if (this.baseType != other.baseType)
            return false;
        return this.backEnd.equals(other.backEnd);
    }
}
