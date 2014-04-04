package com.abstratt.kirra.mdd.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.BehavioredClassifier;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPackage.Literals;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.NamedElement;
import com.abstratt.kirra.Namespace;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Schema;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.Service;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.NamedElementLookupCache;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.runtime.RuntimeClass;
import com.abstratt.mdd.core.runtime.RuntimeObject;

public class SchemaManagementOnMDDRepository implements SchemaManagement {

    private static Log log = LogFactory.getLog(SchemaManagementOnMDDRepository.class);

    private SchemaBuilder schemaBuilder = new SchemaBuilder();

	private NamedElementLookupCache lookup;

	private IRepository repository;
    
    public SchemaManagementOnMDDRepository() {
    	this.repository = RepositoryService.DEFAULT.getCurrentResource().getFeature(IRepository.class);
    	this.lookup = RepositoryService.DEFAULT.getCurrentResource().getFeature(NamedElementLookupCache.class);    	
    }
    
    protected TypeRef convertType(Type umlType) {
        if (umlType == null)
            return null;
        String mappedTypeName = mapToClientType(umlType.getQualifiedName());
		return new TypeRef(mappedTypeName, getKind(umlType));
    }
    
	private static String mapToClientType(String typeName) {
	    return typeName.replace("::", TypeRef.SEPARATOR);
	}


    private TypeKind getKind(Type umlType) {
    	if (umlType instanceof Enumeration || umlType instanceof StateMachine)
    		return TypeKind.Enumeration;
    	if (KirraHelper.isService(umlType))
    		return TypeKind.Service;
    	if (KirraHelper.isEntity(umlType))
    		return TypeKind.Entity;
    	if (KirraHelper.isTupleType(umlType))
    		return TypeKind.Tuple;
    	return TypeKind.Primitive;
	}
	@Override
    public List<Entity> getAllEntities() {
        return getEntities(new EObjectCondition() {
            @Override
            public boolean isSatisfied(EObject eObject) {
                if (!UMLPackage.Literals.CLASS.isInstance(eObject))
                    return false;
                Class clazz = (Class) eObject;
                return KirraHelper.isEntity(clazz);
            }
        });
    }
	
@Override
public List<Service> getAllServices() {
    return getServices(new EObjectCondition() {
        @Override
        public boolean isSatisfied(EObject eObject) {
            if (!UMLPackage.Literals.CLASS.isInstance(eObject))
                return false;
            Class clazz = (Class) eObject;
            return KirraHelper.isService(clazz);
        }
    });
}

@Override
public List<TupleType> getAllTupleTypes() {
    return getTupleTypes(new EObjectCondition() {
        @Override
        public boolean isSatisfied(EObject eObject) {
            if (!UMLPackage.Literals.CLASS.isInstance(eObject))
                return false;
            Class clazz = (Class) eObject;
            return KirraHelper.isTupleType(clazz);
        }
    });
}
	
	@Override
    public List<Entity> getEntities(final String namespace) {
        return getEntities(new EObjectCondition() {
            @Override
            public boolean isSatisfied(EObject eObject) {
                if (!UMLPackage.Literals.CLASS.isInstance(eObject))
                    return false;
                Class clazz = (Class) eObject;
                return KirraHelper.isEntity(clazz) && (namespace == null || namespace.equals(clazz.getNamespace().getQualifiedName()));
            }
        });
    }
	@Override
    public List<Service> getServices(final String namespace) {
        return getServices(getServiceFilter(namespace));
    }
	
	private EObjectCondition getServiceFilter(final String namespace) {
		return new EObjectCondition() {
            @Override
            public boolean isSatisfied(EObject eObject) {
                if (!UMLPackage.Literals.CLASSIFIER.isInstance(eObject))
                    return false;
                Classifier classifier = (Classifier) eObject;
                return KirraHelper.isService(classifier) && (namespace == null || namespace.equals(classifier.getNamespace().getQualifiedName()));
            }
        };
	}
	@Override
    public List<TupleType> getTupleTypes(final String namespace) {
        return getTupleTypes(new EObjectCondition() {
            @Override
            public boolean isSatisfied(EObject eObject) {
                if (!UMLPackage.Literals.CLASSIFIER.isInstance(eObject))
                    return false;
                Classifier classifier = (Classifier) eObject;
                return KirraHelper.isTupleType(classifier) && (namespace == null || namespace.equals(classifier.getNamespace().getQualifiedName()));
            }
        });
    }
	private List<TupleType> getTupleTypes(EObjectCondition condition) {
		log.debug(getRepository().getBaseURI().toString());
		List<Element> allClasses = findInApplication(condition);
        List<TupleType> services = new ArrayList<TupleType>();
        for (Element element : allClasses)
            services.add(schemaBuilder.getTupleType((Classifier) element));
        sortNamedElements(services);
        return services;
	}

	private List<Element> findInApplication(EObjectCondition condition) {
		return getRepository().findInAnyPackage(condition);
	}
	private List<Entity> getEntities(EObjectCondition condition) {
		log.debug(getRepository().getBaseURI().toString());
		List<Element> allClasses = findInApplication(condition);
        List<Entity> entities = new ArrayList<Entity>();
        for (Element element : allClasses)
            entities.add(schemaBuilder.getEntity((Class) element));
        sortNamedElements(entities);
        return entities;
	}
	private List<Service> getServices(EObjectCondition condition) {
		log.debug(getRepository().getBaseURI().toString());
		List<Element> allClasses = findInApplication(condition);
        List<Service> services = new ArrayList<Service>();
        for (Element element : allClasses)
            services.add(schemaBuilder.getService((BehavioredClassifier) element));
        sortNamedElements(services);
        return services;
	}
	
