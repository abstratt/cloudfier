package com.abstratt.nodestore;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Nodes are hierarchical. Root nodes have keys for direct retrieval. Values can
 * be null, primitives, {@link Date}, or node keys (for cross referencing).
 */
public interface INode {

    public Map<String, Collection<INode>> getChildren();

    /**
     * Returns this node's key. The key is required for retrieving specific
     * instances later.
     * 
     * @return this nodes key, or <code>null</code> if this node has never been
     *         saved
     */
    public INodeKey getKey();

    public INode getParent();

    public Map<String, Object> getProperties();

    public Object getProperties(boolean readonly);

    public Map<String, Collection<NodeReference>> getRelated();

    public boolean isPropertySet(String nodeProperty);

    public boolean isTopLevel();

    public void setChildren(Map<String, Collection<INode>> children);

    /**
     * Sets this node's values.
     * 
     * @param values
     */
    public void setProperties(Map<String, Object> values);

    public void setRelated(Map<String, Collection<NodeReference>> related);
}