package com.abstratt.nodestore.inmemory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.abstratt.pluginutils.LogUtils;

/**
 * A catalog maps to a database+schema.
 */
public class InMemoryNodeStoreCatalog implements INodeStoreCatalog {
	
	static final String NODESTORE_FILE_BASE_KEY = "nodestore.file.base";

	private static final String NODESTORE_FILE_BASE = System.getProperty(NODESTORE_FILE_BASE_KEY);

	protected static final File REPOSITORY_ROOT = computeRepositoryDataRoot();

	private static File computeRepositoryDataRoot() {
		if (NODESTORE_FILE_BASE == null)
			return null;
		return new File(NODESTORE_FILE_BASE);
	}

    private SchemaManagement metadata;

    private String catalogName;

    private Map<String, Map<String, InMemoryNodeStore>> storeSet = Collections.synchronizedMap(new LinkedHashMap<>());

	private boolean readOnly;

	private String environment;

    public InMemoryNodeStoreCatalog(String name, SchemaManagement schema) {
        Validate.isTrue(schema != null);
        this.catalogName = name;
        this.metadata = schema;
    }
    
    SchemaManagement getMetadata() {
		return metadata;
	}
    
    @Override
    public void clearCaches() {
        List<String> clean = getStoreSet().entrySet().stream().filter(it -> !it.getValue().isDirty()).map(it -> it.getKey()).collect(Collectors.toList());
        clean.forEach(it -> getStoreSet().remove(it));
    }

    @Override
    public void commitTransaction() {
        System.out.println("commitTransaction: " + getCatalogPath());
		getStoreSet().values().forEach(it -> it.save());
    }
    
	public Map<String, InMemoryNodeStore> getStoreSet() {
		return storeSet.computeIfAbsent(environment, e -> 
			new LinkedHashMap<>()
		);
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
        System.out.println("abortTransaction: " + getCatalogPath());
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
    public synchronized INodeStore getStore(String storeName) {
    	String sanitizedStoreName = TypeRef.sanitize(storeName);
    	Map<String, InMemoryNodeStore> environmentStoreSet = getStoreSet();
		InMemoryNodeStore store = environmentStoreSet.get(sanitizedStoreName);
    	if (store == null) {
    		store = loadStore(sanitizedStoreName);
    		environmentStoreSet.put(sanitizedStoreName, store);
    	}
		return store;
    }

	private InMemoryNodeStore loadStore(String storeName) {
		LogUtils.debug(InMemoryNodeStoreActivator.BUNDLE_NAME, "Loading store " + storeName + " from " + this);
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
        return new BasicNode(nodeStoreName, generateKey(nodeStoreName));
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
