package resource.util;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import util.PersistenceHelper;

@Provider
public class RequestResponseFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private EntityManagerFactory entityManagerFactory;

    public RequestResponseFilter() {
        entityManagerFactory = Persistence.createEntityManagerFactory("integration-test");
    }
    @Override
    public void filter(ContainerRequestContext arg0) throws IOException {
        PersistenceHelper.setEntityManager(entityManagerFactory.createEntityManager());
    }

    @Override
    public void filter(ContainerRequestContext arg0, ContainerResponseContext response) throws IOException {
        EntityManager entityManager = PersistenceHelper.getEntityManager();
        if (entityManager != null)
            entityManager.close();
        PersistenceHelper.setEntityManager(null);
        response.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
    }
}
