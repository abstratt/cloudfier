package com.abstratt.mdd.target.jee

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractGenerator

import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*

class UserLoginServiceGenerator extends AbstractGenerator {
	
    new(IRepository repository) {
        super(repository)
    }

	def CharSequence generate() {
		val applicationName = KirraHelper.getApplicationName(repository)
		'''
package resource.«applicationName»;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import javax.ws.rs.ext.Provider;

import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.util.security.Password;

import «applicationName».*;
import userprofile.*;
import util.PersistenceHelper;
import util.SecurityHelper;

@Provider
public class UserLoginService extends MappedLoginService {

	@Override
	protected UserIdentity loadUser(String username) {
		Context jettyContext = ContextHandler.getCurrentContext();
		ServletContext servletContext = jettyContext.getContext(jettyContext.getServletContextName());
		UserProfile user = new UserProfileService().findByUsername(username);
		if (user == null)
			return null;
		List<String> roles = SecurityHelper.getRoles(user);
		return new DefaultUserIdentity(new Subject(), new KnownUser(username, new Password(user.getPassword())), roles.toArray(new String[0]));
	}

	@Override
	protected void loadUsers() throws IOException {
	}

}		
		'''
	}
}
