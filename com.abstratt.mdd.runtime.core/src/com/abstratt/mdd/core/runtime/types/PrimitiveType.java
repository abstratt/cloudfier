package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import java.io.Serializable;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Type;

public abstract class PrimitiveType<T> extends BuiltInClass implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
	 * @see BasicType#isEqualsTo
	 */
	@Override
	public final boolean equals(Object another) {
		if (!(another instanceof PrimitiveType))
			return false;
		return another != null && primitiveValue().equals(((PrimitiveType) another).primitiveValue());
	}
	
	@Override
	public final int hashCode() {
	    return primitiveValue().hashCode();
	}

	@SuppressWarnings("unchecked")
	public BooleanType greaterOrEquals(
	ExecutionContext context, PrimitiveType<?> other) {
		return BooleanType.fromValue(greaterThan(context, other).primitiveValue() || equals(context, other).primitiveValue());
	}

	@SuppressWarnings("unchecked")
	public BooleanType greaterThan(ExecutionContext context, PrimitiveType<?> other) {
		return BooleanType.fromValue(other != null && ((Comparable) this.primitiveValue()).compareTo(other.primitiveValue()) > 0);
	}

	@SuppressWarnings("unchecked")
	public BooleanType lowerOrEquals(
	ExecutionContext context, PrimitiveType<?> other) {
		return lowerThan(context, other).or(context, equals(context, other));
	}
	
	@SuppressWarnings("unchecked")
	public BooleanType lowerThan(
	ExecutionContext context, PrimitiveType<?> other) {
		return BooleanType.fromValue(other != null && ((Comparable) this.primitiveValue()).compareTo(other.primitiveValue()) < 0);
	}
	
	public abstract T primitiveValue();
	
	public java.lang.String toString() {
		return primitiveValue().toString();
	}

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

	public static <T extends PrimitiveType> T fromValue(Type basicType, Object value) {
		return (T) convertToBasicType((Classifier) basicType, value) ;
	}

	public static <T extends BasicType> T fromStringValue(Type basicType, String stringValue) {
		return (T) convertToBasicType((Classifier) basicType, stringValue) ;
	}
}
