package resource.{applicationName};

import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;

public final class ContextListener implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent event) {
	    ServletContext context = event.getServletContext();
	    EntityManagerFactory emf = Persistence.createEntityManagerFactory("{applicationName}-local");
        context.setAttribute("emf", emf);
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
	}
}