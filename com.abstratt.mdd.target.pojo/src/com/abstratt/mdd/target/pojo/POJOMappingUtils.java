package com.abstratt.mdd.target.pojo;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.query.conditions.eobjects.EObjectTypeRelationCondition;
import org.eclipse.emf.query.conditions.eobjects.TypeRelation;
import org.eclipse.emf.query.statements.FROM;
import org.eclipse.emf.query.statements.IQueryResult;
import org.eclipse.emf.query.statements.SELECT;
import org.eclipse.emf.query.statements.WHERE;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.InstanceSpecification;
import org.eclipse.uml2.uml.InstanceValue;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.MultiplicityElement;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.TypedElement;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.ValueSpecification;

import com.abstratt.mdd.core.util.BasicTypeUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.StereotypeUtils;

public class POJOMappingUtils {
	public static String mapValue(ValueSpecification valueSpec) {
        if (MDDExtensionUtils.isBasicValue(valueSpec)) {
        	Object basicValue = MDDExtensionUtils.getBasicValue(valueSpec);
			return basicValue instanceof String ? ('"' + basicValue.toString() + '"') : (basicValue + "");
        }
		if (valueSpec instanceof InstanceValue) {
			InstanceSpecification instanceSpec = ((InstanceValue) valueSpec).getInstance();
			if (instanceSpec instanceof EnumerationLiteral) {
				EnumerationLiteral enumLiteral = (EnumerationLiteral) instanceSpec;
				return enumLiteral.getEnumeration().getName() + '.' + enumLiteral.getName();
			}
		}			
		final String stringValue = valueSpec.stringValue();
		return valueSpec instanceof LiteralString ? ('"' + stringValue + '"') : stringValue;
	}

	public static Collection<String> collectImportedTypes(org.eclipse.uml2.uml.Class target) {
		Collection<String> result = new TreeSet<String>();
		EObjectTypeRelationCondition isConnectable = new EObjectTypeRelationCondition(UMLPackage.Literals.CONNECTABLE_ELEMENT, TypeRelation.SAMETYPE_OR_SUBTYPE_LITERAL);
		IQueryResult partial = new SELECT(new FROM(target), new WHERE(isConnectable)).execute();
		for (EObject eObject : partial) {
			TypedElement asTypedElement = (TypedElement) eObject;
			MultiplicityElement asMultiplicityElement = (MultiplicityElement ) eObject;
			if (asMultiplicityElement.isMultivalued())
				result.add(mapToCollectionName(true, asMultiplicityElement));
			if (asTypedElement.getType() != null && asTypedElement.getType().getPackage() != target.getPackage() && !BasicTypeUtils.isBasicType(asTypedElement.getType()))
				result.add(mapTypeReference(asTypedElement.getType()));
		}
		return result;
	}
	
	public static String mapQualifiedName(String qualifiedName) {
		return qualifiedName.replace("::", ".");
	}

	public static String mapTypedElementType(TypedElement element, boolean specific) {
		if (element == null)
			return "void";
		final String baseTypeName = mapTypeReference(element.getType());
		if (!(element instanceof MultiplicityElement))
			return baseTypeName;
		MultiplicityElement me = (MultiplicityElement) element;
		if (!me.isMultivalued())
			return baseTypeName;
		return mapToCollectionName(true, me) + "<" + baseTypeName + ">";
	}

	private static String mapToCollectionName(boolean specific,
			MultiplicityElement me) {
		Class<?> collectionClass;
		if (specific) {
			boolean ordered = me.isOrdered();
			boolean unique = me.isUnique();
			collectionClass = ordered ? List.class : (unique ? Set.class : List.class);
		} else
			collectionClass = Collection.class;
		String collectionClassName = collectionClass.getName();
		return collectionClassName;
	}

	public static String mapTypeReference(Type type) {
		if (type == null)
			return "void";
		String qualifiedName = type.getQualifiedName();
		if (type instanceof PrimitiveType) {
			if (qualifiedName.equals("UMLPrimitiveTypes::Integer"))
				return "int";
			if (qualifiedName.equals("UMLPrimitiveTypes::String"))
				return "String";
			if (qualifiedName.equals("UMLPrimitiveTypes::Boolean"))
				return "boolean";
			if (qualifiedName.equals("base::Decimal"))
				return "double";
			return "<<?>>";
		}
		if (StereotypeUtils.hasStereotype(type, POJOMapper.EXTERNAL_STEREOTYPE)) {
			IExternalTypeMapper mapper = ExternalTypeMappingManager.getInstance().getMapper(type.getQualifiedName());
			return mapper == null ? "//FIXME: unknown mapper for " + type.getQualifiedName() : mapper.mapTypeReference(type);
		}
		return mapQualifiedName(type.getQualifiedName());
	}

}
