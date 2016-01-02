package com.abstratt.kirra.mdd.rest;

import org.restlet.Request;
import org.restlet.data.Reference;

import com.abstratt.mdd.frontend.web.ReferenceUtils;

public class KirraReferenceUtils {
    public static Reference getBaseReference(Request request, Reference reference) {
        return ReferenceUtils.getBaseReference(request, reference);
    }

    /**
     * Returns a reference to the application base URI (up to the workspace
     * name).
     * 
     * @return
     */
    public static Reference getBaseReference(Request request, Reference reference, String segment) {
        return ReferenceUtils.getBaseReference(request, reference, segment);
    }

    /**
     * Attempts to map the given internal reference to an external reference.
     */
    public static Reference mapToExternal(Request request, Reference internal) {
		return ReferenceUtils.mapToExternal(request, internal);
    }

}
