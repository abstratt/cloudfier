package com.abstratt.kirra.mdd.rest;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.CookieSetting;
import org.restlet.data.Method;
import org.restlet.ext.crypto.CookieAuthenticator;

public class KirraCookieAuthenticator extends CookieAuthenticator implements KirraAuthenticationContext {

	public KirraCookieAuthenticator(Restlet toMonitor) {
        super(null, "Cloudfier App", "u7YzXaKLlsq+KJ1z".getBytes());
        setVerifier(new KirraSecretVerifier());
        setMultiAuthenticating(true);
        setNext(toMonitor);
        setIdentifierFormName("username");
    }
    
    @Override
    public String getCookieName() {
        return "cloudfier-" + WORKSPACE_NAME.get() + "-credentials";
    }

    @Override
    public String getLoginPath() {
        return "/" + WORKSPACE_NAME.get() + "/login";
    }

    @Override
    public String getLogoutPath() {
        return "/" + WORKSPACE_NAME.get() + "/logout";
    }
    
    @Override
    public String getRealm() {
        return WORKSPACE_NAME.get() + "-realm";
    }
    
    @Override
    protected CookieSetting getCredentialsCookie(Request request, Response response) {
    	CookieSetting credentialsCookie = super.getCredentialsCookie(request, response);
    	String cookiePath = request.getRootRef().getPath() +'/' + WORKSPACE_NAME.get();
    	credentialsCookie.setPath(cookiePath);
		return credentialsCookie;
    }

    @Override
    protected int beforeHandle(final Request request, final Response response) {
    	configure(request);
        return super.beforeHandle(request, response);
    }
    
    @Override
    protected boolean isLoggingIn(Request request, Response response) {
        return isInterceptingLogin() && Method.POST.equals(request.getMethod()) && request.getResourceRef().toString().endsWith("/login");
    }

    @Override
    protected boolean isLoggingOut(Request request, Response response) {
        return isInterceptingLogout() && (Method.GET.equals(request.getMethod()) || Method.POST.equals(request.getMethod()))
                        && request.getResourceRef().toString().endsWith("/logout");
    }

    @Override
    protected int unauthenticated(Request request, Response response) {
    	return super.unauthenticated(request, response);
    }
    @Override
    public boolean isOptional() {
    	return KirraAuthenticationContext.super.isOptional() || !IS_AJAX.get();
    }
}