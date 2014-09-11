package com.abstratt.kirra.mdd.rest.impl.v1;

import javax.servlet.ServletException;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.ext.servlet.ServerServlet;

/**
 * A custom servlet instead of using ServerServlet directly so we can work
 * around the fact Restlet itself cannot see our code.
 */
public class LegacyKirraRESTServlet extends ServerServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    protected Application createApplication(Context parentContext) {
        return new LegacyKirraMDDRestletApplication(getComponent());
    }
}
