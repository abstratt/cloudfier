package com.abstratt.nodestore.jdbc;

import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.abstratt.kirra.DataElement;
import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Relationship.Style;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;

/**
 * Heuristics for generating a database schema from a Kirra domain model.
 *
 * Each class becomes a table
 *
 * A many to many relationship becomes a mapping table.
 *
 * A many to one relationship:
 *
 * - if navigable both ways, or Many->One, it becomes a FK on the Many side to
 * the One side - or else, it becomes a mapping table
 *
 * A one-to-one relationship becomes a FK on the primary side to the secondary
 * side (what about composition/aggregation).
 */
public class SQLGenerator {

    public static boolean isMappingTableRelationship(SchemaManagement metadata, Relationship element) {
        Validate.isTrue(!element.isDerived());
        Relationship opposite = metadata.getOpposite(element);
        if (opposite == null)
            return element.isMultiple();
        if (element.isMultiple()) {
            if (opposite.isMultiple())
                // no other way
                return true;
            // can other side hold the FK?
            return !opposite.isNavigable();
        }
        if (!element.isMultiple() && !opposite.isMultiple() && element.isNavigable() && opposite.isNavigable())
            return true;
        // can one of the sides hold the FK?
        return opposite.isMultiple() && !element.isNavigable();
    }

    private SchemaManagement metadata;

    private String catalogName;

    public SQLGenerator(String catalogName, SchemaManagement metadata) {
        this.catalogName = escape(sanitizeName(catalogName));
        this.metadata = metadata;
        Validate.isTrue(metadata != null);
    }

    public String escape(String name) {
        return "\"" + name + "\"";
    }

    public List<String> generateConstraints(Entity clazz) {
        List<String> statements = new ArrayList<String>();
        for (Relationship relationship : getRelationships(clazz)) {
            if (relationship.isNavigable() && !isMappingTableRelationship(relationship)) {
                String stmt = "alter table " + modelToSchemaName(clazz);
                stmt += " add constraint " + generateSelfToOppositeFK(relationship) + " foreign key ("
                        + generateSelfToOppositeFK(relationship) + ") references "
                        + modelToSchemaName(metadata.getEntity(relationship.getTypeRef())) + " (id)";
                stmt += " on delete";
                if (relationship.getStyle() == Style.PARENT)
                	// if you have a parent you should be deleted as well
                    stmt += " cascade";
                else
                	if (!relationship.isRequired())
                		stmt += " set null";
                    else
                    	// let it blow into the user's face
                        stmt += " no action";
                stmt += " deferrable initially deferred;";
                statements.add(stmt);
            }
        }
        for (Relationship multi : getMappingTableRelationships(clazz))
            if (multi.isPrimary())
                statements.addAll(generateMappingConstraints(clazz, multi));
        for (Property property : clazz.getProperties())
            if (property.isUnique() && (!property.isHasDefault() || !property.isDerived())) 
                statements.addAll(generateUniqueConstraints(clazz, property));

        return statements;
    }

    public List<String> generateCreateSchema() {
        List<String> statements = new ArrayList<String>();
        statements.add("create schema " + catalogName + ";");
        statements.add("create sequence " + catalogName + ".sequence;");
        return statements;
    }
    
    public List<String> generateFullCreateSchema(Collection<String> allPackages) {
        List<String> creationStatements = new ArrayList<String>();
        creationStatements.addAll(generateCreateSchema());
        for (String pkg : allPackages)
            creationStatements.addAll(generateCreateTables(pkg));
        for (String pkg : allPackages)
            creationStatements.addAll(generateTableConstraints(pkg));
        return creationStatements;
    }

    public List<String> generateCreateTables(String namespace) {
        List<String> statements = new ArrayList<String>();
        for (Entity clazz : metadata.getEntities(namespace))
            statements.addAll(generateTable(clazz));
        return statements;
    }
    
    public List<String> generateTableConstraints(String namespace) {
        List<String> statements = new ArrayList<String>();
        for (Entity clazz : metadata.getEntities(namespace))
            statements.addAll(generateConstraints(clazz));
        return statements;
    }


    public List<String> generateDelete(Entity clazz, long id) {
    	List<String> statements = new ArrayList<>();
    	statements.add("delete from " + modelToSchemaName(clazz) + " where id = " + id + ";");
		return statements;
    }

