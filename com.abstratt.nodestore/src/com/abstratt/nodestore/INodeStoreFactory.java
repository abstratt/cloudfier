package com.abstratt.nodestore;


/**
 * An abstraction for a data persistence mechanism.
 */
public interface INodeStoreFactory {
	INodeStoreCatalog createCatalog(String name, Object settings);
}
