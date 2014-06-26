package com.abstratt.kirra.mdd.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.BehavioredClassifier;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.Vertex;
import org.eclipse.uml2.uml.VisibilityKind;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.NamedElement;
import com.abstratt.kirra.Namespace;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Operation.OperationKind;
import com.abstratt.kirra.Parameter;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Relationship.Style;
import com.abstratt.kirra.Schema;
import com.abstratt.kirra.SchemaBuilder;
import com.abstratt.kirra.Service;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.core.util.StateMachineUtils;

/**
 * Builds Kirra schema elements based on UML elements.
 */
public class KirraMDDSchemaBuilder implements SchemaBuildingOnUML, SchemaBuilder {
    private static String mapToClientType(String typeName) {
        return TypeRef.sanitize(typeName);
    }

    @Override
    public Schema build() {
        IRepository repository = RepositoryService.DEFAULT.getFeature(IRepository.class);
        Collection<Package> applicationPackages = KirraHelper.getApplicationPackages(repository.getTopLevelPackages(null));
        List<Namespace> namespaces = new ArrayList<Namespace>();
        for (Package package1 : applicationPackages) {
            List<Entity> entities = new ArrayList<Entity>();
            List<Service> services = new ArrayList<Service>();
            List<TupleType> tupleTypes = new ArrayList<TupleType>();
            for (Type type : package1.getOwnedTypes())
                if (KirraHelper.isEntity(type))
                    entities.add(getEntity((Class) type));
                else if (KirraHelper.isService(type))
                    services.add(getService((BehavioredClassifier) type));
                else if (KirraHelper.isTupleType(type))
                    tupleTypes.add(getTupleType((Classifier) type));
            if (!entities.isEmpty() || !services.isEmpty() || !tupleTypes.isEmpty()) {
                Namespace namespace = new Namespace(KirraHelper.getName(package1));
                namespace.setLabel(KirraHelper.getLabel(package1));
                namespace.setDescription(KirraHelper.getDescription(package1));
                namespace.setTimestamp(MDDUtil.getGeneratedTimestamp(package1));
                namespace.setEntities(entities);
                namespace.setServices(services);
                namespace.setTupleTypes(tupleTypes);
                namespaces.add(namespace);
            }
        }
        Schema schema = new Schema();
        schema.setNamespaces(namespaces);
        if (!namespaces.isEmpty()) {
            Namespace first = namespaces.get(0);
            schema.setBuild(first.getTimestamp());
            schema.setApplicationName(first.getLabel());
        }
        return schema;
    }

    @Override
    public Entity getEntity(Class umlClass) {
        if (!KirraHelper.isEntity(umlClass))
            throw new KirraException(umlClass.getName() + " is not an entity", null, KirraException.Kind.SCHEMA);
        final Entity entity = new Entity();
        entity.setNamespace(getNamespace(umlClass));
        setName(umlClass, entity);
        entity.setProperties(getEntityProperties(umlClass));
        entity.setOperations(getEntityOperations(umlClass));
        entity.setRelationships(getEntityRelationships(umlClass));
        entity.setConcrete(KirraHelper.isConcrete(umlClass));
        entity.setInstantiable(KirraHelper.isInstantiable(umlClass));
        entity.setTopLevel(KirraHelper.isTopLevel(umlClass));
        entity.setStandalone(KirraHelper.isStandalone(umlClass));
        entity.setUser(KirraHelper.isUser(umlClass));
        return entity;
    }

    @Override
    public Operation getEntityOperation(org.eclipse.uml2.uml.Operation umlOperation) {
        if (!KirraHelper.isEntityOperation(umlOperation))
            throw new IllegalArgumentException();
        if (!KirraHelper.isAction(umlOperation) && !KirraHelper.isFinder(umlOperation))
            throw new IllegalArgumentException();

        Operation entityOperation = basicGetOperation(umlOperation);
        entityOperation.setKind(KirraHelper.isFinder(umlOperation) ? Operation.OperationKind.Finder : Operation.OperationKind.Action);
        entityOperation.setInstanceOperation(entityOperation.getKind() == OperationKind.Action && !umlOperation.isStatic());
        return entityOperation;
    }