    public List<String> generateDropSchema(boolean required) {
        String stmt = "drop schema";
        if (!required)
            stmt += " if exists";
        stmt += " " + catalogName + " cascade;";
        return Arrays.asList(stmt);
    }

    public String generateGetSequence() {
        return "select nextval('" + catalogName + ".sequence') as id";
    }

    public List<String> generateInsert(Entity clazz, Map<String, Object> values, Map<String, Collection<Long>> references, Long id) {
        String stmt = "insert into " + modelToSchemaName(clazz) + " (id";
        for (DataElement property : getProperties(clazz))
            stmt += ", " + modelToSchemaName(property);
        for (Relationship relationship : getRelationships(clazz))
            if (!isMappingTableRelationship(relationship))
                stmt += ", " + generateSelfToOppositeFK(relationship);
        stmt += ") values (";

        if (id == null)
            stmt += "nextval('" + catalogName + ".sequence')";
        else
            stmt += id;

        for (Property property : getProperties(clazz)) {
            String dbValue = property.isAutoGenerated() ? "nextval('" + catalogName + ".sequence')" : toDBValue(
                    values.get(property.getName()), property.getTypeRef());
            stmt += ", " + dbValue;
        }
        for (Relationship relationship : getRelationships(clazz)) {
            if (!isMappingTableRelationship(relationship)) {
                Collection<Long> singleRef = references.get(relationship.getName());
                Long fkValue = singleRef == null || singleRef.isEmpty() ? null : singleRef.iterator().next();
                fkValue = (fkValue == null && relationship.isRequired()) ? ((Long) (-1L)) : fkValue;
                stmt += ", " + fkValue;
            }
        }
        stmt += ");";

        return Arrays.asList(stmt);
    }

    public List<String> generateRemoveRelated(Relationship myRelationship, TypeRef thisType, Long thisId, TypeRef relatedType, Long otherId) {
        Entity otherEntity = metadata.getEntity(relatedType);
        Entity thisEntity = metadata.getEntity(thisType);
        Relationship otherEnd = metadata.getOpposite(myRelationship);

        if (myRelationship.isMultiple())
            if (otherEnd != null) {
                if (!otherEnd.isMultiple())
                    // go the other way
                    return generateRemoveRelated(otherEnd, relatedType, otherId, thisType, thisId);
                return generateRemoveRelatedViaMappingTable(thisEntity, myRelationship, thisId, otherEnd, otherId);
            }
        if (isMappingTableRelationship(myRelationship)) {
            return generateRemoveRelatedViaMappingTable(thisEntity, myRelationship, thisId, otherEnd, otherId);
        }
        // either 1:1 or many:1
        switch (myRelationship.getStyle()) {
        case CHILD:
            String deleteOtherStmt = "delete from " + modelToSchemaName(otherEntity) + " where id = " + otherId;
            return Arrays.asList(deleteOtherStmt);
        case PARENT:
            String deleteThisStmt = "delete from " + modelToSchemaName(thisEntity) + " where id = " + thisId;
            return Arrays.asList(deleteThisStmt);
        case LINK:
            String unlinkThisStmt = "update " + modelToSchemaName(thisEntity) + " set " + generateSelfToOppositeFK(myRelationship)
                    + " = null where id = " + thisId;
            return Arrays.asList(unlinkThisStmt);
        }
        // never runs
        throw new Error();
    }

    public List<String> generateSelectAll(Entity clazz) {
        String stmt = generateSelect(clazz) + ";";
        return Arrays.asList(stmt);
    }
    

    public List<String> generateSelectSome(Entity clazz, Map<String, Collection<Object>> criteria, Integer limit) {
        String stmt = generateSelect(clazz);
        stmt += " where ";
        List<String> terms = new ArrayList<String>();
        for (Entry<String, Collection<Object>> entry : criteria.entrySet()) {
            if (entry.getValue().size() == 1) {
                Object singleValue = entry.getValue().iterator().next();
                terms.add(entry.getKey() + " = " + (singleValue instanceof String ? ("'" + singleValue + "'") : singleValue));
            } else if (entry.getValue().size() == 2) {
                Collection<Object> multipleValues = entry.getValue();
                String in = entry.getKey() + " in (";
                List<String> items = new ArrayList<String>();
                for (Object value : multipleValues)
                    items.add(value instanceof String ? ("'" + value + "'") : (String) value);
                in += StringUtils.join(items, ", ");
                in += ")";
                terms.add(in);
            }
        }
        stmt += StringUtils.join(terms, " and ");
        if (limit != null)
            stmt += " LIMIT " + limit;
        stmt += ";";
        return Arrays.asList(stmt);
    }


