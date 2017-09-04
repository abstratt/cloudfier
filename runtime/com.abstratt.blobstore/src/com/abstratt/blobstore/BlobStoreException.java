package com.abstratt.blobstore;

public class BlobStoreException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public BlobStoreException(String message) {
        super(message);
    }
    public BlobStoreException(Exception e) {
        super(e);
    }
}