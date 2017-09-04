package com.abstratt.mdd.core.runtime.types;

import java.util.Map;

public final class BlobInfo {

	private final String token;
	private final long contentLength;
	private final String contentType;
	private final String originalName;

	public BlobInfo(String token, String contentType, String originalName, long contentLength) {
		this.token = token;
		this.contentType = contentType;
		this.originalName = originalName;
		this.contentLength = contentLength;
	}	

//	public BlobInfo(Object blobMetadata) {
//	    this(blobMetadata.getToken(), blobMetadata.getContentType(), blobMetadata.getOriginalName(), blobMetadata.getContentLength());
//	}
//	
	public BlobInfo(Map<String, Object> asMap) {
		this.token = (String) asMap.get("token");
		this.contentType = (String) asMap.get("contentType");
		this.originalName = (String) asMap.get("originalName");
		this.contentLength = ((Number) asMap.getOrDefault("contentLength", -1L)).longValue();
	}

	public String getContentType() {
		return contentType;
	}
	
	public String getOriginalName() {
		return originalName;
	}

	public long getContentLength() {
		return contentLength;
	}
	
	public String getToken() {
		return token;
	}
}
