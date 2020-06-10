package com.abstratt.mdd.core.runtime.types;

import java.util.Collections;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class PictureType extends BlobType {
    private static final long serialVersionUID = 1L;

    public PictureType(BlobInfo value) {
        super(value);
    }
    
	@Override
	public String getClassifierName() {
		return "mdd_media::Picture";
	}
	
    public static PictureType empty(@SuppressWarnings("unused") ExecutionContext context) {
        return new PictureType(new BlobInfo(Collections.emptyMap()));
    }
}