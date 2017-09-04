package com.abstratt.blobstore;

public interface IBlobStoreCatalog {
    IBlobStore getBlobStore(String namespace);
    void zap();
}
