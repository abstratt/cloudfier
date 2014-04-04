package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.MetaClass;

public abstract class BuiltInClass extends BasicType {
    @Override
    public MetaClass getMetaClass() {
        return BuiltInMetaClass.findBuiltInClass(getClassifierName());
    }
}
