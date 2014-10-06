package com.abstratt.mdd.target.pojo;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.BehavioralFeature;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Feature;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.StructuralFeature;
import org.eclipse.uml2.uml.StructuredActivityNode;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.TypedElement;
import org.eclipse.uml2.uml.ValueSpecification;
import org.eclipse.uml2.uml.VisibilityKind;

import com.abstratt.mdd.core.target.IMapper;
import com.abstratt.mdd.core.target.IMappingContext.Style;
import com.abstratt.mdd.core.target.ITopLevelMapper.MapperInfo;
import com.abstratt.mdd.core.target.spi.MapperFinder;
import com.abstratt.mdd.core.target.spi.MappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.StructuralFeatureUtils;
import com.abstratt.mdd.internal.target.pojo.ReadExtentActionMapping;
import com.abstratt.mdd.internal.target.pojo.StructuredActivityNodeMapping;
import com.abstratt.mdd.target.engine.st.ISTLanguageMapper;
import com.abstratt.mdd.target.engine.st.ModelWrapper;
import com.abstratt.mdd.target.engine.st.StringRenderer;
import com.abstratt.mdd.target.engine.st.ModelWrapper.PropertyHandler;

public class POJOMapper implements ISTLanguageMapper {
    public final static String EXTERNAL_STEREOTYPE = "base_profile::external";

    public static final String PLUGIN_ID = POJOMapper.class.getPackage()
            .getName();

    protected List<Package> getBasePackages() {
        return Arrays.asList(StructuredActivityNodeMapping.class.getPackage());
    }

    // public void collectAnnotations(List<AnnotationInfo> annotations, Element
    // element, boolean deep) {
    // if (deep && element.getOwner() != null)
    // collectAnnotations(annotations, element.getOwner(), true);
    // List<Stereotype> applied = element.getAppliedStereotypes();
    // for (Stereotype stereotype : applied) {
    // Map<String, Object> values = new HashMap<String, Object>();
    // List<Property> attributes = stereotype.getAllAttributes();
    // for (Property property : attributes)
    // values.put(property.getName(), element.getValue(stereotype,
    // property.getName()));
    // annotations.add(new AnnotationInfo(stereotype, values));
    // }
    // }

    public Operation getEntryPoint(org.eclipse.uml2.uml.Class class_) {
        List<Operation> operations = class_.getAllOperations();
        for (Operation operation : operations)
            if (null != operation
                    .getAppliedStereotype("base_profile::entryPoint"))
                return operation;
        return null;
    }

    public String getInitialization(Property property) {
        ValueSpecification defaultValue = property.getDefaultValue();
        if (defaultValue == null)
            return "";
        return " = " + defaultValue.stringValue();
    }

    public List<Parameter> getInputParameters(Operation operation) {
        return StructuralFeatureUtils.filterParameters(
                operation.getOwnedParameters(),
                ParameterDirectionKind.IN_LITERAL);
    }

    public List<String> getModifiers(NamedElement element) {
        List<String> modifiers = new ArrayList<String>();
        modifiers.add(element.getVisibility().getName());
        if (element instanceof Feature) {
            if (((Feature) element).isStatic())
                modifiers.add("static");
            if (element instanceof StructuralFeature
                    && ((StructuralFeature) element).isReadOnly())
                modifiers.add("final");
            else if (element instanceof BehavioralFeature) {
                BehavioralFeature behavioralFeature = (BehavioralFeature) element;
                if (behavioralFeature.getMethods().isEmpty()
                        || behavioralFeature.isAbstract())
                    modifiers.add("abstract");
            }
        }
        return modifiers;
    }

    public Set<Association> getNavigableAssociations(
            org.eclipse.uml2.uml.Classifier classifier) {
        Set<Association> navigableAssociations = new HashSet<Association>();
        List<Association> allAssociations = classifier.getAssociations();
        for (Association association : allAssociations)
            if (association.getNavigableOwnedEnd(null, classifier) != null)
                navigableAssociations.add(association);
        return navigableAssociations;
    }

    public Parameter getReturnParameter(Operation operation) {
        List<Parameter> returnPar = StructuralFeatureUtils.filterParameters(
                operation.getOwnedParameters(),
                ParameterDirectionKind.RETURN_LITERAL);
        return returnPar.isEmpty() ? null : returnPar.get(0);
    }

    public String mapBehavior(Operation operation) {
        // List<IOperationMapper> mappers = new ArrayList<IOperationMapper>();
        // List<Stereotype> applied = operation.getAppliedStereotypes();
        // for (Stereotype stereotype : applied)
        // mappers.addAll(OperationMappingManager.getInstance().getMappers(stereotype.getQualifiedName()));
        // // add itself as the last (default) mapper
        // mappers.add(this);
        // // invoke the first mapper (which might as well be itself)
        // return mappers.get(0).mapBehavior(operation, mappers.subList(1,
        // mappers.size()));
        List<Behavior> methods = operation.getMethods();
        if (methods.isEmpty())
            return "";
        return mapBehavior(operation, (Activity) operation.getMethods().get(0),
                Style.STATEMENT);
    }

