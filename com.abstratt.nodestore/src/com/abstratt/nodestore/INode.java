package com.abstratt.nodestore;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Nodes are hierarchical. Root nodes have keys for direct retrieval. 
 * Values can be null, primitives, {@link Date}, or node keys (for cross referencing). 
 */
public interface INode {
	
	/**
	 * Returns this node's key. The key is required for retrieving specific instances later.
	 * 
	 * @return this nodes key, or <code>null</code> if this node has never been saved
	 */
	public INodeKey getKey();
	public Map<String, Object> getProperties();
	/**
	 * Sets this node's values.
	 * 
	 * @param values
	 */
	public void setProperties(Map<String, Object> values);
	public Map<String, Collection<INode>> getChildren();
	public void setChildren(Map<String, Collection<INode>> children);
	public INode getParent();
	public Map<String, Collection<NodeReference>> getRelated();
	public void setRelated(Map<String, Collection<NodeReference>> related);
	public boolean isTopLevel();
	public boolean isPropertySet(String nodeProperty);
	public Object getProperties(boolean readonly);
}