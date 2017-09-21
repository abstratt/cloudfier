package com.abstratt.blobstore;

public interface IBlobStoreCatalog {
    IBlobStore getBlobStore();
    void zap();
}
