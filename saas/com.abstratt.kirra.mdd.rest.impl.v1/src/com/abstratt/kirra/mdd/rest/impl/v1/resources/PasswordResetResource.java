package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import com.abstratt.kirra.mdd.rest.Activator;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class PasswordResetResource extends AbstractKirraRepositoryResource {

    @Post()
    public void reset(Representation repr) throws IOException {
        Form asForm = new Form(repr.getText());
        String username = asForm.getFirstValue("login");
        ResourceUtils.ensure(!StringUtils.isBlank(username), "'login' required", Status.CLIENT_ERROR_BAD_REQUEST);
        boolean result = Activator.getInstance().getAuthenticationService().resetPassword(username);
        ResourceUtils.ensure(result, "Could not reset password for user " + username, Status.CLIENT_ERROR_BAD_REQUEST);
    }

}