    /**
     * Maps the given activity.
     * 
     * @return the resulting text
     */
    public String mapBehavior(Operation operation, Activity behavior,
            Style style) {
        StructuredActivityNode body = (StructuredActivityNode) behavior
                .getNodes().get(0);
        MappingContext mappingContext = new MappingContext(this, style,
                new MapperFinder(ReadExtentActionMapping.class));
        StructuredActivityNode rootAction = (StructuredActivityNode) body
                .getNodes().get(0);
        return mappingContext.map(rootAction);
    }

    public String mapQualifiedName(String qualifiedName) {
        return POJOMappingUtils.mapQualifiedName(qualifiedName);
    }

    public String mapTypedElementType(TypedElement element, boolean specific) {
        return POJOMappingUtils.mapTypedElementType(element, specific);
    }

    public String mapTypeReference(Type type) {
        return POJOMappingUtils.mapTypeReference(type);
    }

    @Override
    public String map(Classifier toMap) {
        // TODO pretty hacky, copied as is from an experiment
        StringTemplateGroup group = loadTemplateGroup();
        group.registerRenderer(String.class, new StringRenderer());
        StringTemplate template = group.lookupTemplate("topLevel");
        Map<Class<?>, ModelWrapper.PropertyHandler<?>> handlers = new HashMap<Class<?>, ModelWrapper.PropertyHandler<?>>();
        registerHandlers(handlers);

        template.setAttribute("class",
                ModelWrapper.wrapModelObject(toMap, handlers));
        return template.toString();
    }

    public void registerHandlers(
            Map<Class<?>, ModelWrapper.PropertyHandler<?>> handlers) {
        handlers.put(NamedElement.class, new PropertyHandler<NamedElement>() {
            public Object getProperty(NamedElement target, String propertyName) {
                if ("public".equals(propertyName))
                    return target.getVisibility() == VisibilityKind.PUBLIC_LITERAL;
                return null;
            }
        });
        handlers.put(Property.class, new PropertyHandler<Property>() {
            public Object getProperty(Property target, String propertyName) {
                if ("method".equals(propertyName)) {
                    Constraint derivation = StructuralFeatureUtils
                            .getDerivation(target);
                    if (derivation == null)
                        return null;
                    Activity behavior = (Activity) ActivityUtils
                            .resolveBehaviorReference(derivation
                                    .getSpecification());
                    return POJOMapper.this.mapBehavior(null, behavior,
                            Style.STATEMENT);
                }
                return null;
            }
        });

        handlers.put(ValueSpecification.class,
                new PropertyHandler<ValueSpecification>() {
                    public Object getProperty(ValueSpecification target,
                            String propertyName) {
                        if ("stringValue".equals(propertyName))
                            return POJOMappingUtils.mapValue(target);
                        return null;
                    }
                });

        // if we wanted to deal with types here...
        // handlers.put(TypedElement.class, new PropertyHandler<TypedElement>()
        // {
        // public Object getProperty(TypedElement target, String propertyName) {
        // if ("specificType".equals(propertyName))
        // return POJOMappingUtils.mapTypedElementType(target, true);
        // return null;
        // }
        // });

        handlers.put(org.eclipse.uml2.uml.Class.class,
                new PropertyHandler<org.eclipse.uml2.uml.Class>() {
                    public Object getProperty(
                            org.eclipse.uml2.uml.Class target,
                            String propertyName) {
                        if ("javaImportedTypes".equals(propertyName))
                            return collectPackageImports(target);
                        if ("javaAnnotations".equals(propertyName))
                            return collectAnnotations(target);
                        return null;
                    }

                });

        handlers.put(Parameter.class, new PropertyHandler<Parameter>() {
            public Object getProperty(Parameter target, String propertyName) {
                if ("return".equals(propertyName))
                    return target.getDirection() == ParameterDirectionKind.RETURN_LITERAL;
                return null;
            }
        });

        handlers.put(Operation.class, new PropertyHandler<Operation>() {
            public Object getProperty(Operation target, String propertyName) {
                if ("method".equals(propertyName)) {
                    return POJOMapper.this.mapBehavior(target);
                }
                return null;
            }
        });
    }

    protected Collection<String> collectPackageImports(
            org.eclipse.uml2.uml.Class target) {
        return POJOMappingUtils.collectImportedTypes(target);
    }

    protected Collection<AnnotationInfo> collectAnnotations(
            org.eclipse.uml2.uml.Class target) {
        return new ArrayList<AnnotationInfo>();
    }

    private StringTemplateGroup loadTemplateGroup() {
        return new StringTemplateGroup(new InputStreamReader(
                POJOMapper.class.getResourceAsStream("pojo.stg")));
    }

    @Override
    public String mapFileName(Classifier toMap) {
        return toMap.getQualifiedName().replace(NamedElement.SEPARATOR, "/")
                + ".java";
    }

    public String applyChildMapper(String mapperName, Element element) {
        throw new UnsupportedOperationException();
    }
    
    public Collection<String> getChildMappers() {
        return Collections.emptyList();
    }
    public MapperInfo describeChildMapper(String mapperName) {
        throw new UnsupportedOperationException();
    }

}