	@Override
	public Collection<TypeRef> getEntityNames() {
        log.debug(getRepository().getBaseURI().toString());
        final Collection<TypeRef> entityNames = new TreeSet<TypeRef>();
        findInApplication(new EObjectCondition() {
            @Override
            public boolean isSatisfied(EObject eObject) {
                if (!UMLPackage.Literals.CLASS.isInstance(eObject))
                    return false;
                Class clazz = (Class) eObject;
                if (KirraHelper.isEntity(clazz))
                	entityNames.add(convertType(clazz));
                return false;
            }
        });
        return entityNames;
	}

    @Override
    public final Entity getEntity(String namespace, String name) {
        log.debug(getRepository().getBaseURI().toString());
        Class clazz = (Class) getModelClass(namespace, name, UMLPackage.Literals.CLASS);
        return schemaBuilder.getEntity(clazz);
    }
    @Override
    public TupleType getTupleType(String namespace, String name) {
        log.debug(getRepository().getBaseURI().toString());
        Classifier clazz = getModelElement(namespace, name, UMLPackage.Literals.CLASSIFIER);
        return schemaBuilder.getTupleType(clazz);
    }
    @Override
    public Service getService(String namespace, String name) {
        log.debug(getRepository().getBaseURI().toString());
        Class serviceClass = getModelElement(namespace, name, UMLPackage.Literals.CLASSIFIER);
        return schemaBuilder.getService(serviceClass);
    }
    @Override
    public Entity getEntity(TypeRef typeRef) {
    	return getEntity(typeRef.getEntityNamespace(), typeRef.getTypeName());
    }
    
    @Override
    public Service getService(TypeRef typeRef) {
    	return getService(typeRef.getEntityNamespace(), typeRef.getTypeName());
    }
    @Override
    public TupleType getTupleType(TypeRef typeRef) {
    	return getTupleType(typeRef.getEntityNamespace(), typeRef.getTypeName());
    }

    @Override
    public List<Operation> getEntityOperations(String namespace, String name) {
        log.debug(getRepository().getBaseURI().toString());
        Class modelClass = (Class) getModelClass(namespace, name, UMLPackage.Literals.CLASS);
        if (modelClass == null)
            return Collections.<Operation> emptyList();
        return schemaBuilder.getEntityOperations(modelClass);
    }

    @Override
    public List<Property> getEntityProperties(String namespace, String name) {
        log.debug(getRepository().getBaseURI().toString());
        Class modelClass = (Class) getModelClass(namespace, name, UMLPackage.Literals.CLASS);
        if (modelClass == null)
            return Collections.<Property> emptyList();
        return schemaBuilder.getEntityProperties(modelClass);
    }

    @Override
    public List<Relationship> getEntityRelationships(String namespace, String name) {
        log.debug(getRepository().getBaseURI().toString());
        Class modelClass = (Class) getModelClass(namespace, name, UMLPackage.Literals.CLASS);
        if (modelClass == null)
            return Collections.<Relationship> emptyList();
        return schemaBuilder.getEntityRelationships(modelClass);
    }

	public Classifier getModelClass(String entityNamespace, String entityName, EClass eClass) {
        return getModelElement(entityNamespace, entityName, eClass);
    }
    
	public <NE extends org.eclipse.uml2.uml.NamedElement> NE getModelElement(String namespace, String name, EClass elementClass) {
		return lookup.find(SchemaManagementOperations.getQualifiedName(namespace, name), elementClass);
    }
	
    @Override
    public List<String> getNamespaces() {
        log.debug(getRepository().getBaseURI().toString());
        List<String> packageNames = new ArrayList<String>();
        for (Element current : getApplicationPackages())
           packageNames.add(((org.eclipse.uml2.uml.Package) current).getQualifiedName());
        return packageNames;
    }
    
	private Collection<Package> getApplicationPackages() {
		return KirraHelper.getApplicationPackages(getRepository().getTopLevelPackages(null));
	}

    private IRepository getRepository() {
    	return repository;
    }
    
    private NamedElementLookupCache getLookup() {
    	return lookup;
    }

    @Override
    public Schema getSchema() {
    	List<String> namespaceNames = getNamespaces();
        log.debug(getRepository().getBaseURI().toString());
        for (ListIterator<String> li = namespaceNames.listIterator(); li.hasNext();) {
            String current = li.next();
            if (getRepository().findPackage(current, Literals.PACKAGE) == null || getEntities(current).isEmpty())
                li.remove();
        }
        if (namespaceNames.isEmpty())
            namespaceNames = getNamespaces();
        List<Namespace> namespaces = new ArrayList<Namespace>();
        for (String namespaceName : namespaceNames)
			if (getRepository().findPackage(namespaceName, Literals.PACKAGE) != null)
				namespaces.add(getNamespace(namespaceName));
        Schema schema = new Schema();
        schema.setNamespaces(namespaces);
        return schema;
    }

    @Override
	public Namespace getNamespace(String namespaceName) {
		Namespace namespace = new Namespace(namespaceName);
		namespace.setEntities(getEntities(namespaceName));
		namespace.setServices(getServices(namespaceName));
		namespace.setTupleTypes(getTupleTypes(namespaceName));
		return namespace;
	}

    @Override
    public List<Entity> getTopLevelEntities(String namespace) {
        return getEntities(namespace);
    }
    
    protected void sortNamedElements(List<? extends NamedElement> entities) {
        Collections.sort(entities, new Comparator<NamedElement>() {
            public int compare(NamedElement o1, NamedElement o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }
    
	@Override
	public Relationship getOpposite(Relationship relationship) {
		return SchemaManagementOperations.getOpposite(this, relationship);
	}
}