    @Override
    public Property getEntityProperty(org.eclipse.uml2.uml.Property umlAttribute) {
        Property entityProperty = new Property();
        setName(umlAttribute, entityProperty);
        entityProperty.setRequired(KirraHelper.isRequired(umlAttribute));
        entityProperty.setMultiple(umlAttribute.isMultivalued());
        entityProperty.setHasDefault(umlAttribute.getDefaultValue() != null);
        entityProperty.setInitializable(KirraHelper.isInitializable(umlAttribute));
        entityProperty.setEditable(KirraHelper.isEditable(umlAttribute));
        Type umlType = umlAttribute.getType();
        setTypeInfo(entityProperty, umlType);
        entityProperty.setDerived(KirraHelper.isDerived(umlAttribute));
        entityProperty.setUnique(KirraHelper.isUnique(umlAttribute));
        entityProperty.setUserVisible(KirraHelper.isUserVisible(umlAttribute));
        return entityProperty;
    }

    @Override
    public Relationship getEntityRelationship(org.eclipse.uml2.uml.Property umlAttribute) {
        Relationship entityRelationship = new Relationship();
        setName(umlAttribute, entityRelationship);

        org.eclipse.uml2.uml.Property otherEnd = umlAttribute.getOtherEnd();
        if (otherEnd != null && KirraHelper.isRelationship(otherEnd))
            entityRelationship.setOpposite(otherEnd.getName());

        Style style;

        if (KirraHelper.isChildRelationship(umlAttribute))
            style = Style.CHILD;
        else if (KirraHelper.isParentRelationship(umlAttribute))
            style = Style.PARENT;
        else
            style = Style.LINK;

        entityRelationship.setStyle(style);
        entityRelationship.setAssociationName(umlAttribute.isDerived() ? null : umlAttribute.getAssociation().getName());
        entityRelationship.setPrimary(KirraHelper.isPrimary(umlAttribute));
        entityRelationship.setNavigable(umlAttribute.isNavigable());
        entityRelationship.setRequired(!umlAttribute.isDerived() && umlAttribute.getLower() > 0);
        entityRelationship.setHasDefault(umlAttribute.getDefaultValue() != null);
        entityRelationship.setInitializable(KirraHelper.isInitializable(umlAttribute));
        entityRelationship.setEditable(KirraHelper.isEditable(umlAttribute));
        entityRelationship.setMultiple(umlAttribute.isMultivalued());
        setTypeInfo(entityRelationship, umlAttribute.getType());
        entityRelationship.setDerived(KirraHelper.isDerived(umlAttribute));
        entityRelationship.setVisible(umlAttribute.isNavigable()
                && (umlAttribute.getOtherEnd() == null || umlAttribute.getOtherEnd().getAggregation() == AggregationKind.NONE_LITERAL));
        return entityRelationship;
    }

    @Override
    public List<Relationship> getEntityRelationships(Class modelClass) {
        List<Relationship> entityRelationships = new ArrayList<Relationship>();
        for (org.eclipse.uml2.uml.Property attribute : KirraHelper.getRelationships(modelClass))
            entityRelationships.add(getEntityRelationship(attribute));
        return entityRelationships;
    }

    @Override
    public String getNamespace(org.eclipse.uml2.uml.NamedElement umlClass) {
        return SchemaManagementOperations.getNamespace(umlClass);
    }

    @Override
    public Service getService(BehavioredClassifier serviceClassifier) {
        final Service service = new Service();
        setName(serviceClassifier, service);
        service.setNamespace(getNamespace(serviceClassifier));
        service.setOperations(getServiceOperations(serviceClassifier));
        return service;
    }

    @Override
    public Operation getServiceOperation(org.eclipse.uml2.uml.BehavioralFeature umlOperation) {
        if (!KirraHelper.isServiceOperation(umlOperation))
            throw new IllegalArgumentException();
        Operation serviceOperation = basicGetOperation(umlOperation);
        serviceOperation.setKind(umlOperation instanceof org.eclipse.uml2.uml.Operation ? Operation.OperationKind.Retriever
                : Operation.OperationKind.Event);
        serviceOperation.setInstanceOperation(false);
        return serviceOperation;
    }

    @Override
    public TupleType getTupleType(Classifier umlClass) {
        if (!KirraHelper.isTupleType(umlClass))
            throw new KirraException(umlClass.getName() + " is not a tuple type", null, KirraException.Kind.SCHEMA);
        final TupleType tupleType = new TupleType();
        tupleType.setNamespace(getNamespace(umlClass));
        setName(umlClass, tupleType);
        // piggyback on entity properties for now
        tupleType.setProperties(getEntityProperties(umlClass));
        return tupleType;
    }

