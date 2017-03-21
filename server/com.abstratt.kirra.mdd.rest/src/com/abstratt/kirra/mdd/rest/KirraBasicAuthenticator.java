package com.abstratt.kirra.mdd.rest;

import javax.ws.rs.core.Response.Status;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.security.ChallengeAuthenticator;

import com.abstratt.kirra.rest.resources.ResourceHelper;

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
        if (isAjax())
        	return CONTINUE;
        return super.beforeHandle(request, response);
    }
    
    @Override
    protected int unauthenticated(Request request, Response response) {
    	super.unauthenticated(request, response);
    	ResourceHelper.ensure(!PROTECTED.get() || !LOGIN_REQUIRED.get(), "Login required", Status.FORBIDDEN);
    	return CONTINUE; 
    }
}