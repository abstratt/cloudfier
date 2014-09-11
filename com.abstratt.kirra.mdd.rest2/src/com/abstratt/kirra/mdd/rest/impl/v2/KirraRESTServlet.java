package com.abstratt.kirra.mdd.rest.impl.v2;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.ext.servlet.ServerServlet;

public class KirraRESTServlet extends ServerServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected Application createApplication(Context parentContext) {
        KirraOnMDDRestletApplication kirraRestletApplication = new KirraOnMDDRestletApplication(getComponent());
        return kirraRestletApplication;
    }
}
