package com.abstratt.nodestore.jdbc;

import com.abstratt.kirra.SchemaManagement;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.nodestore.INodeStoreFactory;

public class JDBCNodeStoreFactory implements INodeStoreFactory {
	@Override
	public INodeStoreCatalog createCatalog(String name, Object repository) {
		return new JDBCNodeStoreCatalog(name, (SchemaManagement) repository);
	}
}
