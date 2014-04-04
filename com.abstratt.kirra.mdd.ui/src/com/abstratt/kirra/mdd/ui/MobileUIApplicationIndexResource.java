package com.abstratt.kirra.mdd.ui;

import org.restlet.data.Reference;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.abstratt.kirra.mdd.rest.KirraRESTUtils;
import com.abstratt.mdd.frontend.web.ReferenceUtils;

/**
 * Provides access to an application session.
 */
public class MobileUIApplicationIndexResource extends ServerResource {
	
	@Get
	public void index() {
		Reference originalRef = ReferenceUtils.getExternal(getRequest().getOriginalRef());
		originalRef.setFragment(null);
		originalRef.setQuery(null);
		originalRef.setPath(ReferenceUtils.getExternal(getRequest().getRootRef()).getPath() + "/" + KirraRESTUtils.getWorkspaceFromProjectPath(getRequest()) + "/mobile/source/");
		redirectTemporary(originalRef);
	} 
}
