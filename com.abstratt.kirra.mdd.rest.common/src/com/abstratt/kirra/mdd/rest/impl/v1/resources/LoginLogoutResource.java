package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.abstratt.kirra.mdd.rest.impl.v1.LegacyKirraMDDRestletApplication;
import com.abstratt.pluginutils.LogUtils;

public class LoginLogoutResource extends AbstractKirraRepositoryResource {
    @Post
    public void login(Representation repr) {
        LogUtils.debug(LegacyKirraMDDRestletApplication.ID, "Logging in");
    }

    @Get
    public void logout() {
        LogUtils.debug(LegacyKirraMDDRestletApplication.ID, "Logging out");
    }
}
