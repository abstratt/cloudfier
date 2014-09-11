package com.abstratt.kirra.mdd.rest.impl.v2;

import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Restlet;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.service.LogService;

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
        return new KirraRepositoryFilter(super.createInboundRoot()) {
            @Override
            protected String getWorkspace(Request request) {
                String workspace = request.getResourceRef().getSegments().get(2);
                return workspace;
            }
        };
    }
}
