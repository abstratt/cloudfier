package com.abstratt.kirra.mdd.rest;

import java.util.Base64;
import java.util.StringTokenizer;

import javax.ws.rs.core.Response.Status;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.CookieSetting;
import org.restlet.data.Method;
import org.restlet.engine.header.Header;
import org.restlet.ext.crypto.CookieAuthenticator;
import org.restlet.util.Series;

import com.abstratt.kirra.rest.common.Paths;
import com.abstratt.kirra.rest.resources.ResourceHelper;

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
        String cookieName = "cloudfier-" + WORKSPACE_NAME.get() + "-credentials";
		return cookieName;
    }

    @Override
    public String getLoginPath() {
        return Paths.LOGIN_PATH.replaceFirst("{application}", WORKSPACE_NAME.get());
    }

    @Override
    public String getLogoutPath() {
        return Paths.LOGOUT_PATH.replaceFirst("{application}", WORKSPACE_NAME.get());
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
    protected void login(Request request, Response response) {
    	// first discard the authentication cookie from the request
    	request.getCookies().removeIf(it -> it.getName().equals(getCookieName()));
    	Series<Header> requestHeaders = (Series<Header>)request.getAttributes().computeIfAbsent("org.restlet.http.headers", key -> new Series<Header>(Header.class));
    	Header authorizationHeader = requestHeaders.getFirst("Authorization");
		if (authorizationHeader == null || !authorizationHeader.getValue().startsWith("Custom ")) {
			super.logout(request, response);
			return;
		}
		String authorization = authorizationHeader.getValue();
		
		String encodedUserPassword = authorization.replaceFirst("Custom ", "");
		String usernameAndPassword = new String(Base64.getDecoder().decode(encodedUserPassword));
		StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
		String username = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
		String password = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;

        // Set credentials
        ChallengeResponse cr = new ChallengeResponse(new ChallengeScheme("Custom", "custom"), username, password);
        request.setChallengeResponse(cr);

        // Attempt to redirect
        attemptRedirect(request, response);
    }

    @Override
    protected int beforeHandle(final Request request, final Response response) {
    	configure(request);
        return super.beforeHandle(request, response);
    }
    
    @Override
    protected boolean isLoggingIn(Request request, Response response) {
        return isInterceptingLogin() && Method.POST.equals(request.getMethod()) && request.getResourceRef().toString().endsWith(Paths.LOGIN);
    }

    @Override
    protected boolean isLoggingOut(Request request, Response response) {
        return isInterceptingLogout() && (Method.GET.equals(request.getMethod()) || Method.POST.equals(request.getMethod()))
                        && request.getResourceRef().toString().endsWith(Paths.LOGOUT);
    }

    @Override
    protected int unauthenticated(Request request, Response response) {
    	super.unauthenticated(request, response);
    	ResourceHelper.ensure(!isProtected() || !isLoginRequired(), "login_required", Status.UNAUTHORIZED);
    	return CONTINUE; 
    }
    @Override
    public boolean isOptional() {
    	boolean optional = KirraAuthenticationContext.super.isOptional() || !isAjax();
		return optional;
    }
}