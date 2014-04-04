package com.abstratt.kirra.server;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class KirraServer implements IApplication {

	public Object start(IApplicationContext context) throws Exception {
		synchronized (this) {
			this.wait();
		}
		return null;
	}

	public void stop() {
		// nothing to do here
	}

}
