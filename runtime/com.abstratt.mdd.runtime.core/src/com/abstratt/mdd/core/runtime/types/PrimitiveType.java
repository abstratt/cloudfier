package com.abstratt.mdd.core.runtime.types;

import java.io.Serializable;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.LiteralReal;
import org.eclipse.uml2.uml.LiteralSpecification;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public abstract class PrimitiveType<T> extends BuiltInClass implements ComparableType, Serializable {
    public static BasicType convertToBasicType(Type converterType, Object original) {
        String packageName = ValueConverter.class.getPackage().getName();
        String converterName = packageName + '.' + converterType.getName() + "Converter";
        ValueConverter valueConverter;
        try {
            valueConverter = (ValueConverter) java.lang.Class.forName(converterName).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException("Error finding converter for " + converterType.getQualifiedName() + " to convert " + original, e);
        }
        return valueConverter.convertToBasicType(original);
    }
    
    public static <T extends BasicType> T fromStringValue(Type basicType, String stringValue) {
        return (T) convertToBasicType((Classifier) basicType, stringValue);
    }
    
    public static <T extends PrimitiveType> T fromValue(Type basicType, Object value) {
        return (T) convertToBasicType((Classifier) basicType, value);
    }
    
    public static <T extends PrimitiveType> T fromValue(Type basicType, LiteralSpecification literalSpecification) {
    	Object value;
    	if (literalSpecification instanceof LiteralReal)
    		value = literalSpecification.realValue();
    	else if (literalSpecification instanceof LiteralInteger)
    		value = literalSpecification.integerValue();
    	else if (literalSpecification instanceof LiteralString)
    		value = literalSpecification.stringValue();
    	else if (literalSpecification instanceof LiteralBoolean)
    		value = literalSpecification.booleanValue();
    	else 
    		value = null;
        return (T) convertToBasicType((Classifier) basicType, value);
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
    
    protected T value;
    
    protected PrimitiveType(T value) {
    	this.value = value;
    }
    
    public T primitiveValue() {
        return value;
    }


    /**
     * @see BasicType#isEqualsTo
     */
    @Override
    public final boolean equals(Object another) {
        boolean result;
		if (!(another instanceof PrimitiveType))
            result = false;
        else
        	result = another != null && primitiveValue().equals(((PrimitiveType) another).primitiveValue());
		return result;
    }

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
    public BooleanType lowerThan(ExecutionContext context, ComparableType other) {
        return BooleanType.fromValue(other != null && ((Comparable) this.primitiveValue()).compareTo(((PrimitiveType<?>) other).primitiveValue()) < 0);
    }
    
    @Override
    public final int hashCode() {
        return primitiveValue().hashCode();
    }

    @Override
    public java.lang.String toString() {
        return primitiveValue().toString();
    }
}
