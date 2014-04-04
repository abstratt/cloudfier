package com.abstratt.nodestore;

import java.util.Collection;

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
	public INodeStoreCatalog getCatalog();
	
	public String getName();
	
	/**
	 * Returns the entity with the given key.
	 * 
	 * @param key
	 * @return
	 */
	public INode getNode(INodeKey key);
	
	public boolean containsNode(INodeKey key);
	
	/**
	 * Creates a new root node, assigning a key to it, if it doesn't have one yet. 
	 * The node becomes part of the store, can be retrieved with {@link #getNode(INodeKey)} etc.
	 * 
	 * @param node, which will have a key assigned
	 * @return the key for the node created
	 */
	
	public INodeKey createNode(INode node);

	/**
	 * Produces a new key.
	 * 
	 * @return the key generated
	 */
	public INodeKey generateKey();
	/**
	 * Updates an existing root node.
	 * 
	 * @param node
	 */
	public void updateNode(INode node);

	/**
	 * Deletes the root node with the given key.
	 * 
	 * @param key
	 */
	public void deleteNode(INodeKey key);

	/**
	 * Returns all root nodes.
	 */
	public Collection<INode> getNodes();
	/**
	 * Returns the keys of all root nodes.
	 */
	public Collection<INodeKey> getNodeKeys();
	
	/**
	 * Returns all related nodes.
	 */
	public Collection<INode> getRelatedNodes(INodeKey key, String relationship);
	
	/**
	 * Returns the keys of all related nodes.
	 */
	public Collection<INodeKey> getRelatedNodeKeys(INodeKey key, String relationship);

	/**
	 * Sets multiple related nodes.
	 * 
	 * @param key the target node
	 * @param relationship the name of the relationship
	 * @param related references to related nodes
	 */
	public void linkMultipleNodes(INodeKey key, String relationship, Collection<NodeReference> related);
	public void linkNodes(INodeKey key, String relationship, NodeReference related);
	public void unlinkNodes(INodeKey key, String relationship, NodeReference related);
}
