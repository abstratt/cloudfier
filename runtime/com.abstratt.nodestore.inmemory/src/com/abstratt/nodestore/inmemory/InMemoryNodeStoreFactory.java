package com.abstratt.nodestore.inmemory;

import com.abstratt.kirra.SchemaManagement;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.nodestore.INodeStoreFactory;

public class InMemoryNodeStoreFactory implements INodeStoreFactory {
    @Override
    public INodeStoreCatalog createCatalog(String name, Object repository) {
        return new InMemoryNodeStoreCatalog(name, (SchemaManagement) repository);
    }
}
