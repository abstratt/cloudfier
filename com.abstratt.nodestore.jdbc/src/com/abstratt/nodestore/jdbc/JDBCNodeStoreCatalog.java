package com.abstratt.nodestore.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.Assert;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.abstratt.nodestore.BasicNode;
import com.abstratt.nodestore.INode;
import com.abstratt.nodestore.INodeKey;
import com.abstratt.nodestore.INodeStore;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.nodestore.NodeReference;
import com.abstratt.nodestore.NodeStoreException;
import com.abstratt.nodestore.NodeStoreNotFoundException;
import com.abstratt.nodestore.NodeStoreValidationException;
import com.abstratt.nodestore.jdbc.JDBCNodeStore.ConnectionRunnable;

/**
 * A catalog maps to a database+schema.
 */
public class JDBCNodeStoreCatalog implements INodeStoreCatalog {

    private SchemaManagement metadata;

    private String name;

    private SQLGenerator generator;

    private ConnectionProvider connectionProvider;

    private Map<String, JDBCNodeStore> stores = new LinkedHashMap<String, JDBCNodeStore>();

    public JDBCNodeStoreCatalog(String name, SchemaManagement schema) {
        Assert.isNotNull(schema);
        this.name = name;
        this.metadata = schema;
        this.generator = new SQLGenerator(name, schema);
        this.connectionProvider = new ConnectionProvider();
    }

    @Override
    public void abortTransaction() {
        if (!connectionProvider.hasConnection())
            return;
        try {
            connectionProvider.releaseConnection(false);
        } catch (SQLException e) {
            throw new NodeStoreException("Error rolling back changes: " + e.getMessage());
        }
    }

    @Override
    public void beginTransaction() {
        try {
            connectionProvider.acquireConnection();
        } catch (SQLException e) {
            if ("3D000".equals(e.getSQLState()))
                throw new NodeStoreNotFoundException();
            throw new NodeStoreException("Could not acquire connection", e);
        }
    }

    public void clearCache() {
        this.stores = new LinkedHashMap<String, JDBCNodeStore>();
    }

    @Override
    public void clearCaches() {
        for (JDBCNodeStore cached : stores.values())
            cached.clearCaches();
    }

    @Override
    public void commitTransaction() {
        try {
            connectionProvider.releaseConnection(true);
        } catch (SQLException e) {
            throw new NodeStoreException("Error committing changes: " + e.getMessage());
        }
    }

    @Override
    public INodeStore createStore(String name) {
        return getStore(name);
    }

    @Override
    public void deleteStore(String name) {
        // nothing to do, no use case requires this
    }

    @Override
    public boolean exists(NodeReference ref) {
        return getStore(ref.getStoreName()).getNode(ref.getKey()) != null;
    }

    public INodeKey generateKey() {
        return JDBCNodeStore.loadOne(connectionProvider, new JDBCNodeStore.LoadKeyHandler(), generator.generateGetSequence());
    }

    public SQLGenerator getGenerator() {
        return generator;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public INodeStore getStore(String name) {
        JDBCNodeStore existing = stores.get(name);
        if (existing != null)
            return existing;
        JDBCNodeStore newStore = new JDBCNodeStore(this, getConnectionProvider(), metadata, new TypeRef(name, TypeKind.Entity));
        stores.put(name, newStore);
        return newStore;
    }

    @Override
    public boolean isInitialized() {
        final Collection<String> allPackages = findAllPackages();
        final Set<String> missing = new HashSet<String>(allPackages);
        try {
            JDBCNodeStore.runWithConnection(connectionProvider, new ConnectionRunnable<Object>() {
                @Override
                public Object run(Connection connection) throws SQLException {
                    for (String pkg : allPackages) {
                        DatabaseMetaData dbMetadata = connection.getMetaData();
                        ResultSet schemas = dbMetadata.getSchemas(null, generator.modelToSchemaName(pkg));
                        try {
                            if (schemas.next())
                                missing.remove(pkg);
                        } finally {
                            schemas.close();
                        }
                    }
                    return null;
                }
            });
        } catch (SQLException e) {
            // don't sweat it
        }
        return missing.isEmpty();
    }

    @Override
    public Collection<String> listStores() {
        Collection<String> entityNames = new TreeSet<String>();
        for (TypeRef typeRef : metadata.getEntityNames())
            entityNames.add(typeRef.getFullName());
        return entityNames;
    }

    @Override
    public INode newNode() {
        return new BasicNode(generateKey());
    }

    @Override
    public void prime() {
        try {
            JDBCNodeStore.perform(connectionProvider, generator.generateDropSchema(false), false, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JDBCNodeStore.perform(connectionProvider, generator.generateCreateSchema(), false, false);
        for (String pkg : findAllPackages())
            JDBCNodeStore.perform(connectionProvider, generator.generateCreateTables(pkg), false, false);
    }

    @Override
    public INode resolve(NodeReference ref) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void validateConstraints() {
        for (Entity entity : this.metadata.getAllEntities()) {
            List<Relationship> relationships = entity.getRelationships();
            for (Relationship relationship : relationships) {
                List<String> stmts = generator.generateValidate(relationship);
                for (String statement : stmts) {
                    Number count = JDBCNodeStore
                            .<Number> loadOne(connectionProvider, new JDBCNodeStore.LoadSingleValueHandler(), statement);
                    if (count != null && count.longValue() > 0)
                        throw new NodeStoreValidationException("Relationship " + relationship.getLabel() + " (from " + entity.getLabel()
                                + ") failed validation");
                }
            }
        }
    }

    @Override
    public void zap() {
        // zap should not require metadata (repository may not be available)
        JDBCNodeStore.perform(connectionProvider, generator.generateDropSchema(false), false, false);
    }

    protected SQLGenerator newSQLGenerator() {
        return new SQLGenerator(getName(), metadata);
    }

    private Collection<String> findAllPackages() {
        return metadata.getNamespaces();
    }

    private ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }
}