    public List<String> generateSelectOne(Entity clazz, long key) {
        String stmt = generateSelect(clazz);
        stmt += " where id = " + key + ";";
        return Arrays.asList(stmt);
    }
    
    /**
     * A convenience method that assumes the relationship is not polymorphic.
     */
    public List<String> generateSelectRelatedKeys(Relationship myRelationship, long key) { 
    	return generateSelectRelatedKeys(myRelationship, myRelationship.getTypeRef(), key);
    }

    /**
     * Generates a SQL query that returns the related keys from the context object through the given relationship, where all
     * keys identify object of the given other entity.
     * 
     * @param myRelationship the relationship to traverse
     * @param otherEntity the type of the related objects (useful in polymorphic associations)
     * @param key the id of the current object
     */
    public List<String> generateSelectRelatedKeys(Relationship myRelationship, TypeRef otherEntityRef, long key) {
        Relationship otherEnd = metadata.getOpposite(myRelationship);
        Entity contextEntity = metadata.getEntity(myRelationship.getOwner());
        Entity otherEntity = metadata.getEntity(otherEntityRef);
        if (otherEnd == null) {
            if (myRelationship.isMultiple())
                return generateSelectManyRelatedKeysViaMappingTable(myRelationship, key, otherEnd, contextEntity);
            return generateSelectOneRelatedKey(myRelationship, key, otherEnd, otherEntity, contextEntity);
        }
        if (isMappingTableRelationship(myRelationship))
            return generateSelectManyRelatedKeysViaMappingTable(myRelationship, key, otherEnd, contextEntity);
        if (myRelationship.isMultiple())
            return generateSelectManyRelatedDirectlyKeys(key, otherEnd, otherEntity);
        return generateSelectOneRelatedKey(myRelationship, key, otherEnd, otherEntity, contextEntity);
    }

    public List<String> generateSetRelated(Relationship myRelationship, long targetKey, Collection<Long> relatedKeys,
            boolean replaceExisting) {
        Validate.isTrue(myRelationship != null);
        Relationship otherEnd = metadata.getOpposite(myRelationship);
        List<String> result = new ArrayList<String>();

        if (isMappingTableRelationship(myRelationship)) {
            Entity contextClass = metadata.getEntity(myRelationship.getOwner());
            String associationName = getMappingTableName(contextClass, myRelationship, otherEnd);
            String mappingTableName = tableName(contextClass.getEntityNamespace(), associationName);
            // don't delete other existing relationships if relationship admits
            // multiple instances
            if (replaceExisting && !myRelationship.isMultiple()) {
                String unlinkStmt = "delete from " + mappingTableName + " where " + generateOppositeToSelfFK(otherEnd) + " = " + targetKey
                        + ";";
                result.add(unlinkStmt);
            }
            if (!relatedKeys.isEmpty()) {
                for (Long relatedKey : relatedKeys) {
                    String newLinkStmt = "insert into " + mappingTableName;
                    newLinkStmt += " (" + generateSelfToOppositeFK(myRelationship) + ", " + generateOppositeToSelfFK(otherEnd) + ")";
                    newLinkStmt += " values (" + relatedKey + ", " + targetKey + ");";
                    result.add(newLinkStmt);
                }
            }
            return result;
        }

        if (myRelationship.isMultiple()) {
            Entity clazz = metadata.getEntity(myRelationship.getTypeRef());
            if (replaceExisting && !otherEnd.isRequired()) {
                String unlinkStmt = "update " + modelToSchemaName(clazz);
                unlinkStmt += " set " + generateSelfToOppositeFK(otherEnd) + " = null";
                unlinkStmt += " where " + generateSelfToOppositeFK(otherEnd) + " = " + targetKey + ";";
                result.add(unlinkStmt);
            }
            if (!relatedKeys.isEmpty()) {
                String newLinkStmt = "update " + modelToSchemaName(clazz);
                newLinkStmt += " set " + generateSelfToOppositeFK(otherEnd) + " = " + targetKey;
                newLinkStmt += " where id in (" + StringUtils.join(relatedKeys, ", ") + ");";
                result.add(newLinkStmt);
            }
            return result;
        }

        Entity clazz = metadata.getEntity(myRelationship.getOwner());

        if (!myRelationship.isRequired()) {
            String unlinkStmt = "update " + modelToSchemaName(clazz);
            unlinkStmt += " set " + modelToSchemaName(myRelationship) + " = null";
            unlinkStmt += " where id = " + targetKey + ";";
            result.add(unlinkStmt);
        }

        if (!relatedKeys.isEmpty()) {
            String updateStmt = "update " + modelToSchemaName(clazz);
            updateStmt += " set " + modelToSchemaName(myRelationship) + " = " + relatedKeys.iterator().next();
            updateStmt += " where id = " + targetKey + ";";
            result.add(updateStmt);
        }
        return result;
    }

