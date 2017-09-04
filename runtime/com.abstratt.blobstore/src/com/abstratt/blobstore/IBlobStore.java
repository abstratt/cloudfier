package com.abstratt.blobstore;

import java.io.InputStream;

public interface IBlobStore {
    BlobMetadata addBlob(String originalName, String contentType) throws BlobStoreException;
    InputStream getContents(String token) throws BlobStoreException;
    BlobMetadata setContents(String token, InputStream contents) throws BlobStoreException;
    BlobMetadata getMetadata(String token) throws BlobStoreException;
    void deleteBlob(String token) throws BlobStoreException;
}
