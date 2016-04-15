package resource.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import util.PersistenceHelper;

import org.apache.commons.lang3.StringUtils;

@Provider
public class ContainerRequestResponseFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Collection<String> MUTATION_METHODS = Arrays.asList("PUT", "POST", "DELETE");

    @PersistenceContext(unitName = "{applicationName}-jta", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        if (MUTATION_METHODS.contains(request.getMethod())) {
            entityManager.getTransaction().begin();
        }
        try {
        	PersistenceHelper.setEntityManager(entityManager);
        } catch (RuntimeException e) {
        	e.printStackTrace();
        }
    }
    
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        EntityManager entityManager = PersistenceHelper.getEntityManager();
        if (entityManager != null) {
            if (MUTATION_METHODS.contains(request.getMethod())) {
                if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                    entityManager.getTransaction().commit();
                } else {
                    entityManager.getTransaction().rollback();
                }
            }
        }	
        PersistenceHelper.setEntityManager(null);
        response.getHeaders().putSingle("Access-Control-Allow-Origin", StringUtils.trimToEmpty(request.getHeaderString("Origin")));
        response.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
        response.getHeaders().putSingle("Access-Control-Allow-Methods", "HEAD, GET, PUT, POST, DELETE, OPTIONS, TRACE");
        response.getHeaders().putSingle("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");
    }
}
