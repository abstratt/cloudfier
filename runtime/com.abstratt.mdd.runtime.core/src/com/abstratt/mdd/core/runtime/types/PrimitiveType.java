package com.abstratt.mdd.core.runtime.types;

import java.io.Serializable;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public abstract class PrimitiveType<T> extends BuiltInClass implements ComparableType, Serializable {
    public static BasicType convertToBasicType(Classifier converterType, Object original) {
        String packageName = ValueConverter.class.getPackage().getName();
        String converterName = packageName + '.' + converterType.getName() + "Converter";
        ValueConverter valueConverter;
        try {
            valueConverter = (ValueConverter) java.lang.Class.forName(converterName).newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Error finding converter for " + converterType.getQualifiedName() + " to convert " + original, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error finding converter for " + converterType.getQualifiedName() + " to convert " + original, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error finding converter for " + converterType.getQualifiedName() + " to convert " + original, e);
        }
        return valueConverter.convertToBasicType(original);
    }
    
    public static <T extends BasicType> T fromStringValue(Type basicType, String stringValue) {
        return (T) PrimitiveType.convertToBasicType((Classifier) basicType, stringValue);
    }
    
    public static <T extends PrimitiveType> T fromValue(Type basicType, Object value) {
        return (T) PrimitiveType.convertToBasicType((Classifier) basicType, value);
    }

    public static boolean hasConverter(Classifier converterType) {
        String packageName = ValueConverter.class.getPackage().getName();
        String converterName = packageName + '.' + converterType.getName() + "Converter";
        try {
            java.lang.Class.forName(converterName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static final long serialVersionUID = 1L;

    /**
     * @see BasicType#isEqualsTo
     */
    @Override
    public final boolean equals(Object another) {
        if (!(another instanceof PrimitiveType))
            return false;
        boolean result = another != null && primitiveValue().equals(((PrimitiveType) another).primitiveValue());
		return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BooleanType greaterThan(ExecutionContext context, ComparableType other) {
        return BooleanType.fromValue(other != null && ((Comparable) this.primitiveValue()).compareTo(((PrimitiveType<?>) other).primitiveValue()) > 0);
    }
    
    @Override
    public BooleanType greaterOrEquals(ExecutionContext context, ComparableType other) {
    	return greaterThan(context, other).or(context, equals(context, other));
    }
    
    @Override
    public BooleanType lowerOrEquals(ExecutionContext context, ComparableType other) {
    	return lowerThan(context, other).or(context, equals(context, other));
    }

    @Override
    public final int hashCode() {
        return primitiveValue().hashCode();
    }

    @Override
	@SuppressWarnings("unchecked")
    public BooleanType lowerThan(ExecutionContext context, ComparableType other) {
        return BooleanType.fromValue(other != null && ((Comparable) this.primitiveValue()).compareTo(((PrimitiveType<?>) other).primitiveValue()) < 0);
    }
    
    public abstract T primitiveValue();

    @Override
    public java.lang.String toString() {
        return primitiveValue().toString();
    }
}
