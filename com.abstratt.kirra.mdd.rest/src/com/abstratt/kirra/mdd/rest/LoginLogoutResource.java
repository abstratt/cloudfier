package com.abstratt.kirra.mdd.rest;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.abstratt.pluginutils.LogUtils;

public class LoginLogoutResource extends AbstractKirraRepositoryResource {
	@Get
	public void logout() {
		LogUtils.debug(LegacyKirraMDDRestletApplication.ID, "Logging out");
	}
	
	@Post
	public void login(Representation repr) {
		LogUtils.debug(LegacyKirraMDDRestletApplication.ID, "Logging in");
	}
}
