package com.abstratt.mdd.core.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.InstanceSpecification;
import org.eclipse.uml2.uml.InstanceValue;
import org.eclipse.uml2.uml.LiteralNull;
import org.eclipse.uml2.uml.LiteralSpecification;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.ValueSpecification;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.CollectionType;
import com.abstratt.mdd.core.runtime.types.ElementReferenceType;
import com.abstratt.mdd.core.runtime.types.EnumerationType;
import com.abstratt.mdd.core.runtime.types.PrimitiveType;
import com.abstratt.mdd.core.runtime.types.StateMachineType;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.ClassifierUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.StereotypeUtils;

public class RuntimeUtils {
	
    /**
     * Applies the given collector in the context of the given base class, and optionally in the context of all subclasses.
     * @param baseClass
     * @param includeSubclasses
     * @param collector
     * @return the collected objects
     */
    public static List<RuntimeObject> collectInstancesFromHierarchy(IRepository repository, Classifier baseClass, boolean includeSubclasses, Function<Classifier, Collection<RuntimeObject>> collector) {
    	BiConsumer<Classifier, List<RuntimeObject>> consumer = (classifier, collected) -> collected.addAll(collector.apply(classifier));
    	return ClassifierUtils.collectFromHierarchy(repository, baseClass, includeSubclasses, new ArrayList<RuntimeObject>(), consumer);
    }

    public static BasicType extractValueFromSpecification(RuntimeObject self, ValueSpecification valueSpec) {
        if (MDDExtensionUtils.isBasicValue(valueSpec))
            return PrimitiveType.fromValue(valueSpec.getType(), MDDExtensionUtils.getBasicValue(valueSpec));
        if (ActivityUtils.isBehaviorReference(valueSpec)) {
            Activity behavior = (Activity) ActivityUtils.resolveBehaviorReference(valueSpec);
            if (ActivityUtils.getClosureInputParameters(behavior).size() == 0) {
                // no parameters, evaluate on the spot
                Runtime runtime = Runtime.get();
                return runtime.runBehavior(self, behavior.getName(), behavior);
            }
            return new ElementReferenceType(behavior);
        }
        if (MDDExtensionUtils.isVertexLiteral(valueSpec))
            return new StateMachineType(MDDExtensionUtils.resolveVertexLiteral(valueSpec));
        if (valueSpec instanceof LiteralNull)
            return null;
        if (valueSpec instanceof LiteralSpecification)
            return PrimitiveType.fromStringValue(valueSpec.getType(), valueSpec.stringValue());
        if (valueSpec instanceof InstanceValue) {
            InstanceSpecification instanceSpec = ((InstanceValue) valueSpec).getInstance();
            if (instanceSpec instanceof EnumerationLiteral)
                return new EnumerationType((EnumerationLiteral) instanceSpec);
        }
        throw new IllegalArgumentException("Unsupported value spec: " + valueSpec.eClass().getInstanceClassName());
    }

    public static BasicType extractValueFromSpecification(ValueSpecification valueSpec) {
        return RuntimeUtils.extractValueFromSpecification(null, valueSpec);
    }

    public static BasicType getDefaultValue(Classifier type) {
        if (PrimitiveType.hasConverter(type))
            return PrimitiveType.fromStringValue(type, "");
        if (type instanceof Enumeration && !((Enumeration) type).getOwnedLiterals().isEmpty())
            // questionable decision... but we can't have null enumeration
            // values
            return new EnumerationType(((Enumeration) type).getOwnedLiterals().get(0));
        return null;
    }

    public static String toString(BasicType value) {
        return RuntimeUtils.toString(value, 1);
    }

    public static String toString(BasicType value, int depth) {
        if (value == null)
            return "";
        if (value instanceof CollectionType) {
            StringBuffer buf = new StringBuffer();
            buf.append("[");
            for (BasicType element : ((CollectionType) value).getBackEnd()) {
                buf.append(RuntimeUtils.toString(element, depth - 1));
                buf.append(",");
            }
            if (buf.charAt(buf.length() - 1) == ',')
                buf.deleteCharAt(buf.length() - 1);
            buf.append("]");
            return buf.toString();
        }
        if (!(value instanceof RuntimeObject))
            return value.toString();
        RuntimeObject instance = (RuntimeObject) value;
        Classifier classifier = instance.getRuntimeClass().getModelClassifier();

        if (StereotypeUtils.hasStereotype(classifier, "formatting::Format")) {
            Pattern pattern = Pattern.compile("\\[\\w[\\w|\\d]*\\]");
            Stereotype stereotype = classifier.getAppliedStereotype("formatting::Format");
            String mask = (String) classifier.getValue(stereotype, "mask");
            Matcher matcher = pattern.matcher(mask);
            String result = mask;
            while (matcher.find()) {
                String group = matcher.group();
                String propertyName = group.substring(1, group.length() - 1);
                Property property = classifier.getAttribute(propertyName, null);
                result = result.replaceAll("\\[" + propertyName + "\\]", RuntimeUtils.toString(instance.getValue(property)));
            }
            return result;
        }

        List<Property> allAttributes = classifier.getAllAttributes();
        for (Property current : allAttributes)
            if (StereotypeUtils.hasStereotype(current, "identification::MainAttribute")) {
                BasicType slotValue = instance.getValue(current);
                if (slotValue == null)
                    continue;
                return RuntimeUtils.toString(slotValue, depth - 1);
            }
        if (depth < 0)
            return "...";
        StringBuffer instanceStringValue = new StringBuffer();

        instanceStringValue.append(":" + classifier.getName());
        instanceStringValue.append(" [");
        for (Property attribute : allAttributes) {
            BasicType slotValue = instance.getValue(attribute);
            if (slotValue == null)
                continue;
            instanceStringValue.append(attribute.getName());
            instanceStringValue.append("=");
            instanceStringValue.append(RuntimeUtils.toString(slotValue, depth - 1));
            instanceStringValue.append(",");
        }
        if (instanceStringValue.length() > 0 && instanceStringValue.charAt(instanceStringValue.length() - 1) == ',')
            instanceStringValue.deleteCharAt(instanceStringValue.length() - 1);
        instanceStringValue.append("]");
        return instanceStringValue.toString();
    }

}
