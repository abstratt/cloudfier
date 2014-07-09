package com.abstratt.mdd.frontend.web;

import org.restlet.data.Reference;

public class ReferenceUtils {
    public static Reference getExternal(Reference internal) {
        return ReferenceUtils.getExternal(internal, ReferenceUtils.EXTERNAL_BASE);
    }

    public static Reference getExternal(Reference internal, String externalBase) {
        if (internal == null)
            return null;
        internal = internal.clone();
        String internalAsString = internal.toString();
        if (!internalAsString.startsWith(ReferenceUtils.INTERNAL_BASE))
            return internal;
        return new Reference(internalAsString.replace(ReferenceUtils.INTERNAL_BASE, externalBase));
    }

    // something like http://foobar/barfoo/ (internal endpoints are
    // http://localhost:8090/mdd/)
    public static final String EXTERNAL_BASE_PROPERTY = "cloudfier.api.externalBaseUri";
    public static final String INTERNAL_BASE_PROPERTY = "cloudfier.api.internalBaseUri";
    public static final String INTERNAL_BASE = System.getProperty(ReferenceUtils.INTERNAL_BASE_PROPERTY, "http://localhost/mdd/");
    public static final String EXTERNAL_BASE = System.getProperty(ReferenceUtils.EXTERNAL_BASE_PROPERTY, ReferenceUtils.INTERNAL_BASE);

}
