package com.abstratt.nodestore;

import java.util.Collection;

/**
 * An abstraction for a data persistence mechanism.
 */
public interface INodeStoreCatalog {
    public void abortTransaction();

    public void beginTransaction();

    public void commitTransaction();

    public boolean exists(NodeReference ref);

    public INode resolve(NodeReference ref);

    void clearCaches();

    INodeStore createStore(String name);

    void deleteStore(String name);

    String getName();

    /**
     * Returns an existing store. Returns <code>null</code> if not found.
     */
    INodeStore getStore(String name);

    boolean isInitialized();

    Collection<String> listStores();

    INode newNode(String nodeStoreName);

    void prime();

    void validateConstraints();

    void zap();
}
