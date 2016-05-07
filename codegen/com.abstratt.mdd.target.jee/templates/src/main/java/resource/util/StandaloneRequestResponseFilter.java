package resource.util;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Priority;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;

import {applicationName}.*;

import resource.expenses.EntityResource;
import resource.expenses.IndexResource;
import userprofile.Profile;
import userprofile.ProfileService;
import util.PersistenceHelper;
import util.SecurityHelper;

@Priority(Priorities.AUTHORIZATION)
public class StandaloneRequestResponseFilter implements ContainerRequestFilter, ContainerResponseFilter {
	private static final Collection<String> MUTATION_METHODS = Arrays.asList("PUT", "POST", "DELETE");
	private EntityManagerFactory entityManagerFactory;

	public StandaloneRequestResponseFilter() {
		entityManagerFactory = Persistence.createEntityManagerFactory("expenses-local");
	}

	@Override
	public void filter(ContainerRequestContext request) throws IOException {
		System.out.println("Before " + request.getUriInfo());
		SecurityHelper.setCurrentUsername(null);
		beginTransaction(request);
		if (!authenticate(request)) {
			List<Object> matchedResources = request.getUriInfo().getMatchedResources();
			if (matchedResources.get(0) instanceof IndexResource || matchedResources.get(0) instanceof EntityResource) {
				// fine
				return;
			}
			request.abortWith(Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", "Custom realm=\"Basic realm\"").build());
		} else {
			SecurityContext securityContext = request.getSecurityContext();
			if (securityContext != null)
				SecurityHelper.setCurrentUsername(securityContext.getUserPrincipal().getName());
		}
	}

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
		try {
			enableCORS(request, response);
			endTransaction(request, response);
		} finally {
			System.out.println("After " + request.getUriInfo());
		}
	}

	private boolean authenticate(ContainerRequestContext request) {
		if (request.getRequest().getMethod().equals("OPTIONS")) {
			return true;
		}
		Cookie authCookie = request.getCookies().get("Custom-Authentication");
		String username;
		String password;
		if (authCookie != null) {
			username = authCookie.getValue();
			password = null;
		} else {
			String authorization = request.getHeaderString("Authorization");
			if (authorization == null || !authorization.startsWith("Custom "))
				return false;
			String encodedUserPassword = authorization.replaceFirst("Custom ", "");
			String usernameAndPassword = new String(Base64.getDecoder().decode(encodedUserPassword));
			StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
			username = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
			password = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
		}

		Profile user = new ProfileService().findByUsername(username);
		if (user == null)
			return false;
		if (password != null && !password.equals(user.getPassword()))
			return false;
		
		
		List<String> roles = SecurityHelper.getRoles(user);
		Principal principal = new Principal() {
			@Override
			public String getName() {
				return username;
			}
		};
		
		request.setSecurityContext(new SecurityContext() {
			
			@Override
			public boolean isUserInRole(String toCheck) {
				return roles.contains(toCheck);
			}
			
			@Override
			public boolean isSecure() {
				return false;
			}
			
			@Override
			public Principal getUserPrincipal() {
				return principal;
			}
			
			@Override
			public String getAuthenticationScheme() {
				return "custom";
			}
		});
		return true;
	}

	private void enableCORS(ContainerRequestContext request, ContainerResponseContext response) {
		response.getHeaders().putSingle("Access-Control-Allow-Origin", StringUtils.trimToEmpty(request.getHeaderString("Origin")));
		response.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
		response.getHeaders().putSingle("Access-Control-Allow-Methods", "HEAD, GET, PUT, POST, DELETE, OPTIONS, TRACE");
		response.getHeaders().putSingle("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, Authorization");
	}

	private void beginTransaction(ContainerRequestContext request) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		if (MUTATION_METHODS.contains(request.getMethod())) {
			entityManager.getTransaction().begin();
		}
		PersistenceHelper.setEntityManager(entityManager);
	}

	private void endTransaction(ContainerRequestContext request, ContainerResponseContext response) {
		SecurityHelper.setCurrentUsername(null);
		EntityManager entityManager = PersistenceHelper.getEntityManager(false);
		if (entityManager != null) {
			try {
				if (MUTATION_METHODS.contains(request.getMethod())) {
					if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
						entityManager.getTransaction().commit();
					} else {
						entityManager.getTransaction().rollback();
					}
				}
			} finally {
				entityManager.close();
				PersistenceHelper.removeEntityManager();
			}
		}
	}
}