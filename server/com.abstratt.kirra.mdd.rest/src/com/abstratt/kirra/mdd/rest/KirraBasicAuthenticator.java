package com.abstratt.kirra.mdd.rest;

import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.engine.header.Header;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.util.Series;

import com.abstratt.kirra.mdd.runtime.KirraMDDConstants;

public class KirraBasicAuthenticator extends ChallengeAuthenticator implements KirraAuthenticationContext {

	public KirraBasicAuthenticator(Restlet toMonitor) {
        super(null, ChallengeScheme.HTTP_BASIC, "Cloudfier App");
        setVerifier(new KirraSecretVerifier());
        setMultiAuthenticating(true);
        setNext(toMonitor);
    }
    
    @Override
    public String getRealm() {
        return WORKSPACE_NAME.get() + "-realm";
    }

    @Override
    protected int beforeHandle(final Request request, final Response response) {
    	configure(request);
        if (IS_AJAX.get())
        	return CONTINUE;
        return super.beforeHandle(request, response);
    }
    
    @Override
    protected int unauthenticated(Request request, Response response) {
    	return super.unauthenticated(request, response);
    }

    @Override
    public boolean isOptional() {
    	return KirraAuthenticationContext.super.isOptional();
    }
}