    public List<String> generateTable(Entity clazz) {
        String stmt = "create table " + modelToSchemaName(clazz) + " (id bigint primary key";
        for (DataElement property : getProperties(clazz)) {
            stmt += ", " + modelToSchemaName(property) + " " + getDBType(property);
            if (property.isRequired())
                stmt += " not null";
        }
        List<Relationship> mappingTableRelationships = new ArrayList<Relationship>();
        for (Relationship relationship : getRelationships(clazz, false)) {
            if (!relationship.isNavigable())
                continue;
            if (isMappingTableRelationship(relationship)) {
                if (relationship.isPrimary())
                    mappingTableRelationships.add(relationship);
                continue;
            }
            stmt += ", " + generateSelfToOppositeFK(relationship) + " bigint";
            if (relationship.isRequired())
                stmt += " not null";
        }
        stmt += ");";
        List<String> result = new ArrayList<String>();
        result.add(stmt);
        for (Relationship mappingRelationship : getMappingTableRelationships(clazz))
            if (mappingRelationship.isPrimary())
                result.addAll(generateMappingTable(clazz, mappingRelationship));
        return result;
    }

    public List<String> generateUpdate(Entity clazz, Map<String, Object> values, Map<String, Collection<Long>> references, Long id) {
        String stmt = "update " + modelToSchemaName(clazz) + " set ";
        for (Property property : getProperties(clazz))
            if (!property.isAutoGenerated())
                stmt += modelToSchemaName(property) + " = " + toDBValue(values.get(property.getName()), property.getTypeRef()) + ", ";
        for (Relationship relationship : getRelationships(clazz)) {
            if (!isMappingTableRelationship(relationship)) {
                Collection<Long> singleRef = references.get(relationship.getName());
                stmt += generateSelfToOppositeFK(relationship) + " = "
                        + (singleRef == null || singleRef.isEmpty() ? null : singleRef.iterator().next()) + ", ";
            }
        }
        stmt = stmt.substring(0, stmt.length() - 2);
        stmt += " where id = " + id + ";";
        return Arrays.asList(stmt);
    }

    public List<String> generateValidate(Relationship relationship) {
        Validate.isTrue(relationship != null);
        if (relationship.isDerived() || !relationship.isNavigable())
            return Collections.emptyList();
        List<String> statements = new ArrayList<String>();
        // check lower boundary
        Entity context = metadata.getEntity(relationship.getOwner());
        if (relationship.isRequired()) {
            String otherAlias = generateSelfToOppositeFK(relationship);
            Relationship otherEnd = metadata.getOpposite(relationship);
            String thisAlias = generateOppositeToSelfFK(otherEnd);
            if (isMappingTableRelationship(relationship)) {
                if (relationship.isPrimary()) {
                    String statement = "select 1 from " + modelToSchemaName(context) + " as " + thisAlias;
                    String mappingTableName = getMappingTableName(context, relationship, otherEnd);
                    String mappingTableAlias = escape(mappingTableName);
                    statement += " left join " + tableName(context.getEntityNamespace(), mappingTableName) + asAlias(mappingTableAlias);
                    statement += " on " + mappingTableAlias + "." + generateOppositeToSelfFK(otherEnd);
                    statement += " = " + thisAlias + ".id";
                    statement += " where " + mappingTableAlias + "." + otherAlias + " is null;";
                    statements.add(statement);
                }
            } else {
                if (relationship.isPrimary()) {
                    Entity otherEntity = metadata.getEntity(relationship.getTypeRef());
                    String statement = "select 1 from " + modelToSchemaName(context) + " as " + thisAlias;
                    statement += " left join " + tableName(otherEntity.getEntityNamespace(), otherEntity.getName()) + asAlias(otherAlias);
                    statement += " on " + otherAlias + ".id";
                    statement += " = " + thisAlias + '.' + modelToSchemaName(relationship);
                    statement += " where " + otherAlias + ".id is null;";
                    statements.add(statement);
                }
            }
        }
        // check upper boundary
        return statements;
    }

