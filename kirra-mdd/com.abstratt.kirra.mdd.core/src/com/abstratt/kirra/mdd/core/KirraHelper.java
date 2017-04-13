package com.abstratt.kirra.mdd.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Path;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.BehavioralFeature;
import org.eclipse.uml2.uml.BehavioredClassifier;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Feature;
import org.eclipse.uml2.uml.Interface;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Reception;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.VisibilityKind;

import com.abstratt.kirra.Relationship.Style;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.util.AssociationUtils;
import com.abstratt.mdd.core.util.BasicTypeUtils;
import com.abstratt.mdd.core.util.FeatureUtils;
import com.abstratt.mdd.core.util.ClassifierUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.core.util.NamedElementUtils;
import com.abstratt.mdd.core.util.StateMachineUtils;
import com.abstratt.mdd.core.util.StereotypeUtils;
import com.abstratt.pluginutils.NodeSorter;

public class KirraHelper {
    public static class Metadata {
        public Map<String, Map<String, Object>> values = new HashMap<String, Map<String,Object>>();
    }
    
    private static boolean inSession() {
        Assert.isTrue(RepositoryService.DEFAULT.isInSession());
        return RepositoryService.DEFAULT.getCurrentResource().hasFeature(Metadata.class);
    }
    
    public static List<Class> getPrerequisites(Class entity) {
        return addPrerequisites(entity, new ArrayList<Class>());
    }
    
    public static List<Class> addPrerequisites(Class entity, List<Class> collected) {
        if (!collected.contains(entity)) {
            collected.add(entity);
            for (Property relationship : getRelationships(entity))
                if (isPrimary(relationship) && isRequired(relationship))
                    addPrerequisites((Class) relationship.getType(), collected);
        }
        return collected;
    }

