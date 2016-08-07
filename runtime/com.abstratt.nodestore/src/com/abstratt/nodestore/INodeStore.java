package com.abstratt.nodestore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Storage for hierarchical nodes. Services:
 * <ul>
 * <li>create
 * <li>update
 * <li>delete
 * <li>retrieve
 * <li>enumerate
 */
public interface INodeStore {
    public boolean containsNode(INodeKey key);

    /**
     * Creates a new root node, assigning a key to it, if it doesn't have one
     * yet. The node becomes part of the store, can be retrieved with
     * {@link #getNode(INodeKey)} etc.
     * 
     * @param node
     *            , which will have a key assigned
     * @return the key for the node created
     */

    public INodeKey createNode(INode node);

    /**
     * Deletes the root node with the given key.
     * 
     * @param key
     */
    public void deleteNode(INodeKey key);

    /**
     * Produces a new key.
     * 
     * @return the key generated
     */
    public INodeKey generateKey();

    public String getName();

    /**
     * Returns the entity with the given key.
     * 
     * @param key
     * @return
     */
    public INode getNode(INodeKey key);

    /**
     * Returns the keys of all root nodes.
     */
    public Collection<INodeKey> getNodeKeys();
    

    /**
     * Returns all root nodes.
     */
    public Collection<INode> getNodes();

    /**
     * Returns the keys of all related nodes.
     */
    public Collection<INodeKey> getRelatedNodeKeys(INodeKey key, String relationship, String relatedNodeStoreName);

    /**
     * Returns all related nodes.
     */
    public Collection<INode> getRelatedNodes(INodeKey key, String relationship, String relatedNodeStoreName);

    /**
     * Sets multiple related nodes.
     * 
     * @param key
     *            the target node
     * @param relationship
     *            the name of the relationship
     * @param related
     *            references to related nodes
     */
    public void linkMultipleNodes(INodeKey key, String relationship, Collection<NodeReference> related, boolean replace);

    public void linkNodes(INodeKey key, String relationship, NodeReference related);

    public void unlinkNodes(INodeKey key, String relationship, NodeReference related);

    /**
     * Updates an existing root node.
     * 
     * @param node
     */
    public void updateNode(INode node);

    public Collection<INodeKey> filter(Map<String, Collection<Object>> nodeCriteria, Integer limit);
    
    public default NodeReference getReference(INodeKey key) {
    	return new NodeReference(getName(), key);
    }
}
