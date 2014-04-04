package com.abstratt.kirra.mdd.ui;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.ext.servlet.ServerServlet;

/**
 * A custom servlet instead of using ServerServlet directly so we can work
 * around the fact Restlet itself cannot see our code.
 */
public class KirraUIServlet extends ServerServlet {
	
	private static final long serialVersionUID = 1L;
	@Override
	protected Application createApplication(Context parentContext) {
		return new KirraUIApplication(getComponent());
	}
	
	@Override
	protected Component createComponent() {
		Component component = super.createComponent();
		// used for serving static files
		component.getClients().add(Protocol.CLAP);
		return component;
	}
	
}
