package resource.{applicationName};

import java.io.IOException;

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

import {applicationName}.*;
import util.PersistenceHelper;

@Provider
public class UserLoginService extends MappedLoginService {

	@Override
	protected UserIdentity loadUser(String username) {
		Context jettyContext = ContextHandler.getCurrentContext();
		ServletContext servletContext = jettyContext.getContext(jettyContext.getServletContextName());
		EntityManagerFactory emf = (EntityManagerFactory) servletContext.getAttribute("emf");
		PersistenceHelper.setEntityManager(emf.createEntityManager());
		try {
			User user = new UserService().findByUsername(username);
			if (user == null)
				return null;
			return new DefaultUserIdentity(new Subject(), new KnownUser(username, new Password(user.getPassword())), new String[] { {userRoleNames} });
		} finally {
			PersistenceHelper.setEntityManager(null);
		}
	}

	@Override
	protected void loadUsers() throws IOException {
	}

}