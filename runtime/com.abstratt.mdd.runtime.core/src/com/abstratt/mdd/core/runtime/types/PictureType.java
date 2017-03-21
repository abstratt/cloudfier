package com.abstratt.mdd.core.runtime.types;

public class PictureType extends BlobType {
    private static final long serialVersionUID = 1L;

    public PictureType(BlobInfo value) {
        super(value);
    }

	@Override
	public String getClassifierName() {
		return "mdd_media::Picture";
	}
}