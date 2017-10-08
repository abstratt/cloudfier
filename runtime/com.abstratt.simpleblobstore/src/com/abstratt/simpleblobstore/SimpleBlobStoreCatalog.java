package com.abstratt.simpleblobstore;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

import com.abstratt.blobstore.BlobStoreException;
import com.abstratt.blobstore.IBlobStore;
import com.abstratt.blobstore.IBlobStoreCatalog;

public class SimpleBlobStoreCatalog implements IBlobStoreCatalog {
    
    static final String BLOBSTORE_FILE_BASE_KEY = "blobstore.file.base";

    private static final String BLOBSTORE_FILE_BASE = System.getProperty(BLOBSTORE_FILE_BASE_KEY, System.getProperty("java.io.tmpdir"));

    protected static final Path REPOSITORY_ROOT = computeRepositoryDataRoot();

    private static Path computeRepositoryDataRoot() {
        if (BLOBSTORE_FILE_BASE == null)
            return null;
        return Paths.get(BLOBSTORE_FILE_BASE);
    }

    private String environment;

    private String catalogName;
    public SimpleBlobStoreCatalog(String catalogName) {
        this.catalogName = catalogName;
    }
    
    @Override
    public void zap() {
        try {
            FileUtils.deleteDirectory(getBasePath().toFile());
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
    }
    
    @Override
    public IBlobStore getBlobStore() {
        return new SimpleBlobStore(getBasePath());
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    private Path getBasePath() {
        return REPOSITORY_ROOT.resolve(environment).resolve(catalogName);
    }

}
