package com.abstratt.blobstore;

public class BlobMetadata {
    private String token;
    private String originalName;
    private String contentType;
    private long contentLength;
    public BlobMetadata(String token, String originalName, String contentType, long contentLength) {
        this.token = token;
        this.originalName = originalName;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }
    public BlobMetadata() {
    }
    
    public String getToken() {
        return token;
    }
    public String getOriginalName() {
        return originalName;
    }
    public String getContentType() {
        return contentType;
    }
    public long getContentLength() {
        return contentLength;
    }
}