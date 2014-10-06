package com.abstratt.mdd.target.jpa;

import static com.abstratt.mdd.core.util.ActivityUtils.getBodyNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Property;

import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.target.IMappingContext.Style;
import com.abstratt.mdd.core.target.spi.MapperFinder;
import com.abstratt.mdd.core.target.spi.MappingContext;
import com.abstratt.mdd.core.util.StereotypeUtils;
import com.abstratt.mdd.target.engine.st.ModelWrapper.PropertyHandler;
import com.abstratt.mdd.target.pojo.AnnotationInfo;
import com.abstratt.mdd.target.pojo.POJOMapper;
import com.abstratt.mdd.target.query.Join;
import com.abstratt.mdd.target.query.Query;
import com.abstratt.mdd.target.query.QueryCore;

public class JPAMapper extends POJOMapper {
	private QueryCore queryMapper = new QueryCore() ;
	
	@Override
	protected Collection<AnnotationInfo> collectAnnotations(Class target) {
		Collection<AnnotationInfo> basicAnnotations = super.collectAnnotations(target);
		basicAnnotations.add(new AnnotationInfo("Entity", null));
		EList<Operation> allOperations = target.getAllOperations();
		for (Operation operation : allOperations) 
			if (isQuery(operation) && !operation.getMethods().isEmpty()) {
				String behaviour = mapBehavior(operation);
				Map<String, Object> attributes = new TreeMap<String, Object>();
				attributes.put("name", "\"" + operation.getName() + "\"");
				attributes.put("query", "\"" + behaviour + "\"");
				basicAnnotations.add(new AnnotationInfo("NamedQuery", attributes));
			}
		return basicAnnotations;
	}

	private boolean isQuery(Operation operation) {
		return StereotypeUtils.hasStereotype(operation, "kirra::Query");
	}
	
	@Override
	protected List<Package> getBasePackages() {
		List<Package> basePackages = new ArrayList<Package>();
		basePackages.add(ValueSpecificationActionMapping.class.getPackage());
		// order matters - our mappings before the base mappings
		basePackages.addAll(super.getBasePackages());
		return basePackages;
	}
	
	@Override
	protected Collection<String> collectPackageImports(Class target) {
		Collection<String> baseImports = super.collectPackageImports(target);
		baseImports.add("javax.persistence.*");
		return baseImports;
	}
	
	@Override
	public void registerHandlers(
			Map<java.lang.Class<?>, PropertyHandler<?>> handlers) {
		super.registerHandlers(handlers);
		final PropertyHandler<Property> basicPropertyPropertyHandler = (PropertyHandler<Property>) handlers.get(Property.class);
		final PropertyHandler<Class> basicClassPropertyHandler = (PropertyHandler<Class>) handlers.get(Class.class);
		handlers.put(Class.class, new PropertyHandler<Class>() {
			public Object getProperty(Class target, String propertyName) {
				if ("ownedOperations".equals(propertyName)) {
					EList<Operation> ownedOperations = target.getOwnedOperations();
					ArrayList<Operation> nonQueryOperations = new ArrayList<Operation>(ownedOperations.size());
					for (Operation operation : ownedOperations)
						if (!isQuery(operation))
							nonQueryOperations.add(operation);
					return nonQueryOperations;
				}
				return basicClassPropertyHandler.getProperty(target, propertyName);
			}
		});

 		handlers.put(Property.class, new PropertyHandler<Property>() {
			@Override
			public Object getProperty(Property target, String propertyName) {
				if ("javaGetterAnnotations".equals(propertyName)) {
					if (target.getAssociation() != null) {
						String relationshipType;
						Map<String, Object> relationshipAttributes = new HashMap<String, Object>();
						if (target.getUpper() == 1) {
							boolean manyToOne = target.getOpposite() == null || target.getOpposite().isMultivalued();
							relationshipType = manyToOne ? "ManyToOne" : "OneToOne";
							relationshipAttributes.put("optional", target.getLower() == 0);	
						} else {
							boolean manyToMany = target.getOpposite() != null && target.getOpposite().isMultivalued();
							relationshipType = manyToMany ? "ManyToMany" : "OneToMany";
						}
						return new AnnotationInfo(relationshipType, relationshipAttributes);
					}
				}
				return basicPropertyPropertyHandler.getProperty(target, propertyName);
			}
		});
	}
	
	private void buildFromClause(StringBuffer result, Query query) {
		// FIXME: This clearly does not support the same entity appearing twice (as there is only one possible 
		// alias for a classifier), as often happens with recursive relationships, but should do for now. 
		// The metamodel for queries itself will have to be revisited in order to support that.
		
		// FIXME: this allows only joining via regular relationships. Joining by arbitrary attributes is not supported.

		Classifier sourceType = query.getSourceType();
		String resultAlias;
		if (query.getJoins().isEmpty())
			resultAlias = getAliasFor(sourceType);
		else 
			resultAlias = getAliasFor(query.getJoins().get(query.getJoins().size() - 1).getTarget());
		result.append(resultAlias);
        String fromTable = mapTypeReference(sourceType);
        String fromTableAlias = getAliasFor(sourceType);
		result.append(" from " + fromTable + " " + fromTableAlias);
    	if (query.getJoins().isEmpty())
    		return;
    	for (Join mapping; (mapping = queryMapper.findMapping(query.getJoins(), sourceType)) != null;) {
    		String toTableAlias = getAliasFor(mapping.getTarget());
    		List<Property> memberEnds = mapping.getAssociation().getMemberEnds();
    		Property fromProperty = memberEnds.get(0).getType() == mapping.getTarget().getType() ? memberEnds.get(0) : memberEnds.get(1);
			result.append(" inner join " + fromTableAlias + "." + fromProperty.getName() + " as " + toTableAlias);
    		sourceType = (Classifier) mapping.getTarget().getType();
    		fromTableAlias = toTableAlias;
		}
	}

	private void buildWhereClause(IMappingContext context, StringBuffer result, List<Activity> filters) {
    	if (filters.isEmpty())
    		return;
    	result.append(" where ");
    	for (int i = 0; i < filters.size(); i++) {
    		result.append(context.map(getBodyNode(filters.get(i))));
    		result.append(" and ");
		}
    	result.delete(result.length() - " and ".length(), result.length());
	}

	/**
	 * Computes an alias for the given classifier.
	 */
	public static String getAliasFor(NamedElement classifier) {
		return "_" + StringUtils.uncapitalize(classifier.getName()) + "_"; 
	}
	
	
	@Override
	public String mapBehavior(Operation operation, Activity behavior,
			Style style) {
		if (operation == null || !isQuery(operation))
			return super.mapBehavior(operation, behavior, style);
		Query query = queryMapper.transformActivityToQuery(behavior);
		if (query == null)
			return "";
		IMappingContext context = new MappingContext(this, Style.EXPRESSION, new MapperFinder(StructuredActivityNodeMapping.class));
        StringBuffer result = new StringBuffer("select ");
        buildFromClause(result, query);
        buildWhereClause(context, result, query.getFilters());
        // what we can translate:
        
        // AddVariableValue[returnValue] <- ReadExtent[classifier] : select * from classifier.getName()
        
        // AddVariableValue[returnValue] (CallOperation[Collection#select] (ReadExtent[classifier])) : select * from classifier.getName()
    	
		return result.toString();
	}

}
