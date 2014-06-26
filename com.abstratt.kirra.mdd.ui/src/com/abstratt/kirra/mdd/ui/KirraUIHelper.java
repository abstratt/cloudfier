package com.abstratt.kirra.mdd.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;

import com.abstratt.kirra.mdd.core.KirraHelper;

public class KirraUIHelper extends KirraHelper {
    public static List<Property> getChildTabRelationships(final Class it) {
        return KirraHelper.get(it, "getChildTabRelationships", new Callable<List<Property>>() {
            @Override
            public List<Property> call() throws Exception {
                List<Property> childTabs = new ArrayList<Property>();
                for (Property property : KirraHelper.getRelationships(it))
                    if (KirraUIHelper.isChildTabRelationship(property))
                        childTabs.add(property);
                return childTabs;
            }
        });
    }

    public static List<Class> getEntities(final List<org.eclipse.uml2.uml.Package> namespaces) {
        List<Class> entities = new ArrayList<Class>();
        for (org.eclipse.uml2.uml.Package namespace : namespaces)
            entities.addAll(KirraUIHelper.getEntities(namespace));
        return entities;
    }

    public static List<Property> getFormFields(final org.eclipse.uml2.uml.Class entity) {
        return KirraHelper.get(entity, "getFormFields", new Callable<List<Property>>() {
            @Override
            public List<Property> call() throws Exception {
                List<Property> result = new ArrayList<Property>();
                for (Property it : KirraHelper.getPropertiesAndRelationships(entity))
                    if (KirraUIHelper.isFormField(it)) {
                        result.add(it);
                    }
                return result;
            }
        });
    }

    public static List<Class> getUserEntities(final List<org.eclipse.uml2.uml.Package> namespaces) {
        List<Class> userEntities = new ArrayList<Class>();
        for (org.eclipse.uml2.uml.Package namespace : namespaces)
            userEntities.addAll(KirraUIHelper.getUserEntities(namespace));
        return userEntities;
    }

    public static boolean hasChildTabRelationship(final Class clazz) {
        return !KirraUIHelper.getChildTabRelationships(clazz).isEmpty();
    }

    public static boolean isChildTabRelationship(final Property it) {
        return KirraHelper.get(it, "isChildTabRelationship", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return it.isMultivalued() && KirraHelper.isPublic(it) && it.isNavigable();
            }
        });
    }

    public static boolean isConcreteEntity(Classifier it) {
        return KirraHelper.isEntity(it) && KirraHelper.isConcrete(it);
    }

    public static boolean isDetachableRelationship(Property it) {
        return it.isMultivalued();
    }

    public static boolean isEditable(Property umlAttribute, boolean creation) {
        return !KirraHelper.isReadOnly(umlAttribute, creation);
    }

    public static boolean isEditableFormField(Parameter it, boolean creation) {
        return !KirraHelper.isReadOnly(it);
    }

    public static boolean isEditableFormField(Property it, boolean creation) {
        return KirraUIHelper.isFormField(it) && KirraUIHelper.isEditable(it, creation);
    }

    public static boolean isFormField(Property it) {
        return KirraHelper.isUserVisible(it) && !it.isMultivalued();
    }

    public static boolean isTopLevelEntity(Classifier it) {
        return KirraHelper.isEntity(it) && KirraHelper.isConcrete(it) && KirraHelper.isTopLevel(it);
    }

    public static List<Property> tabledProperties(final org.eclipse.uml2.uml.Class entity) {
        return KirraHelper.get(entity, "tabledProperties", new Callable<List<Property>>() {
            @Override
            public List<Property> call() throws Exception {
                List<Property> result = new ArrayList<Property>();
                for (Property it : KirraHelper.getPropertiesAndRelationships(entity))
                    if (KirraHelper.isEssential(it))
                        result.add(it);
                return result;
            }
        });
    }

    private static List<Class> getEntities(final org.eclipse.uml2.uml.Package namespace) {
        return KirraHelper.get(namespace, "getEntities", new Callable<List<Class>>() {
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

    private static List<Class> getUserEntities(final org.eclipse.uml2.uml.Package namespace) {
        return KirraHelper.get(namespace, "getUserEntities", new Callable<List<Class>>() {
            @Override
            public List<Class> call() throws Exception {
                List<Class> entities = new ArrayList<Class>();
                for (Type type : namespace.getOwnedTypes())
                    if (KirraHelper.isEntity(type) && KirraHelper.isUser((Classifier) type))
                        entities.add((Class) type);
                return entities;
            }
        });
    }

}
