package com.abstratt.nodestore;

import java.util.Collection;

/**
 * An abstraction for a data persistence mechanism.
 */
public interface INodeStoreCatalog {
	String getName();
	
	INodeStore createStore(String name);
	
	/**
	 * Returns an existing store. Returns <code>null</code> if not found.
	 */
	INodeStore getStore(String name);

	void deleteStore(String name);

	INode newNode();

	Collection<String> listStores();

	public INode resolve(NodeReference ref);

	public boolean exists(NodeReference ref);
	
	public void beginTransaction();
	
	public void commitTransaction();
	
	public void abortTransaction();

	void zap();

	void prime();

	boolean isInitialized();

	void validateConstraints();

	void clearCaches();
}
