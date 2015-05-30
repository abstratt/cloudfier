package resource.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import util.PersistenceHelper;

@Provider
public class RequestResponseFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Collection<String> MUTATION_METHODS = Arrays.asList("PUT", "POST", "DELETE");
    private EntityManagerFactory entityManagerFactory;

    public RequestResponseFilter() {
        entityManagerFactory = Persistence.createEntityManagerFactory("integration-test");
    }
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        if (MUTATION_METHODS.contains(request.getMethod())) {
            entityManager.getTransaction().begin();
        }
        PersistenceHelper.setEntityManager(entityManager);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
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
            }
        }
        PersistenceHelper.setEntityManager(null);
        response.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
        response.getHeaders().putSingle("Access-Control-Allow-Methods", "HEAD, GET, PUT, POST, DELETE, OPTIONS, TRACE");
        response.getHeaders().putSingle("Access-Control-Allow-Headers", "Content-Type");
        
    }
}
