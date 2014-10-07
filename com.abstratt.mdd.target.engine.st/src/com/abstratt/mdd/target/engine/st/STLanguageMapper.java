package com.abstratt.mdd.target.engine.st;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplateWriter;
import org.antlr.stringtemplate.language.FormalArgument;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.MultiplicityElement;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ObjectNode;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.StructuredActivityNode;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.Variable;
import org.eclipse.uml2.uml.VisibilityKind;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.target.spi.ActionMappingUtils;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.core.util.StructuralFeatureUtils;
import com.abstratt.mdd.target.engine.st.ModelWrapper.PropertyHandler;
import com.abstratt.mdd.target.query.QueryCore;
import com.abstratt.pluginutils.LogUtils;

public class STLanguageMapper implements ITopLevelMapper {
    private static Pattern stereotypePattern = Pattern.compile("as(.+)");
    private URI templateUri;

    private Map<Activity, Map<Variable, String>> suggestedVariableNames = new HashMap<Activity, Map<Variable, String>>();
    private List<URI> baseURIs;

    static class StereotypeApplication {
        private Stereotype stereotype;
        private Element element;

        public StereotypeApplication(Stereotype stereotype, Element element) {
            this.stereotype = stereotype;
            this.element = element;
        }

        public Object getValue(String propertyName) {
            return element.getValue(stereotype, propertyName);
        }
    }

