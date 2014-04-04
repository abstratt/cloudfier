package com.abstratt.kirra.mdd.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;

import com.abstratt.kirra.mdd.core.KirraHelper;

public class KirraUIHelper extends KirraHelper {
	public static boolean isEditable(Property umlAttribute, boolean creation) {
		return !isReadOnly(umlAttribute, creation);
	}
	
	public static boolean isChildTabRelationship(final Property it) {
		return get(it, "isChildTabRelationship", new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return it.isMultivalued() && isPublic(it) && it.isNavigable();
			}
		});
	}
	
	public static List<Property> getChildTabRelationships(final Class it) {
		return get(it, "getChildTabRelationships", new Callable<List<Property>>() {
			@Override
			public List<Property> call() throws Exception {
				List<Property> childTabs = new ArrayList<Property>();
				for (Property property : getRelationships(it))
					if (isChildTabRelationship(property))
						childTabs.add(property);
				return childTabs;
			}
		});
	}
	
	public static boolean hasChildTabRelationship(final Class clazz) {
		return !getChildTabRelationships(clazz).isEmpty();
	}
	
	public static boolean isDetachableRelationship(Property it) {
		return it.isMultivalued();
	}
	
	public static List<Property> tabledProperties(final org.eclipse.uml2.uml.Class entity) {
		return get(entity, "tabledProperties", new Callable<List<Property>>() {
			@Override
			public List<Property> call() throws Exception {
			    List<Property> result = new ArrayList<Property>();
			    for (Property it : KirraHelper.getPropertiesAndRelationships(entity))
			    	if (isEssential(it))
			    		result.add(it);
			    return result;
			}
		});
	}

	public static boolean isUserVisible(Property property) {
		if (isParentRelationship(property) && !isTopLevel((Classifier) property.getOwner()))
			return false;
		return isPublic(property);
	}
	
	public static List<Property> getFormFields(final org.eclipse.uml2.uml.Class entity) {
		return get(entity, "getFormFields", new Callable<List<Property>>() {
			@Override
			public List<Property> call() throws Exception {
			    List<Property> result = new ArrayList<Property>();
			    for (Property it : KirraHelper.getPropertiesAndRelationships(entity))
			    	if (isFormField(it)) {
			    		result.add(it);
			    	}
			    return result;
			}
		});
	}
	
	public static boolean isConcreteEntity(Classifier it) {
		return isEntity(it) && isConcrete(it);
	}
	
	public static boolean isTopLevelEntity(Classifier it) {
		return isEntity(it) && isConcrete(it) && isTopLevel(it);
	}

	public static List<Class> getEntities(final List<org.eclipse.uml2.uml.Package> namespaces) {
		List<Class> entities = new ArrayList<Class>();
		for (org.eclipse.uml2.uml.Package namespace : namespaces)
			entities.addAll(getEntities(namespace));
		return entities;
	}

	private static List<Class> getEntities(
			final org.eclipse.uml2.uml.Package namespace) {
		return get(namespace, "getEntities", new Callable<List<Class>>() {
			@Override
			public List<Class> call() throws Exception {
				List<Class> entities = new ArrayList<Class>();
				for (Type type : namespace.getOwnedTypes())
					if (KirraHelper.isEntity(type))
						entities.add((Class) type);
				return entities;
			}
		});
	}
	
	private static List<Class> getUserEntities(
			final org.eclipse.uml2.uml.Package namespace) {
		return get(namespace, "getUserEntities", new Callable<List<Class>>() {
			@Override
			public List<Class> call() throws Exception {
				List<Class> entities = new ArrayList<Class>();
				for (Type type : namespace.getOwnedTypes())
					if (KirraHelper.isEntity(type) && isUser((Classifier) type))
						entities.add((Class) type);
				return entities;
			}
		});
	}

	public static List<Class> getUserEntities(final List<org.eclipse.uml2.uml.Package> namespaces) {
		List<Class> userEntities = new ArrayList<Class>();
		for (org.eclipse.uml2.uml.Package namespace : namespaces)
			userEntities.addAll(getUserEntities(namespace));
		return userEntities;
	}

	public static boolean isFormField(Property it) {
		return isUserVisible(it) &&	!it.isMultivalued();
	}
	
	public static boolean isEditableFormField(Property it, boolean creation) {
		return isFormField(it) && isEditable(it, creation);
	}
	
	public static boolean isEditableFormField(Parameter it, boolean creation) {
		return !isReadOnly(it);
	}

}
