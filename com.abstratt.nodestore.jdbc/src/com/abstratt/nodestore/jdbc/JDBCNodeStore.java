package com.abstratt.nodestore.jdbc;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Assert;

import com.abstratt.kirra.DataElement;
import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.TypeRef;
import com.abstratt.nodestore.BasicNode;
import com.abstratt.nodestore.INode;
import com.abstratt.nodestore.INodeKey;
import com.abstratt.nodestore.INodeStore;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.nodestore.IntegerKey;
import com.abstratt.nodestore.NodeNotFoundException;
import com.abstratt.nodestore.NodeReference;
import com.abstratt.nodestore.NodeStoreException;
import com.abstratt.pluginutils.LogUtils;

public class JDBCNodeStore implements INodeStore {

    interface ConnectionRunnable<R> {
        public R run(Connection connection) throws SQLException;
    }

    interface IResultHandler<T> {
        T handle(ResultSet rs) throws SQLException;
    }

    static final class LoadKeyHandler implements IResultHandler<INodeKey> {
        @Override
        public INodeKey handle(ResultSet rs) throws SQLException {
            return new IntegerKey(rs.getLong("id"));
        }
    }

    static final class LoadSingleValueHandler<T> implements IResultHandler<T> {
        @Override
        public T handle(ResultSet rs) throws SQLException {
            return (T) rs.getObject(1);
        }
    }

    private final class LoadNodeHandler implements IResultHandler<INode> {
        @Override
        public INode handle(ResultSet rs) throws SQLException {
            Map<String, Object> properties = new HashMap<String, Object>();
            for (Property property : clazz.getProperties()) {
                if (isPersistable(property)) {
                    Object value = getValue(property, rs);
                    if (value != null)
                        properties.put(property.getName(), value);
                }
            }
            Map<String, Collection<NodeReference>> related = new HashMap<String, Collection<NodeReference>>();
            for (Relationship relationship : clazz.getRelationships()) {
                if (isPersistable(relationship) && !basicGetCatalog().getGenerator().isMappingTableRelationship(relationship)) {
                    Long id = rs.getLong(getGenerator().modelToSchemaName(relationship, false));
                    if (!rs.wasNull())
                        related.put(relationship.getName(),
                                Collections.singleton(new NodeReference(relationship.getTypeRef().getFullName(), idToKey(id))));
                }
            }
            BasicNode loadedNode = new BasicNode(idToKey(rs.getLong("id")));
            loadedNode.setProperties(properties);
            loadedNode.setRelated(related);
            return loadedNode;
        }
    }

    static <T> List<T> loadMany(ConnectionProvider provider, final IResultHandler<T> handler, final List<String> statements) {
        try {
            return JDBCNodeStore.runWithConnection(provider, new ConnectionRunnable<List<T>>() {
                @Override
                public List<T> run(Connection connection) throws SQLException {
                    List<T> results = new ArrayList<T>();
                    for (String string : statements) {
                        JDBCNodeStore.logSQLStatement(string);
                        PreparedStatement prepared = connection.prepareStatement(string);
                        try {
                            ResultSet rs = prepared.executeQuery();
                            try {
                                while (rs.next())
                                    results.add(handler.handle(rs));
                            } finally {
                                rs.close();
                            }
                        } finally {
                            prepared.close();
                        }
                    }
                    return results;
                }

            });
        } catch (SQLException e) {
            throw new NodeStoreException("Error performing query: " + e.getMessage(), e);
        }
    }

    static <T> T loadOne(ConnectionProvider provider, IResultHandler<T> handler, String statement) {
        List<T> results = JDBCNodeStore.loadMany(provider, handler, Arrays.asList(statement));
        return results.isEmpty() ? null : results.get(0);
    }

    static void logSQLStatement(String string) {
        if (JDBCNodeStore.DEBUG_SQL)
            System.out.println("***" + string);
    }

    static List<INodeKey> perform(ConnectionProvider provider, final List<String> statements, final boolean returnKeys,
            boolean changeExpected) {
        List<SQLStatement> sqlStatements = new ArrayList<SQLStatement>(statements.size());
        for (String string : statements)
            sqlStatements.add(new SQLStatement(string, false));
        return JDBCNodeStore.performStatements(provider, sqlStatements, returnKeys, changeExpected);
    }

    static List<INodeKey> performStatements(ConnectionProvider provider, final List<SQLStatement> statements, final boolean returnKeys) {
        return JDBCNodeStore.performStatements(provider, statements, returnKeys, false);
    }

