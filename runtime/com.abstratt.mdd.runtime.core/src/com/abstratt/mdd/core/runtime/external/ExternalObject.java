package com.abstratt.mdd.core.runtime.external;

import com.abstratt.mdd.core.runtime.MetaClass;
import com.abstratt.mdd.core.runtime.types.BasicType;

/**
 * There is only one thing backing all external objects mapping to the same
 * class.
 *
 * Different classes will have different behaviors.
 *
 * There is only one delegate taking care of all external behaviors.
 *
 * The external object makes an external service a 1st class citizen and
 * provides context of the classifier it corresponds to. Not as important when
 * running operations (you could be ok with inferring the classifier from the
 * operation, but critical for signals).
 */
public class ExternalObject extends BasicType {
    public static final MetaClass<ExternalObject> META_CLASS = new ExternalMetaClass();
    private String classifierName;

    public ExternalObject(String classifierName, ExternalObjectDelegate delegate) {
        this.classifierName = classifierName;
    }

    @Override
    public String getClassifierName() {
        return classifierName;
    }

    @Override
    public MetaClass<ExternalObject> getMetaClass() {
        return ExternalObject.META_CLASS;
    }
}
