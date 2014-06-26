package com.abstratt.kirra.mdd.rest2;

import java.util.List;

import org.restlet.Request;
import org.restlet.data.Reference;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.util.NamedValue;
import org.restlet.util.Series;

import com.abstratt.mdd.frontend.web.ReferenceUtils;

public class KirraReferenceUtils {
    public static Reference getBaseReference(Request request, Reference reference) {
        return KirraReferenceUtils.getBaseReference(request, reference, com.abstratt.mdd.frontend.web.Paths.API);
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
        return KirraReferenceUtils.mapToExternal(request, reference);
    }

    /**
     * Attempts to map the given internal reference to an external reference.
     */
    public static Reference mapToExternal(Request request, Reference reference) {
        Series<NamedValue<String>> httpHeaders = (Series<NamedValue<String>>) request.getAttributes()
                .get(HeaderConstants.ATTRIBUTE_HEADERS);
        String proxiedHeader = httpHeaders.getFirstValue("X-Kirra-Proxied");
        if ("true".equals(proxiedHeader)) {
            String applicationBaseUri = KirraRESTUtils.getRepository().getProperties().getProperty("mdd.api.rest.base");
            if (applicationBaseUri != null)
                return ReferenceUtils.getExternal(reference, applicationBaseUri);
        }
        return ReferenceUtils.getExternal(reference);
    }
}
