package com.abstratt.nodestore.inmemory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.abstratt.nodestore.BasicNode;
import com.abstratt.nodestore.INode;
import com.abstratt.nodestore.INodeKey;
import com.abstratt.nodestore.INodeStore;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.nodestore.NodeReference;

/**
 * A catalog maps to a database+schema.
 */
public class InMemoryNodeStoreCatalog implements INodeStoreCatalog {
	
	protected static final File REPOSITORY_ROOT = new File(new File(System.getProperty("nodestore.file.base", System.getProperty("user.dir", "."))), "nodestore");

    private SchemaManagement metadata;

    private String catalogName;

    private Map<String, Map<String, InMemoryNodeStore>> storeSet = new LinkedHashMap<>();

	private boolean readOnly;

	private String environment;

    public InMemoryNodeStoreCatalog(String name, SchemaManagement schema) {
        Validate.isTrue(schema != null);
        this.catalogName = name;
        this.metadata = schema;
        if (name.indexOf('-') == -1)
        	new Throwable("Creating node store catalog as " + name).printStackTrace(System.out);
    }
    
    SchemaManagement getMetadata() {
		return metadata;
	}
    
    @Override
    public void clearCaches() {
    }

    @Override
    public void commitTransaction() {
    	getStoreSet().values().forEach(it -> InMemoryNodeStore.save(it));
    }
    
    public Map<String, InMemoryNodeStore> getStoreSet() {
		return storeSet.computeIfAbsent(environment, e -> new LinkedHashMap<>());
	}
    
    @Override
    public void beginTransaction() {
    	try {
			FileUtils.forceMkdir(getCatalogPath());
			System.out.println("beginTransaction: " + getCatalogPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    	getStoreSet().clear();
    }
    
    @Override
    public void abortTransaction() {
    	getStoreSet().clear();
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
        return ((InMemoryNodeStore) getStore(ref.getStoreName())).basicGetNode(ref.getKey()) != null;
    }

    public INodeKey generateKey(String storeName) {
        return getStore(storeName).generateKey();
    }

    @Override
    public String getName() {
        return catalogName;
    }

    public InMemoryNodeStore getStore(TypeRef typeRef) {
    	String storeName = typeRef.getFullName();
    	return (InMemoryNodeStore) getStore(storeName);
    }

	protected File getStorePath(TypeRef typeRef) {
		File storePath = new File(getCatalogPath(), typeRef.getFullName());
		return storePath;
	}

	private File getCatalogPath() {
		return new File(new File(REPOSITORY_ROOT, environment), catalogName);
	}
    
    @Override
    public INodeStore getStore(String storeName) {
    	String sanitizedStoreName = TypeRef.sanitize(storeName);
    	InMemoryNodeStore store = getStoreSet().get(sanitizedStoreName);
    	if (store == null) {
    		store = loadStore(sanitizedStoreName);
    		getStoreSet().put(sanitizedStoreName, store);
    	}
		return store;
    }

	private InMemoryNodeStore loadStore(String storeName) {
		return InMemoryNodeStore.load(this, new TypeRef(storeName, TypeKind.Entity));
	}

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public Collection<String> listStores() {
    	return metadata.getEntityNames().stream().map(it -> it.getFullName()).collect(Collectors.toSet());
    }

    @Override
    public INode newNode(String nodeStoreName) {
        return new BasicNode(generateKey(nodeStoreName));
    }

    @Override
    public void prime() {
    	zap();
    }

    @Override
    public INode resolve(NodeReference ref) {
    	INode resolved = Optional.ofNullable(getStore(ref.getStoreName()).getNode(ref.getKey())).map(it -> it.clone()).orElse(null);
		return resolved;
    }

    @Override
    public void validateConstraints() {
        for (Entity entity : this.metadata.getAllEntities()) {
			InMemoryNodeStore store = getStore(entity.getTypeRef());
			if (store == null)
				System.out.println("No store found for " + entity.getTypeRef());
			else
				store.validateConstraints();
		}
    }

    @Override
    public void zap() {
        // zap should not require metadata (repository may not be available)
    	FileUtils.deleteQuietly(getCatalogPath());
    	getStoreSet().clear();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

	public void setEnvironment(String environment) {
		this.environment = environment;
	}
}
