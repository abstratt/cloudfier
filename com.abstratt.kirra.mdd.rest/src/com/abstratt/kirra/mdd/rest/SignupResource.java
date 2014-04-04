package com.abstratt.kirra.mdd.rest;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import com.abstratt.mdd.frontend.web.ResourceUtils;

public class SignupResource extends AbstractKirraRepositoryResource {
	
	@Post()
	public void signup(Representation repr) throws IOException {
		Form asForm = new Form(repr.getText());
		String username = asForm.getFirstValue("login");
		String password = asForm.getFirstValue("password");
		ResourceUtils.ensure(!StringUtils.isBlank(username), "'login' required", Status.CLIENT_ERROR_BAD_REQUEST);
		ResourceUtils.ensure(!StringUtils.isBlank(password), "'password' required", Status.CLIENT_ERROR_BAD_REQUEST);
		boolean result = Activator.getInstance().getAuthenticationService().createUser(username, password);
		ResourceUtils.ensure(result, "Could not provision user " + username, Status.CLIENT_ERROR_BAD_REQUEST);
	}

}