    public int getJDBCType(TypeRef type) {
        String name = type.getTypeName();
        if ("Integer".equals(name))
            return Types.BIGINT;
        if ("Double".equals(name))
            return Types.NUMERIC;
        if ("String".equals(name) || "Memo".equals(name))
            return Types.VARCHAR;
        if ("Boolean".equals(name))
            return Types.BOOLEAN;
        if ("Date".equals(name))
            return Types.DATE;
        if (type.getKind() == TypeKind.Enumeration)
            return Types.VARCHAR;
        if ("Blob".equals(name))
            return Types.BLOB;
        throw new IllegalArgumentException("" + name);
    }

    public boolean isCatalogSchema(String schemaName) {
        return schemaName.startsWith(getCatalogPrefix());
    }

    public String modelToSchemaName(DataElement property) {
        return modelToSchemaName(property, true);
    }

    public String modelToSchemaName(DataElement property, boolean escape) {
        String basic = basicModelToSchemaName(property);
        return escape ? escape(basic) : basic;
    }

    protected String asAlias(String alias) {
        return alias == null ? "" : " as " + alias;
    }

    protected String basicModelToSchemaName(DataElement property) {
        Validate.isTrue(property.getName() != null);
        String propertyName = property.getName();
        validateSchemaName(property.getClass().getSimpleName(), propertyName);
        return propertyName;
    }

    protected String generateOppositeToSelfFK(Relationship opposite) {
        return generateSelfToOppositeFK(opposite);
    }

    protected String generateSelect(Entity clazz) {
        return generateSelect(clazz, null);
    }

    protected String generateSelect(Entity clazz, String alias) {
        return generateSelect(clazz, alias, false);
    }

    protected String generateSelect(Entity clazz, String alias, boolean keysOnly) {
        String aliasPrefix = alias == null ? "" : alias + '.';
        String stmt = "select " + aliasPrefix + "id";
        if (!keysOnly) {
            for (DataElement property : getProperties(clazz))
                stmt += ", " + aliasPrefix + modelToSchemaName(property);
            for (Relationship relationship : getRelationships(clazz))
                if (relationship.isNavigable() && !isMappingTableRelationship(relationship))
                    stmt += ", " + aliasPrefix + generateSelfToOppositeFK(relationship);
        }
        stmt += " from " + modelToSchemaName(clazz);
        return stmt + asAlias(alias);
    }

    protected String generateSelfToOppositeFK(Relationship multi) {
        return multi == null ? "__self__" : modelToSchemaName(multi);
    }

    protected String getCatalogPrefix() {
        return catalogName + "/";
    }

    protected String getMappingTableName(Entity contextEntity, Relationship source, Relationship opposite) {
        String associationName = source.getAssociationName();
        if (!StringUtils.isBlank(associationName))
            return associationName;
        if (opposite != null && opposite.isNavigable()) {
            Relationship primary = source.isPrimary() ? source : opposite;
            Relationship secondary = source.isPrimary() ? opposite : source;
            return primary.getName() + "_" + secondary.getName();
        }
        return contextEntity.getName() + "_" + source.getName();
    }

    protected boolean isPersistable(DataElement property) {
        return !property.isDerived() && !property.isMultiple();
    }

    protected boolean isPersistable(Relationship property) {
        Relationship opposite = metadata.getOpposite(property);
        return !property.isDerived() && (opposite == null || !opposite.isDerived());
    }

    protected boolean isRedundant(Relationship relationship) {
        return relationship.isNavigable() && metadata.getOpposite(relationship) != null && !metadata.getOpposite(relationship).isMultiple()
                && metadata.getOpposite(relationship).isPrimary();
    }

    protected String modelToSchemaName(Entity clazz) {
        return tableName(clazz.getEntityNamespace(), clazz.getName());
    }

    protected String modelToSchemaName(String package_) {
        String schemaName = getCatalogPrefix() + sanitizeName(package_);
        validateSchemaName("schema", schemaName);
        return "\"" + schemaName + "\"";
    }

    protected String modelToSchemaName(TypeRef clazz) {
        return tableName(clazz.getEntityNamespace(), clazz.getTypeName());
    }

    protected String sanitizeName(String name) {
        return name.replace("\"", "-").replace("/", "-");
    }

