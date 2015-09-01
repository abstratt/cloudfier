package util;

import javax.persistence.EntityManager;

public class PersistenceHelper {
    public static ThreadLocal<EntityManager> entityManager = new ThreadLocal<>();
    public static EntityManager getEntityManager() {
        return entityManager.get();
    }
    
    public static void setEntityManager(EntityManager em) {
        entityManager.set(em);
    }
    
    public static void flush() {
        flush(false);
    }
    
    public static void flush(boolean clear) {
        getEntityManager().flush();
        if (clear)
            getEntityManager().clear();
    }
    
    public static void refresh(Object... toRefresh) {
        for (Object object : toRefresh) {
            getEntityManager().refresh(object);
        }
    }
    public static void persist(Object... toPersist) {
        for (Object object : toPersist) {
            getEntityManager().persist(object);
        }
    }

}