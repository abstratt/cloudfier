package com.abstratt.mdd.core.runtime.types;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public final class BlobInfo {

	private final String token;
	private final long contentLength;
	private final String contents;
	private final String contentType;
	private final String originalName;

	public BlobInfo(String contentType, String originalName, byte[] contents) {
		this(UUID.randomUUID().toString(), contentType, originalName, contents);
	}
	
	public BlobInfo(String token, String contentType, String originalName, byte[] contents) {
		this.token = token;
		this.contentType = contentType;
		this.originalName = originalName;
		this.contents = Base64.getEncoder().encodeToString(contents);
		this.contentLength = contents.length;
	}
	
	public BlobInfo(String token, String contentType, String originalName, long contentLength) {
		this.token = token;
		this.contentType = contentType;
		this.originalName = originalName;
		this.contents = null;
		this.contentLength = contentLength;
	}	

	public BlobInfo(Map<String, Object> asMap) {
		this.token = (String) asMap.get("token");
		this.contentType = (String) asMap.get("contentType");
		this.originalName = (String) asMap.get("originalName");
		this.contentLength = ((Number) asMap.getOrDefault("contentLength", -1L)).longValue();
		Object mapContents = asMap.get("contents"); 
		if (mapContents instanceof byte[])
			this.contents = Base64.getEncoder().encodeToString((byte[]) mapContents);
		else if (mapContents instanceof String)
			this.contents = (String) mapContents;
		else
			this.contents = null;
	}

	public String getContentType() {
		return contentType;
	}
	
	public String getOriginalName() {
		return originalName;
	}
	
	public String getContents() {
		return contents;
	}
	
	public long getContentLength() {
		return contentLength;
	}
	
	public String getToken() {
		return token;
	}
	
	public byte[] getContentsAsBytes() {
		return this.contents == null ? null : Base64.getDecoder().decode(this.contents);
	}
}
