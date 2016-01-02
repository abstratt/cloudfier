package com.abstratt.mdd.frontend.web;

import java.util.List;

import org.restlet.Request;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.util.NamedValue;
import org.restlet.util.Series;

public class ReferenceUtils {
    // something like http://foobar/barfoo/ (internal endpoints are
    // http://localhost:8090/services/)
    private static final String EXTERNAL_BASE_PROPERTY = "cloudfier.api.externalBaseUri";
    private static final String INTERNAL_BASE_PROPERTY = "cloudfier.api.internalBaseUri";
    public static final String INTERNAL_BASE = System.getProperty(ReferenceUtils.INTERNAL_BASE_PROPERTY, "http://localhost/services/");
    public static final String EXTERNAL_BASE = System.getProperty(ReferenceUtils.EXTERNAL_BASE_PROPERTY, ReferenceUtils.INTERNAL_BASE);

    
    public static Reference getBaseReference(Request request, Reference reference) {
        return getBaseReference(request, reference, Paths.API);
    }

    /**
     * Returns a reference to the application base URI (up to the workspace
     * name).
     * 
     * @return
     */
    public static Reference getBaseReference(Request request, Reference reference, String segment) {
        List<String> segments = reference.getSegments();
        int sessionIndex = segments.indexOf(segment);
        int segmentCount = segments.size();
        if ("".equals(segments.get(segmentCount - 1)))
            segmentCount--;
        for (int currentSegment = segmentCount - 1; currentSegment > sessionIndex + 1; currentSegment--)
            reference = reference.getParentRef();
        return mapToExternal(request, reference);
    }

    /**
     * Attempts to map the given internal reference to an external reference.
     */
    public static Reference mapToExternal(Request request, Reference internal) {
        if (internal == null)
            return null;
        Series<NamedValue<String>> httpHeaders = (Series<NamedValue<String>>) request.getAttributes()
                .get(HeaderConstants.ATTRIBUTE_HEADERS);
        String proxyProto = httpHeaders.getFirstValue("X-Forwarded-Proto");
        String proxyHost = httpHeaders.getFirstValue("X-Forwarded-Host");
        Reference external = internal.clone();
        if (proxyProto != null) 
    		external.setProtocol(Protocol.valueOf(proxyProto));
        if (proxyHost != null) 
    		external.setHostDomain(proxyHost);
		return external;
    }

}