    protected String sanitizeStringValue(String value) {
        // escape single quotes
        return value.replace("'", "''");
    }

    protected String tableName(String entityNamespace, String localName) {
        String tableName = escape(entityNamespace + '_' + localName);
        validateSchemaName("table", tableName);
        return catalogName + '.' + tableName;
    }

    protected void validateSchemaName(String kind, String schemaName) {
        if (schemaName.length() > 63)
            throw new RuntimeException(kind + " name is too long: " + schemaName);
    }

    boolean isMappingTableRelationship(Relationship element) {
        return SQLGenerator.isMappingTableRelationship(metadata, element);
    }

    private Collection<String> generateMappingConstraints(Entity contextEntity, Relationship multiPrimary) {
        Validate.isTrue(multiPrimary.isPrimary());
        Relationship multiSecondary = metadata.getOpposite(multiPrimary);
        String selfToOppositeFK = generateSelfToOppositeFK(multiPrimary);
        String oppositeToSelfFK = generateOppositeToSelfFK(multiSecondary);
        String associationName = getMappingTableName(contextEntity, multiPrimary, multiSecondary);

        String tableName = tableName(contextEntity.getEntityNamespace(), associationName);
        String stmt1 = "alter table " + tableName;
        stmt1 += " add constraint " + selfToOppositeFK + " foreign key (" + selfToOppositeFK + ") references "
                + modelToSchemaName(metadata.getEntity(multiPrimary.getTypeRef())) + " (id)";
        stmt1 += " on delete cascade;";

        String stmt2 = "alter table " + tableName;
        stmt2 += " add constraint " + oppositeToSelfFK + " foreign key (" + oppositeToSelfFK + ") references "
                + modelToSchemaName(contextEntity) + " (id)";
        stmt2 += " on delete cascade;";

        return Arrays.asList(stmt1, stmt2);
    }

    private Collection<String> generateMappingTable(Entity contextEntity, Relationship multi) {
        Validate.isTrue(multi.isPrimary());
        Relationship opposite = metadata.getOpposite(multi);
        String selfToOppositeFK = generateSelfToOppositeFK(multi);
        String oppositeToSelfFK = generateOppositeToSelfFK(opposite);
        String associationName = getMappingTableName(contextEntity, multi, opposite);
        String stmt = "create table " + tableName(contextEntity.getEntityNamespace(), associationName) + " (";
        stmt += selfToOppositeFK + " bigint not null";
        stmt += ", " + oppositeToSelfFK + " bigint not null";
        stmt += ");";
        return Arrays.asList(stmt);
    }

    private List<String> generateRemoveRelatedViaMappingTable(Entity thisEntity, Relationship myRelationship, Long thisId,
            Relationship otherEnd, Long otherId) {
        String associationName = getMappingTableName(thisEntity, myRelationship, otherEnd);
        String mappingTableName = tableName(thisEntity.getEntityNamespace(), associationName);

        String unlinkStmt = "delete from " + mappingTableName + " where " + generateOppositeToSelfFK(otherEnd) + " = " + thisId + " and "
                + generateSelfToOppositeFK(myRelationship) + " = " + otherId + ";";
        return Arrays.asList(unlinkStmt);
    }

    private String generateSelectKeysViaMappingTable(Entity context, Relationship myRelationship, Relationship otherEnd) {
        Entity otherEntity = metadata.getEntity(myRelationship.getTypeRef());
        String otherAlias = modelToSchemaName(myRelationship);
        String statement = generateSelect(otherEntity, otherAlias, true);
        String mappingTableName = getMappingTableName(context, myRelationship, otherEnd);
        String mappingTableAlias = escape(mappingTableName);
        statement += " join " + tableName(context.getEntityNamespace(), mappingTableName) + asAlias(mappingTableAlias);
        statement += " on " + mappingTableAlias + "." + modelToSchemaName(myRelationship);
        statement += " = " + otherAlias + ".id";
        return statement;
    }

    private List<String> generateSelectManyRelatedDirectlyKeys(long key, Relationship otherEnd, Entity clazz) {
        String stmt = generateSelect(clazz, null, true);
        stmt += " where " + generateSelfToOppositeFK(otherEnd) + " = " + key + ";";
        return Arrays.asList(stmt);
    }

