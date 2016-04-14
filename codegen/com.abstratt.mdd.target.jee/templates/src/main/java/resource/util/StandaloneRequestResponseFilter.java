package resource.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Base64;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import kirra_user_profile.UserProfileService;

import util.PersistenceHelper;

public class StandaloneRequestResponseFilter implements ContainerRequestFilter, ContainerResponseFilter {
	private static final Collection<String> MUTATION_METHODS = Arrays.asList("PUT", "POST", "DELETE");
	private EntityManagerFactory entityManagerFactory;

	public StandaloneRequestResponseFilter() {
		entityManagerFactory = Persistence.createEntityManagerFactory("{applicationName}-local");
	}

	@Override
	public void filter(ContainerRequestContext request) throws IOException {
		beginTransaction(request);
//		if (!authenticate(request))
//			request.abortWith(Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", "BASIC realm=\"Basic realm\"").build());	
	}

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
		enableCORS(response);
		endTransaction(request, response);
	}

	private boolean authenticate(ContainerRequestContext request) {
		if (request.getRequest().getMethod().equals("OPTIONS")) {
			return true;
		}
		String authorization = request.getHeaderString("Authorization");
		if (authorization == null || authorization.isEmpty())
			return false;
		String encodedUserPassword = authorization.replaceFirst("Basic" + " ", "");
		String usernameAndPassword = new String(Base64.getDecoder().decode(encodedUserPassword));
		StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
		String username = tokenizer.nextToken();
		String password = tokenizer.nextToken();		

		Authenticator authenticator = Authenticator.getInstance();
		if (!authenticator.authenticate(username, password))
			return false;
		return true;
	}

	private void enableCORS(ContainerResponseContext response) {
		response.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
		response.getHeaders().putSingle("Access-Control-Allow-Methods", "HEAD, GET, PUT, POST, DELETE, OPTIONS, TRACE");
		response.getHeaders().putSingle("Access-Control-Allow-Headers", "Content-Type");
	}

	private void beginTransaction(ContainerRequestContext request) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		if (MUTATION_METHODS.contains(request.getMethod())) {
			entityManager.getTransaction().begin();
		}
		PersistenceHelper.setEntityManager(entityManager);
	}

	private void endTransaction(ContainerRequestContext request, ContainerResponseContext response) {
		EntityManager entityManager = PersistenceHelper.getEntityManager();
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
				PersistenceHelper.setEntityManager(null);
			}
		}
	}
}
