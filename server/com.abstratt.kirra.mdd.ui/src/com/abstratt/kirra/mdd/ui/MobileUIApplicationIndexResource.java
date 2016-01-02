package com.abstratt.kirra.mdd.ui;

import org.restlet.data.Reference;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.abstratt.kirra.mdd.rest.KirraRESTUtils;
import com.abstratt.kirra.mdd.rest.KirraReferenceUtils;
import com.abstratt.mdd.frontend.web.ReferenceUtils;

/**
 * Provides access to an application session.
 */
public class MobileUIApplicationIndexResource extends ServerResource {

    @Get
    public void index() {
        Reference originalRef = KirraReferenceUtils.mapToExternal(getRequest(), getRequest().getOriginalRef());
        originalRef.setFragment(null);
        originalRef.setQuery(null);
        originalRef.setPath(KirraReferenceUtils.mapToExternal(getRequest(), getRequest().getRootRef()).getPath() + "/"
                + KirraRESTUtils.getWorkspaceFromProjectPath(getRequest()) + "/mobile/source/");
        redirectTemporary(originalRef);
    }
}
