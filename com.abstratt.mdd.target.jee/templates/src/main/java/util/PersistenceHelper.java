package util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class PersistenceHelper {
    public static ThreadLocal<EntityManager> entityManager = new ThreadLocal<>();
    public static EntityManager getEntityManager() {
        return entityManager.get();
    }
    
    public static void setEntityManager(EntityManager em) {
        entityManager.set(em);
    }

    public static void flush(boolean clear) {
        getEntityManager().flush();
        if (clear)
            getEntityManager().clear();
    }
}