    static List<INodeKey> performStatements(ConnectionProvider provider, final List<SQLStatement> statements, final boolean returnKeys,
            final boolean changeExpected) {
        if (statements.isEmpty())
            return Collections.emptyList();
        try {
            return JDBCNodeStore.runWithConnection(provider, new ConnectionRunnable<List<INodeKey>>() {
                @Override
                public List<INodeKey> run(Connection connection) throws SQLException {
                    List<INodeKey> generated = new ArrayList<INodeKey>();
                    int rowsAffected = 0;
                    List<String> sqlStatements = new ArrayList<String>();
                    for (SQLStatement statement : statements) {
                        JDBCNodeStore.logSQLStatement(statement.string);
                        sqlStatements.add(statement.string);
                        PreparedStatement prepared = connection.prepareStatement(statement.string,
                                returnKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
                        if (statement.parameters != null)
                            for (int i = 0; i < statement.parameters.size(); i++)
                                prepared.setObject(i, statement.parameters.get(i));
                        try {
                            rowsAffected += prepared.executeUpdate();
                            if (returnKeys) {
                                ResultSet rs = prepared.getGeneratedKeys();
                                try {
                                    while (rs.next())
                                        generated.add(new IntegerKey(rs.getLong(1)));
                                } finally {
                                    rs.close();
                                }
                            }
                        } finally {
                            prepared.close();
                        }
                    }
                    if (changeExpected && rowsAffected == 0) {
                        LogUtils.logWarning(getClass().getPackage().getName(),
                                "Statements expected to perform a change: \n" + StringUtils.join(sqlStatements, '\n'), null);
                        throw new NodeNotFoundException();
                    }
                    return generated;
                }
            });
        } catch (SQLException e) {
            String message = "Error performing update: " + e.getMessage();
            if ("23503".equals(e.getSQLState()))
                message = "dependant records exist (on deletion) or required records are missing (on update/insert)";
            else if ("23505".equals(e.getSQLState()))
                message = "property does not admit duplicates";
            throw new NodeStoreException(message, e);
        }

    }

    /**
     * Runs a runnable with a connection. Acquires/releases it automatically.
     */
    static <R> R runWithConnection(ConnectionProvider provider, ConnectionRunnable<R> runnable) throws SQLException {
        Connection connection = provider.acquireConnection();
        boolean success = false;
        try {
            R result = runnable.run(connection);
            success = true;
            return result;
        } finally {
            provider.releaseConnection(success);
        }
    }

    static final boolean DEBUG_SQL = Boolean.getBoolean("debug.sql");

    private Entity clazz;

    private JDBCNodeStoreCatalog catalog;

    private ConnectionProvider connectionProvider;

    private Map<String, Map<INodeKey, List<INodeKey>>> relatedNodeKeys = new LinkedHashMap<String, Map<INodeKey, List<INodeKey>>>();

    public JDBCNodeStore(JDBCNodeStoreCatalog catalog, ConnectionProvider connectionProvider, SchemaManagement schema, TypeRef typeRef) {
        Assert.isNotNull(typeRef);
        Assert.isNotNull(connectionProvider);
        Assert.isNotNull(schema);
        Assert.isNotNull(catalog);
        this.catalog = catalog;
        this.clazz = schema.getEntity(typeRef);
        Assert.isNotNull(clazz, typeRef.toString());
        this.connectionProvider = connectionProvider;
    }

    public void clearCaches() {
        relatedNodeKeys.clear();
    }

    @Override
    public boolean containsNode(INodeKey key) {
        return !loadMany(new LoadKeyHandler(), getGenerator().generateSelectOne(getStoreClass(), keyToId(key))).isEmpty();
    }

    @Override
    public INodeKey createNode(INode node) {
        List<INodeKey> result = perform(
                getGenerator().generateInsert(getStoreClass(), node.getProperties(), collectAllReferences(node), keyToId(node.getKey())),
                true, true);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public void deleteNode(INodeKey key) {
        perform(getGenerator().generateDelete(getStoreClass(), keyToId(key)), false, true);
    }

    @Override
    public INodeKey generateKey() {
        return this.basicGetCatalog().generateKey();
    }

    @Override
    public INodeStoreCatalog getCatalog() {
        return basicGetCatalog();
    }

    @Override
    public String getName() {
        return clazz.getTypeRef().toString();
    }

    @Override
    public INode getNode(final INodeKey key) {
        return loadOne(new LoadNodeHandler(), getGenerator().generateSelectOne(getStoreClass(), keyToId(key)).get(0));
    }

    @Override
    public Collection<INodeKey> getNodeKeys() {
        return loadMany(new LoadKeyHandler(), getGenerator().generateSelectAll(getStoreClass()));
    }

    @Override
    public Collection<INode> getNodes() {
        return loadMany(new LoadNodeHandler(), getGenerator().generateSelectAll(getStoreClass()));
    }

    @Override
    public Collection<INodeKey> getRelatedNodeKeys(INodeKey key, String relationship) {
        Map<INodeKey, List<INodeKey>> relationshipCache = relatedNodeKeys.get(relationship);
        if (relationshipCache == null)
            relatedNodeKeys.put(relationship, relationshipCache = new LinkedHashMap<INodeKey, List<INodeKey>>());
        List<INodeKey> nodeRelationshipCache = relationshipCache.get(key);
        if (nodeRelationshipCache != null)
            return nodeRelationshipCache;
        Relationship attribute = clazz.getRelationship(relationship);
        List<INodeKey> relatedNodeKeys = loadMany(new LoadKeyHandler(), getGenerator().generateSelectRelatedKeys(attribute, keyToId(key)));
        relationshipCache.put(key, relatedNodeKeys);
        return relatedNodeKeys;
    }

    @Override
    public Collection<INode> getRelatedNodes(INodeKey key, String relationship) {
        throw new UnsupportedOperationException();
    }

    public INodeKey idToKey(Long id) {
        return new IntegerKey(id);
    }

    @Override
    public void linkMultipleNodes(INodeKey key, String relationshipName, Collection<NodeReference> related) {
        Relationship relationship = clazz.getRelationship(relationshipName);
        if (related.isEmpty() && relationship.isRequired())
            throw new NodeStoreException("Relationship " + relationshipName + " is required");
        if (related.size() > 1 && !relationship.isMultiple())
            throw new NodeStoreException("Relationship " + relationshipName + " accepts only one related instance");
        perform(getGenerator().generateSetRelated(relationship, keyToId(key), collectReferences(related), true), false, true);
    }

    @Override
    public void linkNodes(INodeKey key, String relationshipName, NodeReference related) {
        Relationship relationship = clazz.getRelationship(relationshipName);
        if (related == null && relationship.isRequired())
            throw new NodeStoreException("Relationship " + relationshipName + " is required");
        perform(getGenerator().generateSetRelated(relationship, keyToId(key), Arrays.asList(keyToId(related.getKey())), false), false, true);
    }

    @Override
    public void unlinkNodes(INodeKey key, String relationship, NodeReference related) {
        perform(getGenerator().generateRemoveRelated(clazz.getRelationship(relationship), keyToId(key), keyToId(related.getKey())), false,
                true);
    }

    @Override
    public void updateNode(INode node) {
        perform(getGenerator().generateUpdate(getStoreClass(), node.getProperties(), collectAllReferences(node), keyToId(node.getKey())),
                false, true);
    }

    protected JDBCNodeStoreCatalog basicGetCatalog() {
        return catalog;
    }

    protected Long keyToId(INodeKey key) {
        return key == null ? null : ((IntegerKey) key).getInnerKey();
    }

    <T> T loadOne(IResultHandler<T> handler, String statement) {
        return JDBCNodeStore.loadOne(this.connectionProvider, handler, statement);
    }

    private Map<String, Collection<Long>> collectAllReferences(INode node) {
        Map<String, Collection<Long>> references = new HashMap<String, Collection<Long>>();
        for (Entry<String, Collection<NodeReference>> entry : node.getRelated().entrySet())
            references.put(entry.getKey(), collectReferences(entry.getValue()));
        return references;
    }

    private Collection<Long> collectReferences(Collection<NodeReference> nodeReferences) {
        Collection<Long> keys = new HashSet<Long>();
        for (NodeReference ref : nodeReferences)
            keys.add(keyToId(ref.getKey()));
        return keys;
    }

    private SQLGenerator getGenerator() {
        return basicGetCatalog().getGenerator();
    }

    private Entity getStoreClass() {
        return clazz;
    }

    private Object getValue(Property property, ResultSet rs) throws SQLException {
        String columnName = getGenerator().modelToSchemaName(property, false);
        int jdbcType = getGenerator().getJDBCType(property.getTypeRef());
        switch (jdbcType) {
        case Types.NUMERIC:
        case Types.BIGINT:
            return rs.getBigDecimal(columnName);
        case Types.VARCHAR:
            return rs.getString(columnName);
        case Types.DATE:
            Date sqlDate = rs.getDate(columnName);
            return sqlDate == null ? null : new java.util.Date(sqlDate.getTime());
        case Types.BOOLEAN:
            return rs.getBoolean(columnName);
        default:
            throw new IllegalArgumentException("Unexpected type: " + property.getName() + " : " + property.getTypeRef().getFullName()
                    + " (" + jdbcType + ")");
        }
    }

    private boolean isPersistable(Property property) {
        return isPersistableElement(property);
    }

    private boolean isPersistable(Relationship property) {
        return isPersistableElement(property);
    }

    private boolean isPersistableElement(DataElement property) {
        return !property.isDerived() && !property.isMultiple();
    }

    private <T> List<T> loadMany(IResultHandler<T> handler, List<String> generateSelectAll) {
        return JDBCNodeStore.loadMany(connectionProvider, handler, generateSelectAll);
    }

    private List<INodeKey> perform(List<String> statements, boolean returnKeys, boolean changeExpected) {
        return JDBCNodeStore.perform(connectionProvider, statements, returnKeys, changeExpected);
    }
}