    public STLanguageMapper(Map<String, String> properties, URI baseURI) {
        try {
            String templateLocation = properties.get("template");
            if (templateLocation == null)
                templateLocation = "";
            this.templateUri = URIUtil.append(baseURI, templateLocation.trim());
            String imported = properties.get(IRepository.IMPORTED_PROJECTS);
            this.baseURIs = new ArrayList<URI>();
            baseURIs.add(makeDirURI(baseURI));
            if (imported != null) {
                String[] importedURIs = imported.split(",");
                for (String uri : importedURIs)
                    baseURIs.add(makeDirURI(URI.create(uri)));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private URI makeDirURI(URI baseURI) throws URISyntaxException {
        if (!baseURI.getPath().endsWith("/"))
            return new URI(baseURI.getScheme(), baseURI.getUserInfo(), baseURI.getHost(), baseURI.getPort(), baseURI.getPath() + "/", baseURI.getQuery(), baseURI.getFragment());
        return baseURI;
    }
    
    @Override
    public String applyChildMapper(String mapperName, Element element) {
        return applyMap(element, mapperName, "element", false);
    }
    
    public MapperInfo describeChildMapper(final String mapperName) {
        return perform(new MappingOperation<MapperInfo>() {
            @Override
            public MapperInfo map(StringTemplateGroup templateGroup) {
                StringTemplate definition = templateGroup.getTemplateDefinition(mapperName);
                return new MapperInfo(definition.getGroup().getName() + ".stg", definition.getName(), definition.getGroupFileLine());
            }
        });
    }
    
    @Override
    public Map<String, String> getFormalArgs(final String mapperName) {
        return perform(new MappingOperation<Map<String, String>>() {
            @Override
            public Map<String, String> map(StringTemplateGroup templateGroup) {
                StringTemplate definition = templateGroup.getTemplateDefinition(mapperName);
                Collection<FormalArgument> internalFormalArgs = definition.getFormalArguments().values();
                Map<String, String> formalArgs = new HashMap<String, String>();
                for (FormalArgument internalArg : internalFormalArgs)
                    formalArgs.put(internalArg.name, internalArg.defaultValueST == null ? null : internalArg.defaultValueST.toString());
                return formalArgs;
            }
        });
    }

    @Override
    public String map(Classifier toMap) {
        return applyMap(toMap, "contents", "class", true);
    }

    @Override
    public Collection<String> getChildMappers() {
        return perform(new MappingOperation<Collection<String>>() {
           @Override
            public Collection<String> map(StringTemplateGroup templateGroup) {
                return templateGroup.getTemplateNames();
            } 
        });
    }
    
    interface MappingOperation<T> {
        public T map(StringTemplateGroup templateGroup);
    }
    
    private <T> T perform(MappingOperation<T> operation) {
        String groupFileName = new File(templateUri.getPath()).getName();
        STEGroupLoader.registerBaseURIs(baseURIs.toArray(new URI[0]));
        try {
            StringTemplateGroup template = STEGroupLoader.getInstance()
                    .loadGroup(groupFileName);
            return operation.map(template);
        } finally {
            STEGroupLoader.clearBaseURI();
        }
    }

    private String applyMap(final Element toMap, final String templateName,
            final String templateParameterName, final boolean mustMatch) {
        try {
            return perform(new MappingOperation<String>() {
                public String map(StringTemplateGroup templateGroup) {
                    if (!templateGroup.getTemplateNames().contains(templateName))
                        return "## no " + templateName + " template found in "
                                + new File(templateUri.getPath()).getName();
                    // enables StringUtils functions as format expressions
                    templateGroup.registerRenderer(String.class, new StringRenderer());
                    StringTemplate contentTemplate = templateGroup
                            .getInstanceOf(templateName);
                    Map<Class<?>, PropertyHandler<?>> handlers = createHandlers();
                    if (mustMatch && templateGroup.getTemplateNames().contains("match")) {
                        StringTemplate matchTemplate = templateGroup.getInstanceOf("match");
                        matchTemplate.setAttribute(templateParameterName,
                                ModelWrapper.wrapModelObject(toMap, handlers));
                        String match = matchTemplate.toString(StringTemplateWriter.NO_WRAP);
                        if (!Boolean.parseBoolean(match))
                            return null;
                    }
                    contentTemplate.setAttribute(templateParameterName, ModelWrapper.wrapModelObject(toMap, handlers));
                    return contentTemplate.toString(StringTemplateWriter.NO_WRAP);
                }
            });
        } catch (StackOverflowError e) {
            LogUtils.logError(STLanguageMapper.class.getPackage().getName(), null, e);
            String errorMessage = "Infinite recursion evaluating template \"" + templateName + "\"";
            if (toMap != null)
                errorMessage += " with element " + (toMap instanceof NamedElement  ? ((NamedElement) toMap).getQualifiedName() : "unnamed") + " : " + toMap.eClass().getName();
            return errorMessage;
        } catch (STException e) {
            return e.getMessage() != null ? e.getMessage()
                    : e.getCause() != null ? e.getCause().getMessage() : e
                            .toString();
        }
    }

    private Map<java.lang.Class<?>, ModelWrapper.PropertyHandler<?>> createHandlers() {
        Map<java.lang.Class<?>, ModelWrapper.PropertyHandler<?>> handlers = new HashMap<Class<?>, ModelWrapper.PropertyHandler<?>>(); 
        handlers.put(Element.class, new PropertyHandler<Element>() {
            @Override
            public Object getProperty(Element target, String propertyName) {
                if ("metaClass".equals(propertyName))
                    return target.eClass().getName();
                Matcher matcher = stereotypePattern.matcher(propertyName);
                if (matcher.matches()) {
                    String stereotypeName = matcher.group(1);
                    for (Stereotype stereotype : target.getAppliedStereotypes())
                        if (stereotype.getName().equals(stereotypeName))
                            return new StereotypeApplication(stereotype, target);
                }
                return null;
            }
        });
        handlers.put(StereotypeApplication.class,
                new PropertyHandler<StereotypeApplication>() {
                    @Override
                    public Object getProperty(StereotypeApplication target,
                            String propertyName) {
                        return target.getValue(propertyName);
                    }
                });
        handlers.put(StructuredActivityNode.class,
                new PropertyHandler<StructuredActivityNode>() {
                    @Override
                    public Object getProperty(StructuredActivityNode target,
                            String propertyName) {
                        if ("cast".equals(propertyName))
                            return target.getNodes().size() == 2
                                    && target.getNodes().get(0) instanceof InputPin
                                    && target.getNodes().get(1) instanceof OutputPin;
                        if ("statements".equals(propertyName))
                            return ActivityUtils.findStatements(target);
                        return null;
                    }
                });
        handlers.put(Action.class, new PropertyHandler<Action>() {
            @Override
            public Object getProperty(Action target, String propertyName) {
                if ("terminal".equals(propertyName))
                    return ActivityUtils.isTerminal(target);
                if ("query".equals(propertyName))
                    return new QueryCore().buildQuery(target);
                return null;
            }
        });
        handlers.put(CallOperationAction.class,
                new PropertyHandler<CallOperationAction>() {
                    @Override
                    public Object getProperty(CallOperationAction target,
                            String propertyName) {
                        if ("parameterVariables".equals(propertyName)) {
                            Map<Variable, String> activityPool = getVariableNameAllocations((Activity) MDDUtil.getNearest(target, UMLPackage.Literals.ACTIVITY));
                            return ActionMappingUtils.getParameterVariables(
                                    activityPool, target);
                        }
                        if ("resultVariable".equals(propertyName)) {
                            Map<Variable, String> activityPool = getVariableNameAllocations((Activity) MDDUtil.getNearest(target, UMLPackage.Literals.ACTIVITY));
                            return ActionMappingUtils.getResultVariable(
                                    activityPool, target);
                        }
                        return null;
                    }
                });
        handlers.put(Variable.class, new PropertyHandler<Variable>() {
            @Override
            public Object getProperty(Variable target, String propertyName) {
                if ("suggestedName".equals(propertyName)) {
                    Map<Variable, String> activityPool = getVariableNameAllocations(MDDUtil.<Activity>getNearest(target, UMLPackage.Literals.ACTIVITY));
                    return ActionMappingUtils.generateName(activityPool,
                            target, null);
                }
                return null;
            }
        });

        handlers.put(MultiplicityElement.class,
                new PropertyHandler<MultiplicityElement>() {
                    @Override
                    public Object getProperty(MultiplicityElement target,
                            String propertyName) {
                        if ("required".equals(propertyName))
                            return target.getLower() != 0;
                        return null;
                    }
                });
        handlers.put(ObjectNode.class, new PropertyHandler<ObjectNode>() {
            @Override
            public Object getProperty(ObjectNode target, String propertyName) {
                if ("sourceAction".equals(propertyName))
                    return ActivityUtils.getSourceAction(target);
                if ("targetAction".equals(propertyName))
                    return ActivityUtils.getTargetAction(target);
                return null;
            }
        });
        handlers.put(Classifier.class, new PropertyHandler<Classifier>() {
            @Override
            public Object getProperty(Classifier target, String propertyName) {
                if ("navigableAssociationEnds".equals(propertyName)) {
                    List<Property> navigableAssociationEnds = new ArrayList<Property>();
                    for (Association association : target.getAssociations())
                        for (Property navigableOwnedEnd : association
                                .getNavigableOwnedEnds())
                            if (navigableOwnedEnd.getOtherEnd() != null
                                    && navigableOwnedEnd.getOtherEnd()
                                            .getType() == target)
                                navigableAssociationEnds.add(navigableOwnedEnd);
                    return navigableAssociationEnds;
                }
                return null;
            }
        });
        handlers.put(Property.class, new PropertyHandler<Property>() {
            public Object getProperty(Property target, String propertyName) {
                if ("derivation".equals(propertyName)) {
                    Constraint derivation = StructuralFeatureUtils
                            .getDerivation(target);
                    if (derivation == null)
                        return null;
                    return (Activity) ActivityUtils
                            .resolveBehaviorReference(derivation
                                    .getSpecification());
                }
                return null;
            }
        });
        handlers.put(NamedElement.class, new PropertyHandler<NamedElement>() {
            public Object getProperty(NamedElement target, String propertyName) {
                if ("public".equals(propertyName))
                    return target.getVisibility() == VisibilityKind.PUBLIC_LITERAL;
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
        handlers.put(Activity.class, new PropertyHandler<Activity>() {
            public Object getProperty(Activity target, String propertyName) {
                if ("query".equals(propertyName))
                    return new QueryCore().transformActivityToQuery(target);
                if ("rootAction".equals(propertyName))
                    return ActivityUtils.getRootAction(target);
                return null;
            }
        });
        return handlers;
    }

    @Override
    public String mapFileName(final Classifier toMap) {
        try {
            return perform(new MappingOperation<String>() {
               @Override
                public String map(StringTemplateGroup templateGroup) {
                   if (!templateGroup.isDefinedInThisGroup("outputPath"))
                       return toMap.getName() + ".txt";
                   StringTemplate outputTemplate = templateGroup
                           .getInstanceOf("outputPath");
                   outputTemplate.setAttribute("class", toMap);
                   return outputTemplate.toString(StringTemplateWriter.NO_WRAP);
                } 
            });
        } catch (STException e) {
            return toMap.getName() + "_error.txt";
        }
    }

    /**
     * Returns a map of suggested variable names for the activity the given element
     * is located within.
     * @param element
     * @return
     */
    private Map<Variable, String> getVariableNameAllocations(Activity scope) {
        while (MDDExtensionUtils.isClosure(scope))
            scope = MDDUtil.getNearest(
                    MDDExtensionUtils.getClosureContext(scope),
                    UMLPackage.Literals.ACTIVITY);
        Map<Variable, String> activityPool = this.suggestedVariableNames
                .get(scope);
        if (activityPool == null)
            this.suggestedVariableNames.put(scope,
                    activityPool = new HashMap<Variable, String>());
        return activityPool;
    }

}