package com.abstratt.mdd.core.runtime.types;

public class BlobType extends PrimitiveType<BlobInfo> {
	private static final long serialVersionUID = 1L;

	protected BlobType(BlobInfo value) {
		super(value);
	}
	
	@Override
	public String getClassifierName() {
		return "mdd_media::Blob";
	}
}