    List<Operation> getEntityOperations(Class umlClass) {
        List<Operation> entityOperations = new ArrayList<Operation>();
        for (org.eclipse.uml2.uml.Operation operation : umlClass.getAllOperations())
            if (operation.getVisibility() == VisibilityKind.PUBLIC_LITERAL && KirraHelper.isEntityOperation(operation))
                entityOperations.add(getEntityOperation(operation));
        return entityOperations;
    }

    List<Property> getEntityProperties(Classifier umlClass) {
        List<Property> entityProperties = new ArrayList<Property>();
        for (org.eclipse.uml2.uml.Property attribute : KirraHelper.getProperties(umlClass))
            entityProperties.add(getEntityProperty(attribute));
        return entityProperties;
    }

    List<Operation> getServiceOperations(BehavioredClassifier serviceClass) {
        List<Operation> entityOperations = new ArrayList<Operation>();
        for (org.eclipse.uml2.uml.Interface provided : serviceClass.getImplementedInterfaces()) {
            for (org.eclipse.uml2.uml.Operation operation : provided.getAllOperations())
                if (operation.getVisibility() == VisibilityKind.PUBLIC_LITERAL && KirraHelper.isServiceOperation(operation))
                    entityOperations.add(getServiceOperation(operation));
            for (org.eclipse.uml2.uml.Reception reception : provided.getOwnedReceptions())
                if (reception.getVisibility() == VisibilityKind.PUBLIC_LITERAL && KirraHelper.isServiceOperation(reception))
                    entityOperations.add(getServiceOperation(reception));
        }
        return entityOperations;
    }

    private Operation basicGetOperation(org.eclipse.uml2.uml.BehavioralFeature umlOperation) {
        Operation basicOperation = new Operation();
        setName(umlOperation, basicOperation);
        if (umlOperation instanceof org.eclipse.uml2.uml.Operation)
            setTypeInfo(basicOperation, ((org.eclipse.uml2.uml.Operation) umlOperation).getType());

        basicOperation.setOwner(convertType((Type) umlOperation.getOwner()));
        List<Parameter> entityOperationParameters = new ArrayList<Parameter>();
        for (org.eclipse.uml2.uml.Parameter parameter : KirraHelper.getParameters(umlOperation)) {
            final Parameter entityOperationParameter = new Parameter();
            entityOperationParameter.setOwner(basicOperation.getOwner());
            setName(parameter, entityOperationParameter);
            entityOperationParameter.setRequired(parameter.getLower() > 0);
            entityOperationParameter.setMultiple(parameter.isMultivalued());
            setTypeInfo(entityOperationParameter, parameter.getType());
            entityOperationParameters.add(entityOperationParameter);
        }
        basicOperation.setParameters(entityOperationParameters);
        return basicOperation;
    }

    private TypeRef convertType(Type umlType) {
        if (umlType == null)
            return null;
        String mappedTypeName = KirraMDDSchemaBuilder.mapToClientType(umlType.getQualifiedName());
        return new TypeRef(mappedTypeName, getKind(umlType));
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

    private void setName(org.eclipse.uml2.uml.NamedElement sourceElement, NamedElement<?> targetElement) {
        targetElement.setName(KirraHelper.getName(sourceElement));
        targetElement.setSymbol(KirraHelper.getSymbol(sourceElement));
        targetElement.setLabel(KirraHelper.getLabel(sourceElement));
        targetElement.setDescription(KirraHelper.getDescription(sourceElement));
    }

    private void setTypeInfo(com.abstratt.kirra.TypedElement<?> typedElement, Type umlType) {
        if (umlType instanceof Enumeration) {
            Enumeration umlEnum = (Enumeration) umlType;
            EList<EnumerationLiteral> umlLiterals = umlEnum.getOwnedLiterals();
            List<String> enumValues = new ArrayList<String>(umlLiterals.size());
            for (EnumerationLiteral literal : umlLiterals)
                enumValues.add(literal.getName());
            typedElement.setEnumerationLiterals(enumValues);
        } else if (umlType instanceof StateMachine) {
            List<String> enumValues = new ArrayList<String>();
            for (Vertex literal : StateMachineUtils.getVertices((StateMachine) umlType))
                enumValues.add(literal.getName());
            typedElement.setEnumerationLiterals(enumValues);
        }
        typedElement.setTypeRef(convertType(umlType));
    }
}
