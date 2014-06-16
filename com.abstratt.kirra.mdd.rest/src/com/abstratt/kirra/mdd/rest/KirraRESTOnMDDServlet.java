package com.abstratt.kirra.mdd.rest;

import org.restlet.Application;
import org.restlet.Context;

import com.abstratt.kirra.rest.resources.KirraJaxRsApplication;

/**
 * A custom servlet instead of using ServerServlet directly so we can work
 * around the fact Restlet itself cannot see our code.
 */
public class KirraRESTOnMDDServlet extends KirraRESTServlet {
	
	private static final long serialVersionUID = 1L;
	@Override
	protected Application createApplication(Context parentContext) {
		KirraOnMDDRestletApplication restletApplication = new KirraOnMDDRestletApplication(getComponent());
		KirraJaxRsApplication jaxRsApplication = new KirraJaxRsApplication();
		restletApplication.add(jaxRsApplication);
		return restletApplication;
	}
}
