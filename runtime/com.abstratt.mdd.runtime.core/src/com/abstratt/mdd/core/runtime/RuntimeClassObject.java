package com.abstratt.mdd.core.runtime;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.Property;

import com.abstratt.mdd.core.runtime.types.BasicType;

/**
 * Stores values for class attributes. No relationships.
 */
public class RuntimeClassObject extends RuntimeObject {
    private Map<Property, BasicType> slots = new HashMap<Property, BasicType>();

    public RuntimeClassObject(RuntimeClass runtimeClass) {
        super(runtimeClass);
        // initializes readonly static properties
        for (Property current : runtimeClass.getModelClassifier().getAllAttributes())
            if (current.isStatic() && current.getDefaultValue() != null && !current.isDerived())
                this.setValue(current, RuntimeUtils.extractValueFromSpecification(current.getDefaultValue()));
    }

    public Map<Property, BasicType> getSlots() {
        return new HashMap<Property, BasicType>(slots);
    }
    
    @Override
    public BasicType getValue(Property attribute) {
        if (attribute instanceof Port)
            return readPort((Port) attribute);
        if (attribute.isDerived() && attribute.getDefaultValue() != null)
            return derivedValue(attribute);
        return slots.get(attribute);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public boolean isClassObject() {
        return true;
    }

    @Override
    public void setValue(Property feature, BasicType value) {
        slots.put(feature, value);
    }
}