    protected static <T> T get(String identity, String property, Callable<T> retriever) {
        ensureSession();
        Map<String, Object> cachedProperties = null;
        cachedProperties = getCachedProperties(identity);
        if (cachedProperties == null)
            cachedProperties = initCachedProperties(identity);
        else 
            if (cachedProperties.containsKey(property)) {
                // WIN: from cache
                final T cachedValue = (T) cachedProperties.get(property);
                return cachedValue;
            }
        T retrieved;
        try {
            retrieved = retriever.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        cachedProperties.put(property, retrieved);
        return retrieved;
    }
    
    protected static <T> T get(NamedElement target, String property, Callable<T> retriever) {
        String identity = target.getQualifiedName();
        if (identity == null)
            identity = target.eClass().getName() + "@" + System.identityHashCode(target);
        return get(identity, property, retriever);
    }
    
    private static Map<String, Object> initCachedProperties(String identity) {
        HashMap<String, Object> cachedProperties;
        getMetadataCache().values.put(identity, cachedProperties = new HashMap<String, Object>());
        return cachedProperties;
    }

    private static Map<String, Object> getCachedProperties(String identity) {
        return getMetadataCache().values.get(identity);
    }

    private static Metadata getMetadataCache() {
        return (Metadata) RepositoryService.DEFAULT.getCurrentResource().getFeature(Metadata.class);
    }

    private static void ensureSession() {
        Assert.isTrue(inSession(), "Session must be started");
    }
    
    public static boolean hasStereotype(final NamedElement type, final String stereotypeName) {
        return get(type, "hasStereotype_" + stereotypeName, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return StereotypeUtils.hasStereotype(type, stereotypeName);
            }
        });
    }
    
    private static void addApplications(org.eclipse.uml2.uml.Package target, Set<Package> collected) {
        if (isApplication(target)) {
            collected.add(target);
            for (Package it : target.getImportedPackages())
                if (!collected.contains(it))
                    addApplications(it, collected);
        }
    }

    public static Collection<Package> getApplicationPackages(Package... packages) {
        Set<Package> applicationPackages = new LinkedHashSet<Package>();
        for (Package it : packages)
            KirraHelper.addApplications(it, applicationPackages);
        return applicationPackages;
    }
    
    public static Package getApplicationPackage(Package... packages) { 
    	return Arrays.stream(packages).filter(it -> "userprofile".equals(it.getName())).findAny().orElse(null);
    }
    
    public static Collection<Package> getEntityPackages(Package... packages) {
        Set<Package> applicationPackages = new LinkedHashSet<Package>();
        for (Package it : packages)
            KirraHelper.addApplications(it, applicationPackages);
        return getApplicationPackages(packages).stream().filter(it -> hasEntity(it)).collect(Collectors.toList());
    }

    public static List<Property> getEntityRelationships(Classifier modelClass) {
        return getRelationships(modelClass, false);
    }
    
    public static List<Property> getRelationships(Classifier modelClass) {
        return getRelationships(modelClass, false);
    }

    public static List<Property> getRelationships(final Classifier modelClass, final boolean navigableOnly) {
        return get(modelClass, "getRelationships_" + navigableOnly, new Callable<List<Property>>() {
            @Override
            public List<Property> call() throws Exception {
                Collection<Property> entityRelationships = new LinkedHashSet<Property>();
                for (org.eclipse.uml2.uml.Property attribute : modelClass.getAllAttributes())
                    if (isRelationship(attribute)) 
                    	if (attribute.getVisibility() == VisibilityKind.PUBLIC_LITERAL)
                    		entityRelationships.add(attribute);
                addAssociationOwnedRelationships(modelClass, navigableOnly, entityRelationships);
                List<Property> relationships = removeDuplicates(entityRelationships, propertySelector(modelClass));
				return relationships;
            }
        });
    }
    
    private static <T extends Feature> List<T> removeDuplicates(Collection<T> original, BinaryOperator<T> conflictSolver) {
    	return new ArrayList<T>(original.stream().collect(Collectors.toMap((it -> it.getName()), (it -> it), conflictSolver, LinkedHashMap::new)).values());
    }
    
    protected static BinaryOperator<Property> propertySelector(Classifier context) {
    	return (a, b) -> {
    		Classifier aType;
    		Classifier bType; 
    		if (a.getOtherEnd() != null && b.getOtherEnd() != null) {
	    		aType = (Classifier) a.getOtherEnd().getType();
	    		bType = (Classifier) b.getOtherEnd().getType();
    		} else {
    			aType = (Classifier) a.getClass_();
	    		bType = (Classifier) b.getClass_();
    		}
    		boolean switched = aType != bType && ClassifierUtils.isKindOf(bType, aType);
			return switched ? b : a;
		};
    }
    
    protected static BinaryOperator<Operation> operationSelector(Classifier context) {
    	return (a, b) -> {
    		boolean isSwitched = ClassifierUtils.isKindOf(b.getClass_(), a.getClass_());
			return isSwitched ? b : a;
		};
    }

    protected static void addAssociationOwnedRelationships(Classifier modelClass, boolean navigableOnly, Collection<Property> entityRelationships) {
        List<Classifier> allLevels = new ArrayList<Classifier>(modelClass.allParents());
        allLevels.add(modelClass);
        for (Classifier level : allLevels)
            for (Association association : AssociationUtils.getOwnAssociations(level)) 
                for (Property forwardReference : AssociationUtils.getMemberEnds(association, level))
                    if (forwardReference != null && isRelationship(forwardReference, navigableOnly))
                        entityRelationships.add(forwardReference);
    }

    /**
     * Tests whether a type is an entity type. A type is an entity type if: 
     * 
     * - it is a classifier 
     * - it is virtual or has at least a property, even if derived - OR ELSE USERS CAN'T MAKE SENSE OF IT!!!
     */
    public static boolean isEntity(final Type type) {
        return get(type, "isEntity", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Classifier asClassifier = (Classifier) type;
				boolean isEntity = isBasicallyAnEntity(type) && !isService(type) && (asClassifier.isAbstract() || hasProperties(asClassifier));
				return isEntity;
            }
        });
    }
    
    public static boolean isBasicallyAnEntity(final Type type) {
        return get(type, "isBasicallyAnEntity", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
            	String typePackageName = type.getNearestPackage().getName();
				if (typePackageName.equals(IRepository.COLLECTIONS_NAMESPACE))
            		return false;
				if (typePackageName.equals(IRepository.TYPES_NAMESPACE))
            		return false;
				if (typePackageName.equals(IRepository.MEDIA_NAMESPACE))
            		return false;
				if (type.getName() == null)
					return false;
				if (type.eClass() != UMLPackage.Literals.CLASS)
					return false;
                return type.getVisibility() == VisibilityKind.PUBLIC_LITERAL;
            }
        });
    }

    private static boolean hasProperties(Classifier type) {
        for (Property attribute : type.getAttributes())
            if (isProperty(attribute))
                return true;
        for (Classifier general : type.getGenerals())
            if (hasProperties(general))
                return true;
        return false;
    }
    
    @Deprecated
    // use getUserClass instead
    public static Collection<Class> getUserClasses() {
    	Class userClass = getUserClass();
    	return userClass != null ? Arrays.asList(userClass) : Collections.emptyList();
    }
    
    public static Class getUserClass() {
    	IRepository mddRepository = RepositoryService.DEFAULT.getCurrentResource().getFeature(IRepository.class);
    	return get(mddRepository.getBaseURI().toString(), "getUserClass", new Callable<Class>() {
            @Override
            public Class call() throws Exception {
            	Class userProfileClass = mddRepository.findNamedElement("userprofile::Profile", UMLPackage.Literals.CLASS, null);
            	return userProfileClass;
            }
        });
    }

    public static boolean isRole(final Classifier classifier) {
        return get(classifier, "isRole", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                boolean isRole = !classifier.isAbstract() && MDDExtensionUtils.isRoleClass(classifier);
				return isRole;
            }
        });
    }
    
    public static boolean isUser(final Classifier classifier) {
        return get(classifier, "isUser", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return !classifier.isAbstract() && classifier.getQualifiedName().equals("userprofile::Profile");
            }
        });
    }
        
    
    public static Property getUsernameProperty(final Classifier userClass) {
        return get(userClass, "getUsernameProperty", new Callable<Property>() {
            @Override
            public Property call() throws Exception {
            	Property property = FeatureUtils.findAttribute(userClass, "username", false, true);
                return property != null && isUserNameProperty(property) ? property : null;
            }
        });
    }

    /** Is the given property usable as a username property? */
    public static boolean isUserNameProperty(Property property) {
    	return isUnique(property) && !isEditable(property) && property.getType()!= null && "String".equals(property.getType().getName());
	}

	public static boolean isRequired(Property property) {
        return isRequired(property, false);
    }
    
    /**
     * 
     * @param property
     * @param creation 
     * @return
     */
    public static boolean isRequired(final Property property, final boolean creation) {
        return get(property, "isRequiredProperty_" + creation, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return property.isNavigable() && !isReadOnly(property, creation) && isBasicallyRequired(property) && !isBlob(property.getType()) && !isGeolocation(property.getType());
            }
        });
    }

	public static boolean isBasicallyRequired(final Property property) {
        return get(property, "isBasicallyRequiredProperty", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return property.getLower() > 0;
            }

        });

    }

    public static boolean isStateProperty(final Property property) {
    	return property.getType() instanceof StateMachine;
    }
    public static boolean isRequired(final Parameter parameter) {
        return get(parameter, "isRequiredParameter", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return parameter.getLower() > 0 && !hasDefault(parameter);
            }
        });
    }

    /**
     * @param parameter
     * @param creation only for uniformity so clients call call isRequired(TypedElement, boolean) 
     * @return
     */
    public static boolean isRequired(Parameter parameter, boolean creation) {
        return isRequired(parameter);
    }

    public static boolean isAction(Operation operation) {
        return isPublic(operation) && !operation.isQuery() && VisibilityKind.PUBLIC_LITERAL == operation.getVisibility();
    }
    
    public static boolean isEntityOperation(Operation operation) {
        return isAction(operation) || isFinder(operation);
    }

    public static List<Parameter> getParameters(BehavioralFeature operation) {
        return FeatureUtils.filterParameters(operation.getOwnedParameters(), ParameterDirectionKind.IN_LITERAL);
    }

    public static boolean isFinder(Operation operation) {
        return isPublic(operation) && operation.getReturnResult() != null && operation.isStatic() && operation.isQuery();
    }

    public static boolean isNamespace(org.eclipse.uml2.uml.Package package_) {
        return !(package_ instanceof Profile);
    }

    public static boolean isProperty(final Property attribute) {
        return get(attribute, "isProperty", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // no support for static properties
                boolean isProperty = isRegularProperty(attribute) && isInstance(attribute) && attribute.getName() != null && !isBasicallyAnEntity(attribute.getType());
				return isProperty;
            }
        });
    }

    public static boolean isRegularProperty(Property attribute) {
        return attribute.eClass() == UMLPackage.Literals.PROPERTY && attribute.getType() != null && (attribute.getType().eClass() == UMLPackage.Literals.CLASS || attribute.getType().eClass() == UMLPackage.Literals.STATE_MACHINE || attribute.getType().eClass() == UMLPackage.Literals.ENUMERATION);
    }

    public static boolean isInstance(Property attribute) {
        return !isStatic(attribute);
    }

    public static boolean isPublic(final NamedElement feature) {
        return get(feature, "isPublic", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return feature.getVisibility() == VisibilityKind.PUBLIC_LITERAL;
            }
        });

    }

    public static boolean isParentRelationship(final Property attribute) {
        return get(attribute, "isParentRelationship", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return !attribute.isMultivalued() && attribute.getOtherEnd() != null && isChildRelationship(attribute.getOtherEnd());
            }
        });
    }

    public static boolean isChildRelationship(final Property attribute) {
        return get(attribute, "isChildRelationship", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return isRelationship(attribute) && attribute.getAggregation() != AggregationKind.NONE_LITERAL;
            }
        });
    }

    public static boolean isLinkRelationship(final Property attribute) {
        return get(attribute, "isLinkRelationship", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return isRelationship(attribute) && attribute.getAggregation() == AggregationKind.NONE_LITERAL;
            }
        });
    }
    
    public static boolean isLikeLinkRelationship(final Property attribute) {
        return get(attribute, "isLikeLinkRelationship", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return isLinkRelationship(attribute) ||
                        (isChildRelationship(attribute) && isTopLevel((Classifier) attribute.getType())) || 
                        (isParentRelationship(attribute) && isTopLevel(attribute.getClass_()));
            }
        });
    }


    public static boolean isRelationship(Property attribute) {
        return isRelationship(attribute, false);
    }

    public static boolean isRelationship(final Property attribute, boolean navigableOnly) {
        return get(attribute, "isRelationshipAttribute_"+navigableOnly, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // derived relationships might not actually have an association
                return isRegularProperty(attribute) && attribute.getName() != null
                        && isBasicallyAnEntity(attribute.getType())
                        && (attribute.isDerived() || attribute.getAssociation() != null);
            }
        });
    }

    public static String metaClass(Element element) {
        return element.eClass().getName();
    }

    public static boolean isAutoGenerated(final org.eclipse.uml2.uml.Property umlAttribute) {
    	return get(umlAttribute, "isAutoGenerated", new Callable<Boolean>() {
    		@Override
    		public Boolean call() throws Exception {
    			return umlAttribute.isDerived();
    		}
    	});
    }

    public static boolean isSequence(final org.eclipse.uml2.uml.Property umlAttribute) {
    	return get(umlAttribute, "isSequence", new Callable<Boolean>() {
    		@Override
    		public Boolean call() throws Exception {
    			return isAutoGenerated(umlAttribute) && isPropertyUnique(umlAttribute) && !hasDefault(umlAttribute);
    		}
    	});
    }

	public static boolean isPropertyDerived(final org.eclipse.uml2.uml.Property umlAttribute) {
    	return isDerived(umlAttribute);
    }
    public static boolean isRelationshipDerived(final org.eclipse.uml2.uml.Property umlAttribute) {
    	return isDerived(umlAttribute);
    }    

    
    public static boolean isDerivedRelationship(final org.eclipse.uml2.uml.Property umlAttribute) {
        return isRelationship(umlAttribute) && isDerived(umlAttribute);
    }
    

    public static boolean isDerived(final org.eclipse.uml2.uml.Property umlAttribute) {
        return get(umlAttribute, "isDerived", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return umlAttribute.isDerived() && umlAttribute.getDefaultValue() != null;
            }
        });
    }
    
    /**
     * A read-only property is not ever editable. 
     */
    public static boolean isReadOnly(org.eclipse.uml2.uml.Property umlAttribute) {
        return isReadOnly(umlAttribute, false);
    }
    
    public static boolean isUpdatable(org.eclipse.uml2.uml.Property umlAttribute) {
    	return isReadOnly(umlAttribute, false);
    }
    
    public static boolean isAlwaysReadOnly(org.eclipse.uml2.uml.Property umlAttribute) {
    	return isReadOnly(umlAttribute, false) && isReadOnly(umlAttribute, true);
    }
    
    /**
     * 
     * @param umlAttribute
     * @param creationTime is this for creation time or in general? A property may be read-only after the object is created but writable at creation time (because it is a required property).
     * @return
     */
    public static boolean isReadOnly(final org.eclipse.uml2.uml.Property umlAttribute, final boolean creationTime) {
        return get(umlAttribute, "isReadOnlyProperty_" + creationTime, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (umlAttribute.isDerived())
                    return true;
                if (isStateProperty(umlAttribute))
                    return true;
                if (umlAttribute.getOtherEnd() != null && umlAttribute.getOtherEnd().getAggregation() != AggregationKind.NONE_LITERAL)
                    return true;
                return umlAttribute.isReadOnly() && (!creationTime || !isBasicallyRequired(umlAttribute));
            }
        });
    }
    
    /**
     * A initializable property can be set at creation time only.
     */
    public static boolean isInitializable(org.eclipse.uml2.uml.Property umlAttribute) {
    	if (isBlob(umlAttribute.getType()))
    		return false;
        return !isReadOnly(umlAttribute, true);
    }
    
    /**
     * An editable property can be updated any time (creation or later).
     */
    public static boolean isEditable(org.eclipse.uml2.uml.Property umlAttribute) {
    	if (isBlob(umlAttribute.getType()))
    		return false;
        return !isReadOnly(umlAttribute, false);
    }

    public static boolean isReadOnly(final Class umlClass) {
        return get(umlClass, "isReadOnlyClass", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                List<Property> all = getPropertiesAndRelationships(umlClass);
                for (Property property : all)
                    if (!isReadOnly(property))
                        return false;
                return true;
            }
        });
    }

    public static boolean isDerived(Parameter parameter) {
        return false;
    }

    public static boolean isReadOnly(Parameter parameter) {
        return parameter.getDirection() == ParameterDirectionKind.OUT_LITERAL || parameter.getDirection() == ParameterDirectionKind.RETURN_LITERAL;
    }
    /**
     * This version is here for uniformity so clients can just invoke isReadOnly(TypedElement,Boolean) 
     */
    public static boolean isReadOnly(Parameter parameter, boolean creation) {
        return isReadOnly(parameter);
    }

    public static List<Property> getProperties(final Classifier umlClass) {
        return get(umlClass, "getProperties", new Callable<List<Property>>() {
            @Override
            public List<Property> call() throws Exception {
                List<Property> entityProperties = new ArrayList<Property>();
                addEntityProperties(umlClass, entityProperties);
                return removeDuplicates(entityProperties, propertySelector(umlClass));
            }
        });
    }
    
    public static List<Property> getTupleProperties(final Classifier dataType) {
        return get(dataType, "getTupleProperties", new Callable<List<Property>>() {
            @Override
            public List<Property> call() throws Exception {
                List<Property> tupleProperties = new ArrayList<Property>();
                addTupleProperties(dataType, tupleProperties);
                return removeDuplicates(tupleProperties, propertySelector(dataType));
            }
        });
    }
    
    private static void addTupleProperties(Classifier dataType, List<Property> tupleProperties) {
        for (Classifier general : dataType.getGenerals())
            addTupleProperties(general, tupleProperties);
        for (Property attribute : dataType.getAttributes())
            if (attribute.getName() != null && isPublic(attribute) && attribute.eClass() == UMLPackage.Literals.PROPERTY)
                tupleProperties.add(attribute);
    }
    
	public static List<String> getOrderedDataElements(Class umlClass) {
		return getPropertiesAndRelationships(umlClass).stream().map(it -> KirraHelper.getName(it))
				.collect(Collectors.toList());
	}

    public static List<Property> getPropertiesAndRelationships(final Classifier umlClass) {
        return get(umlClass, "getPropertiesAndRelationships", new Callable<List<Property>>() {
            @Override
            public List<Property> call() throws Exception {
                LinkedHashSet<Property> entityProperties = new LinkedHashSet<Property>();
                addEntityPropertiesAndRelationships(umlClass, entityProperties);
                addAssociationOwnedRelationships(umlClass, true, entityProperties);
                return removeDuplicates(entityProperties, propertySelector(umlClass));
            }
        });
    }
    
    private static void addEntityPropertiesAndRelationships(Classifier umlClass, LinkedHashSet<Property> entityProperties) {
        for (Classifier general : umlClass.getGenerals())
            addEntityPropertiesAndRelationships(general, entityProperties);
        for (Property attribute : umlClass.getAttributes())
            if (isProperty(attribute) || isRelationship(attribute))
                entityProperties.add(attribute);
    }

    /**
     * Collects entity properties, inherited ones first.
     * 
     * @param umlClass
     * @param entityProperties
     */
    private static void addEntityProperties(Classifier umlClass, List<Property> entityProperties) {
        for (Classifier general : umlClass.getGenerals())
            addEntityProperties(general, entityProperties);
        for (Property attribute : umlClass.getAttributes())
            if (isProperty(attribute))
                entityProperties.add(attribute);
    }

    public static List<Operation> getQueries(final Class umlClass) {
        return get(umlClass, "getQueries", new Callable<List<Operation>>() {
            @Override
            public List<Operation> call() throws Exception {
                List<Operation> queries = new ArrayList<Operation>();
                for (Operation operation : umlClass.getAllOperations())
                    if (isFinder(operation))
                        queries.add(operation);
                return removeDuplicates(queries, operationSelector(umlClass));
            }
        });
    }

    public static List<Operation> getActions(final Class umlClass) {
        return get(umlClass, "getActions", new Callable<List<Operation>>() {
            @Override
            public List<Operation> call() throws Exception {
                List<Operation> actions = new ArrayList<Operation>();
                for (Operation operation : umlClass.getAllOperations())
                    if (isAction(operation))
                        actions.add(operation);
                return removeDuplicates(actions, operationSelector(umlClass));
            }
        });
    }
    
    public static List<Operation> getInstanceActions(final Class umlClass) {
        return get(umlClass, "getInstanceActions", new Callable<List<Operation>>() {
            @Override
            public List<Operation> call() throws Exception {
                List<Operation> instanceActions = new ArrayList<Operation>();
                for (Operation operation : getActions(umlClass))
                    if (!operation.isStatic())
                        instanceActions.add(operation);
                return instanceActions;
            }
        });
    }
    
    public static List<Operation> getEntityActions(final Class umlClass) {
        return get(umlClass, "getEntityActions", new Callable<List<Operation>>() {
            @Override
            public List<Operation> call() throws Exception {
                List<Operation> entityActions = new ArrayList<Operation>();
                for (Operation operation : getActions(umlClass))
                    if (operation.isStatic())
                        entityActions.add(operation);
                return entityActions;
            }
        });
    }

    public static boolean isConcrete(Classifier umlClass) {
        return !umlClass.isAbstract();
    }

    public static boolean isTopLevel(final Classifier umlClass) {
        return get(umlClass, "isTopLevel", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
            	if (!isUserVisible(umlClass))
            		return false;
            	if (!isConcrete(umlClass))
            		return false;
                for (Operation operation : umlClass.getAllOperations())
                    if (isFinder(operation))
                        return true;
                int parentCount = 0;
                for (Property attribute : getRelationships(umlClass))
                    if (attribute.getOtherEnd() != null)
                        if (attribute.getOtherEnd().isComposite())
                            parentCount++;
                // If has exactly one parent, it is not top-level.
                // We used to care about whether there were references from other (non-parent) entities
                // but since we could tell whether that was required, and that is not what the UI needed, 
                // we no longer do that
                boolean isTopLevel = parentCount != 1;
				return isTopLevel;
            }
        });
    }

    public static boolean isStandalone(Class umlClass) {
        return true;
    }

    public static Property getMnemonic(final Classifier clazz) {
        return get(clazz, "getMnemonic", new Callable<Property>() {
            @Override
            public Property call() throws Exception {
                return getPropertiesAndRelationships(clazz).stream().filter(it -> isUserVisible(it)).findAny().orElse(null);
            }
        });
    }
    
    public static String getDescription(Element element) {
        return MDDUtil.getDescription(element);
    }

    /**
     * The primary end in a relationship. 
     * If only one end is navigable, that end is the primary one.
     * If both ends are navigable, the non-multiple end is the primary one.
     * If both ends are multiple or non-multiple, the required end is the primary one.
     * If both ends are required or non-required, the first end is the primary one.
     * 
     *  We expect associations to have at least one navigable end, 
     *  so a relationship *always* has a primary end, and only one.
     */
    public static boolean isPrimary(final org.eclipse.uml2.uml.Property thisEnd) {
        return get(thisEnd, "isPrimary", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (thisEnd.getAssociation() == null)
                    return false;
                if (!thisEnd.isNavigable())
                    return false;
                Property otherEnd = thisEnd.getOtherEnd();
                if (otherEnd == null)
                    return true;
                if (!otherEnd.isNavigable())
                    return true;
                if (thisEnd.isMultivalued() != otherEnd.isMultivalued())
                    return !thisEnd.isMultivalued();
                if (isBasicallyRequired(thisEnd) != isBasicallyRequired(otherEnd))
                    return isBasicallyRequired(thisEnd);
                // only one side is association owned
                if (thisEnd.getAssociation().getOwnedEnds().size() == 1)
                	return !thisEnd.getAssociation().getOwnedEnds().contains(thisEnd);
                // both owned or none owned, pick the first one as a tie breaker
                Property firstEnd = thisEnd.getAssociation().getMemberEnds().get(0);
                return firstEnd == thisEnd;
            }
        });        
    }

    public static String getSymbol(org.eclipse.uml2.uml.NamedElement sourceElement) {
        Assert.isNotNull(sourceElement.getName());
        String mangled = sourceElement.getName().replaceAll("[\\W]", "_");
        return mangled;
    }
    
    public static String getLabel(org.eclipse.uml2.uml.NamedElement sourceElement) {
        String symbol = sourceElement.getName();
        return getLabelFromSymbol(symbol);
    }

    public static String getLabelFromSymbol(String symbol) {
        return StringUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(symbol), ' ')).replaceAll("[\\w\\d]^|_", " ").replaceAll("[\\s]+", " ");
    } 
    
    public static String getName(org.eclipse.uml2.uml.NamedElement sourceElement) {
        return sourceElement.getName();
    }

    public static boolean isServiceOperation(
            final BehavioralFeature operation) {
        return get(operation, "isPrimary", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (!(operation.getOwner() instanceof Interface))
                    return false;
                if (operation instanceof Operation)
                    return ((Operation) operation).getReturnResult() != null;
                
                return operation instanceof Reception && operation.getName() != null && operation.getOwnedParameters().size() == 1 && operation.getOwnedParameters().get(0).getType() instanceof Signal;
            }
        });        
    }

    public static boolean isTupleType(final Type classifier) {
        return get(classifier, "isTupleType", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // excludes other DataTypes, such as primitives
                return classifier.getName() != null && (classifier.eClass() == Literals.DATA_TYPE || classifier.eClass() == Literals.SIGNAL) && classifier.getVisibility() != VisibilityKind.PRIVATE_LITERAL;
            }
        });
    }

    public static boolean isService(final Type umlClass) {
        return get(umlClass, "isService", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return umlClass instanceof BehavioredClassifier && hasStereotype(umlClass, "Service");
            }
        });
    }

    public static boolean isApplication(final org.eclipse.uml2.uml.Package current) {
        return get(current, "isApplication", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return isKirraPackage(current) && hasKirraType(current);
            }
        });
    }
    
    public static boolean isKirraPackage(final org.eclipse.uml2.uml.Package current) {
        return get(current, "isKirraPackage", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return StereotypeUtils.hasProfile(current, "kirra");
            }
        });
    }
    
    public static boolean hasKirraType(final org.eclipse.uml2.uml.Package current) {
        for (Type type : current.getOwnedTypes())
            if (isKirraType(type))
                return true;
        return false;
    }
    
    public static boolean hasEntity(final org.eclipse.uml2.uml.Package current) {
        for (Type type : current.getOwnedTypes())
            if (isEntity(type))
                return true;
        return false;
    }

    private static boolean isKirraType(Type type) {
        return isEntity(type) || isService(type) || isTupleType(type);
    }

    
    public static boolean isPropertyUnique(final org.eclipse.uml2.uml.Property umlAttribute) {
    	return isUnique(umlAttribute);
    }
    public static boolean isUnique(final org.eclipse.uml2.uml.Property umlAttribute) {
        return get(umlAttribute, "isUnique", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return umlAttribute.isID() && umlAttribute.getLower() > 0 && !umlAttribute.isMultivalued();
            }
        });
    }

    public static void addNonInstanceActions(Class target, Collection<Operation> collected) {
        for (Operation operation : target.getAllOperations())
            if (isAction(operation) && isStatic(operation))
                collected.add(operation);
    }
    
    public static boolean isStatic(final Feature operation) {
        return get(operation, "isStaticFeature", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return operation.isStatic();
            }
        });
    }

    public static List<Operation> getNonInstanceActions(Class umlClass) {
        List<Operation> actions = new LinkedList<Operation>();
        KirraHelper.addNonInstanceActions(umlClass, actions);
        return actions;
    }

    /**
     * An entity is not instantiable if it has at least one required field which is not initializable and is not automatically generated.
     */
    public static boolean isInstantiable(final Class umlClass) {
        return get(umlClass, "isInstantiable", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                List<Property> all = getPropertiesAndRelationships(umlClass);
                for (Property property : all)
					if (!isAutoGenerated(property) && !isInitializable(property) && isBasicallyRequired(property) && !isStateProperty(property))
                        return false;
                return true;
            }
        });
    }
    
    public static boolean isUserVisible(Property property) {
        if (isRelationship(property) && !property.isNavigable())
            return false;
        if (isParentRelationship(property) && !isTopLevel((Classifier) property.getOwner()))
            return false;
        return isPublic(property);
    }
    
    public static boolean isUserVisible(Classifier classifier) {
        return isPublic(classifier);
    }
    
    public static String getApplicationName(IRepository repository) {
        Properties repositoryProperties = repository.getProperties();
        String applicationName = repositoryProperties.getProperty(IRepository.APPLICATION_NAME);
        if (applicationName == null)
        	applicationName = "app";
		return applicationName.replaceAll("[^a-zA-Z0-9]","_");
    }
    
    public static String getApplicationLabel(IRepository repository) {
        Properties repositoryProperties = repository.getProperties();
        String applicationTitle = repositoryProperties.getProperty(IRepository.APPLICATION_TITLE);
        if (applicationTitle == null)
        	return getApplicationName(repository);
        return applicationTitle;
    }
    
    public static List<Class> getEntities(Collection<Package> applicationPackages) {
        List<Class> result = new ArrayList<Class>();
        for (Package current : applicationPackages)
            for (Type type : current.getOwnedTypes())
                if (KirraHelper.isEntity(type))
                    result.add((Class) type);
        return result;
    }
    
    public static List<Class> getRoleEntities(Collection<Package> applicationPackages) {
        return getEntities(applicationPackages).stream().filter(e -> isRole(e)).collect(Collectors.toList());
    }
    
    public static List<Class> getServices(Collection<Package> applicationPackages) {
        List<Class> result = new ArrayList<Class>();
        for (Package current : applicationPackages)
            for (Type type : current.getOwnedTypes())
                if (KirraHelper.isService(type))
                    result.add((Class) type);
        return result;
    }
    
    public static List<Classifier> getTupleTypes(Collection<Package> applicationPackages) {
        List<Classifier> result = new ArrayList<Classifier>();
        for (Package current : applicationPackages)
            for (Type type : current.getOwnedTypes())
                if (KirraHelper.isTupleType(type))
                    result.add((Classifier) type);
        return result;
    }

    public static boolean isEnumeration(final Type umlType) {
        return get(umlType, "isEnumeration", new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return umlType instanceof Enumeration || umlType instanceof StateMachine;
            }
        });
    }
    
    public static Map<String, String> getEnumerationLiterals(final Type enumOrStateMachine) {
        return get(enumOrStateMachine, "getEnumerationLiterals", new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
            	List<String> names;
                if (enumOrStateMachine instanceof Enumeration)
                	names = NamedElementUtils.getNames(((Enumeration) enumOrStateMachine).getOwnedLiterals());
				else if (enumOrStateMachine instanceof StateMachine)
                    names = NamedElementUtils.getNames(StateMachineUtils.getStates(((StateMachine) enumOrStateMachine)));
				else
					names = Arrays.<String>asList();
                Map<String, String> result = new LinkedHashMap<>();
                names.forEach(name -> result.put(name, getLabelFromSymbol(name)));
				return result ;
            }
        });
    }
    
    public static List<Class> topologicalSort(List<Class> toSort) {
        toSort = new ArrayList<Class>(toSort);
        Collections.sort(toSort, new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        final Map<String, Class> nameToEntity = new HashMap<String, Class>();
        List<String> sortedRefs = new ArrayList<String>();
        for (Class entity : toSort) {
            sortedRefs.add(entity.getQualifiedName());
            nameToEntity.put(entity.getQualifiedName(), entity);
        }
        NodeSorter.NodeHandler<String> walker = new NodeSorter.NodeHandler<String>() {
            @Override
            public Collection<String> next(String vertex) {
                Collection<String> result = new HashSet<String>();
                Class entity = nameToEntity.get(vertex);
                for (Property rel : getRelationships(entity))
                    if (!isDerived(rel) && isPrimary(rel))
                        result.add(rel.getType().getQualifiedName());
                return result;
            }
        };
        try {
            sortedRefs = NodeSorter.sort(sortedRefs, walker);
        } catch (IllegalArgumentException e) {
            // too bad
        }
        toSort.clear();
        for (String typeRef : sortedRefs)
            toSort.add(0, nameToEntity.get(typeRef));
        return toSort;
    }

    public static boolean isPrimitive(Type umlType) {
        return BasicTypeUtils.isBasicType(umlType);
    }
    
    public static boolean isBlob(Type umlType) {
        return BasicTypeUtils.isBlobType(umlType);
    }
    
    public static boolean isGeolocation(Type type) {
		return type.getName().equals("Geolocation");
	}

    
    public static TypeRef.TypeKind getKind(Type umlType) {
        if (isEnumeration(umlType))
            return TypeKind.Enumeration;
        if (isService(umlType))
            return TypeKind.Service;
        if (isEntity(umlType))
            return TypeKind.Entity;
        if (isTupleType(umlType))
            return TypeKind.Tuple;
        if (isPrimitive(umlType))
            return TypeKind.Primitive;
        if (isBlob(umlType))
            return TypeKind.Blob;
        return null;
    }
    
	public static TypeRef convertType(Type umlType) {
        if (umlType == null)
            return null;
        String mappedTypeName = mapToClientType(umlType.getQualifiedName());
        return new TypeRef(mappedTypeName, getKind(umlType));
    }
    private static String mapToClientType(String typeName) {
        return TypeRef.sanitize(typeName);
    }

	public static boolean hasDefault(Property umlAttribute) {
		return umlAttribute.getDefaultValue() != null;
	}
	
	public static boolean hasDefault(Parameter umlParameter) {
		return umlParameter.getDefaultValue() != null;
	}

	public static boolean isMultiple(Parameter umlParameter) {
		return umlParameter.isMultivalued();
	}
	
	public static boolean isMultiple(Property umlAttribute) {
		return umlAttribute.isMultivalued();
	}

	public static String getAssociationName(org.eclipse.uml2.uml.Property umlAttribute) {
		return umlAttribute.isDerived() ? null : umlAttribute.getAssociation().getName();
	}
	
	public static String getAssociationNamespace(org.eclipse.uml2.uml.Property umlAttribute) {
		return getNamespace(umlAttribute.getAssociation());
	}
	
    public static String getNamespace(org.eclipse.uml2.uml.NamedElement umlClass) {
        return umlClass.getNamespace().getQualifiedName().replace(org.eclipse.uml2.uml.NamedElement.SEPARATOR, ".");
    }


	public static Style getRelationshipStyle(org.eclipse.uml2.uml.Property umlAttribute) {
        Style style;
		if (KirraHelper.isChildRelationship(umlAttribute))
            style = Style.CHILD;
        else if (KirraHelper.isParentRelationship(umlAttribute))
            style = Style.PARENT;
        else
            style = Style.LINK;
		return style;
	}

	public static boolean isInherited(Property feature, Classifier candidateOwner) {
		boolean inherited = feature.getOwner() != candidateOwner && ClassifierUtils.isKindOf(candidateOwner, (Classifier) feature.getOwner());
		return inherited;
	}
	
	public static boolean isInherited(Operation feature, Classifier candidateOwner) {
		boolean inherited = feature.getOwner() != candidateOwner && ClassifierUtils.isKindOf(candidateOwner, (Classifier) feature.getOwner());
		return inherited;
	}
}