    private List<String> generateSelectManyRelatedKeysViaMappingTable(Relationship myRelationship, long key, Relationship otherEnd,
            Entity contextEntity) {
        String mappingTableName = getMappingTableName(contextEntity, myRelationship, otherEnd);
        String stmt = generateSelectKeysViaMappingTable(contextEntity, myRelationship, otherEnd);
        stmt += " where " + escape(mappingTableName) + "." + generateSelfToOppositeFK(otherEnd) + " = " + key + ";";
        return Arrays.asList(stmt);
    }

    private List<String> generateSelectOneRelatedKey(Relationship myRelationship, long key, Relationship otherEnd, Entity otherEntity,
            Entity contextEntity) {
        Relationship sourceRelationship = myRelationship.isNavigable() ? otherEnd : myRelationship;
        Relationship targetRelationship = myRelationship.isNavigable() ? myRelationship : otherEnd;

        String statement = generateSelect(otherEntity, modelToSchemaName(myRelationship), true);
        statement += " join " + tableName(contextEntity.getEntityNamespace(), contextEntity.getName())
                + asAlias(generateSelfToOppositeFK(otherEnd));
        statement += " on " + modelToSchemaName(targetRelationship) + ".id";
        statement += " = " + generateSelfToOppositeFK(sourceRelationship) + '.' + modelToSchemaName(targetRelationship);
        statement += " where " + generateSelfToOppositeFK(otherEnd) + ".id = " + key + ";";
        return Arrays.asList(statement);
    }

    private Collection<? extends String> generateUniqueConstraints(Entity contextEntity, Property uniqueProperty) {
        Validate.isTrue(uniqueProperty.isUnique());

        String tableName = modelToSchemaName(contextEntity);
        String stmt1 = "alter table " + tableName;
        String constraintName = uniqueConstraintName(contextEntity, uniqueProperty);
        stmt1 += " add constraint " + constraintName + " unique (" + modelToSchemaName(uniqueProperty) + ");";
        return Arrays.asList(stmt1);
    }

    private String getDBType(DataElement property) {
        return getDBType(property.getTypeRef());
    }

    private String getDBType(TypeRef type) {
        String name = type.getTypeName();
        if ("Blob".equals(name))
            return "bytea";
        if ("Integer".equals(name))
            return "bigint";
        if ("Double".equals(name))
            return "numeric";
        if ("String".equals(name) || "Memo".equals(name))
            return "varchar";
        if ("Boolean".equals(name))
            return "boolean";
        if ("Date".equals(name))
            return "date";
        if (type.getKind() == TypeKind.Enumeration)
            return "varchar";
        throw new IllegalArgumentException(type.toString());
    }

    private Collection<Relationship> getMappingTableRelationships(Entity clazz) {
        Collection<Relationship> result = new LinkedList<Relationship>();
        for (Relationship element : clazz.getRelationships())
            if (isPersistable(element) && isMappingTableRelationship(element))
                result.add(element);
        return result;
    }

    private Collection<Property> getProperties(Entity clazz) {
        Collection<Property> result = new LinkedList<Property>();
        for (Property property : clazz.getProperties())
            if (isPersistable(property))
                result.add(property);
        return result;
    }

    private Collection<Relationship> getRelationships(Entity clazz) {
        return getRelationships(clazz, false);
    }

    private Collection<Relationship> getRelationships(Entity clazz, boolean multiple) {
        Collection<Relationship> result = new LinkedList<Relationship>();
        for (Relationship element : clazz.getRelationships())
            if (isPersistable(element) && multiple == element.isMultiple())
                result.add(element);
        return result;
    }

    private String toDBValue(Object value, TypeRef type) {
        if (value == null)
            return null;
        if (type.getKind() == TypeKind.Enumeration)
            return "'" + sanitizeStringValue(value.toString()) + "'";
        String name = type.getTypeName();
        if ("String".equals(name) || "Memo".equals(name))
            return "'" + sanitizeStringValue((String) value) + "'";
        if ("Date".equals(name))
            return "'" + new SimpleDateFormat("yyyy-MM-dd").format((Date) value) + "'";
        return "" + value;
    }

    private String uniqueConstraintName(Entity contextEntity, DataElement uniqueProperty) {
        String namespace = contextEntity.getNamespace();
        String entityName = contextEntity.getName();
        String constraintName = escape(namespace + '_' + entityName + '_' + modelToSchemaName(uniqueProperty, false) + "_uk");
        validateSchemaName("constraint", constraintName);
        return constraintName;
    }
}
