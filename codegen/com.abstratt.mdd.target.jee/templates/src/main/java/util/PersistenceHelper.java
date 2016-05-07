package util;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

public class PersistenceHelper {
    public static ThreadLocal<EntityManager> entityManager = new ThreadLocal<>();
    public static EntityManager getEntityManager(boolean required) {
    	EntityManager em = entityManager.get();
    	if (em == null && required)
    		throw new NullPointerException();
        return em;
    }
 
    public static EntityManager getEntityManager() {
        return getEntityManager(true);
    }
    
    public static void setEntityManager(EntityManager em) {
    	System.out.println("Setting entityManager to " + em);
    	if (em == null)
    		throw new NullPointerException();
        entityManager.set(em);
    }
    
    public static void removeEntityManager() {
    	System.out.println("Removing entityManager");
        entityManager.remove();
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
    public static EntityManager createSchema() {
        return Persistence.createEntityManagerFactory("{applicationName}-schema-init").createEntityManager();
    }

    public static EntityManager createSchemaAndInitData() {
    	return Persistence.createEntityManagerFactory("{applicationName}-schema-data-init").createEntityManager();
    }
    
    public static EntityManager openEntityManager() {
    	return Persistence.createEntityManagerFactory("{applicationName}-local").createEntityManager();
    }
}