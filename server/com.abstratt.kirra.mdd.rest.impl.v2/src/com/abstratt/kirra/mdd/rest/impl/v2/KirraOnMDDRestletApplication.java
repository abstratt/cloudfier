package com.abstratt.kirra.mdd.rest.impl.v2;

import java.util.List;
import java.util.function.Predicate;

import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.service.LogService;

import com.abstratt.kirra.mdd.rest.KirraCookieAuthenticator;
import com.abstratt.kirra.mdd.rest.KirraBasicAuthenticator;
import com.abstratt.kirra.mdd.rest.KirraCORSFilter;
import com.abstratt.kirra.mdd.rest.KirraRepositoryFilter;
import com.abstratt.kirra.mdd.rest.KirraStatusService;
import com.abstratt.kirra.rest.resources.KirraJaxRsApplication;

public class KirraOnMDDRestletApplication extends JaxRsApplication {
    private KirraStatusService customStatusService;
    private LogService customLogService;
    private Component component;

    public KirraOnMDDRestletApplication(Component component) {
        super();
        customStatusService = new KirraStatusService();
        customLogService = new LogService(false);
        this.component = component;
        this.component.setStatusService(customStatusService);
        this.component.setLogService(customLogService);
        this.getServices().remove(this.getStatusService());
        this.setStatusService(customStatusService);
        KirraJaxRsApplication jaxApplication = new KirraJaxRsApplication();
        this.add(jaxApplication);
    }
    
    @Override
    public Restlet createInboundRoot() {
        Restlet baseInboundRoot = super.createInboundRoot();
        KirraBasicAuthenticator basicAuthenticator = new KirraBasicAuthenticator(baseInboundRoot);
        KirraCookieAuthenticator cookieAuthenticator = new KirraCookieAuthenticator(basicAuthenticator);
		KirraRepositoryFilter repositoryFilter = new KirraRepositoryFilter(cookieAuthenticator) {
            @Override
            protected String getWorkspace(Request request) {
                String workspace = request.getResourceRef().getSegments().get(2);
                return workspace;
            }
        };
        KirraCORSFilter corsFilter = new KirraCORSFilter(repositoryFilter);
        return corsFilter;
    }
}
