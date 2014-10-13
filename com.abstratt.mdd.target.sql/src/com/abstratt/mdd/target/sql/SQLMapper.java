package com.abstratt.mdd.target.sql;

import static com.abstratt.mdd.core.util.ActivityUtils.getBodyNode;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Property;

import com.abstratt.mdd.core.target.IMappingContext.Style;
import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.target.spi.MapperFinder;
import com.abstratt.mdd.core.target.spi.MappingContext;
import com.abstratt.mdd.target.query.Join;
import com.abstratt.mdd.target.query.Query;
import com.abstratt.mdd.target.query.QueryCore;
import com.abstratt.mdd.target.sql.mappers.select.SelectMapper;
import com.abstratt.mdd.target.sql.mappers.select.StructuredActivityNodeMapping;

public class SQLMapper implements ITopLevelMapper<Operation> {
    private QueryCore queryMapper = new QueryCore();

    @Override
    public String map(Operation operation) {
        List<Behavior> methods = operation.getMethods();
        if (methods.isEmpty())
            return null;
        // we ignore any additional methods
        Activity behavior = (Activity) operation.getMethods().get(0);
        Query query = queryMapper.transformActivityToQuery(behavior);
        if (query == null)
            return "";
        StringBuffer result = new StringBuffer("select ");
        buildFromClause(result, query);
        buildWhereClause(result, query.getFilters());
        // what we can translate:

        // AddVariableValue[returnValue] <- ReadExtent[classifier] : select *
        // from classifier.getName()

        // AddVariableValue[returnValue] (CallOperation[Collection#select]
        // (ReadExtent[classifier])) : select * from classifier.getName()

        return result.toString();
    }
    
    @Override
    public boolean canMap(Operation element) {
        return true;
    }
    
    @Override
    public String mapFileName(Operation element) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public String mapAll(List<Operation> toMap) {
        throw new UnsupportedOperationException();
    }
    
    private void buildFromClause(StringBuffer result, Query query) {
        // FIXME: This clearly does not support the same entity appearing twice
        // (as there is only one possible
        // alias for a classifier), as often happens with recursive
        // relationships, but should do for now.
        // The metamodel for queries itself will have to be revisited in order
        // to support that.

        // FIXME: this allows only joining via regular relationships. Joining by
        // arbitrary attributes is not supported.

        Classifier sourceType = query.getSourceType();
        String resultAlias;
        if (query.getJoins().isEmpty())
            resultAlias = getAliasFor(query.getTargetType());
        else
            resultAlias = getAliasFor(query.getJoins()
                    .get(query.getJoins().size() - 1).getTarget());
        result.append(resultAlias + ".*");

        String fromTable = query.getSourceType().getName();
        String fromTableAlias = getAliasFor(query.getSourceType());
        result.append(" from " + fromTable + " " + fromTableAlias);
        if (query.getJoins().isEmpty())
            return;
        for (Join mapping; (mapping = queryMapper.findMapping(query.getJoins(),
                sourceType)) != null;) {
            String toTable = mapping.getTarget().getType().getName();
            List<Property> memberEnds = mapping.getAssociation()
                    .getMemberEnds();
            Property fromProperty = memberEnds.get(0).getType() == mapping
                    .getTarget().getType() ? memberEnds.get(0) : memberEnds
                    .get(1);
            result.append(" inner join " + toTable + " "
                    + getAliasFor(mapping.getTarget()));
            result.append(" on  " + fromTableAlias + "._"
                    + fromProperty.getName() + "ID_ = "
                    + getAliasFor(mapping.getTarget()) + "._"
                    + StringUtils.uncapitalize(toTable) + "ID_");
            sourceType = (Classifier) mapping.getTarget().getType();
        }
    }

    /**
     * Computes an alias for the given classifier.
     */
    public static String getAliasFor(NamedElement classifier) {
        return "_" + StringUtils.uncapitalize(classifier.getName()) + "_";
    }

    private void buildWhereClause(StringBuffer result, List<Activity> filters) {
        if (filters.isEmpty())
            return;
        result.append(" where ");
        SelectMapper selectMapper = new SelectMapper();
        for (int i = 0; i < filters.size(); i++) {
            result.append(selectMapper.map(getBodyNode(filters.get(i)),
                    new MappingContext(this, Style.EXPRESSION,
                            new MapperFinder(
                                    StructuredActivityNodeMapping.class))));
            result.append(" and ");
        }
        result.delete(result.length() - " and ".length(), result.length());
    }
}
