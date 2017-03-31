package com.abstratt.kirra.mdd.rest;

import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.restlet.Request;
import org.restlet.data.Method;
import org.restlet.engine.header.Header;
import org.restlet.util.Series;

import com.abstratt.kirra.mdd.core.KirraMDDConstants;

public interface KirraAuthenticationContext {
	/** 
	 * Does the application allow "guest" users? 
	 */
	ThreadLocal<Boolean> ALLOWS_ANONYMOUS = new ThreadLocal<Boolean>();
	/**
	 * Is authentication optional for this resource?
	 */
    ThreadLocal<Boolean> IS_OPTIONAL = new ThreadLocal<Boolean>();
    /** 
     * Does this resource allow authentication? 
     */
    ThreadLocal<Boolean> PROTECTED = new ThreadLocal<Boolean>();
    /** 
     * Does this resource allow authentication? 
     */
    ThreadLocal<Boolean> AUTHORIZED = new ThreadLocal<Boolean>();    
    /** 
     * Does this application require authentication? 
     */
    ThreadLocal<Boolean> LOGIN_REQUIRED = new ThreadLocal<Boolean>();    
    /**
     * Is this an AJAX request?
     */
    ThreadLocal<Boolean> IS_AJAX = new ThreadLocal<Boolean>();
    /**
     * The name of the current workspace.
     */
    ThreadLocal<String> WORKSPACE_NAME = new ThreadLocal<String>();
    
    default void configure(Request request) {
    	IPath path = new Path(request.getResourceRef().getRemainingPart(true, false)).makeRelative();
    	String workspace = path.isEmpty() ? KirraRESTUtils.getWorkspaceFromProjectPath(request) : path.segment(0);
        WORKSPACE_NAME.set(workspace);
        Properties properties = KirraRESTUtils.getProperties(workspace);
        LOGIN_REQUIRED.set(Boolean.valueOf(properties.getProperty(KirraMDDConstants.LOGIN_REQUIRED, Boolean.FALSE.toString())));
        ALLOWS_ANONYMOUS.set(Boolean.valueOf(properties.getProperty(KirraMDDConstants.ALLOW_ANONYMOUS, Boolean.FALSE.toString())));
        PROTECTED.set(false);
        IS_OPTIONAL.set(request.getMethod().equals(Method.OPTIONS) || !PROTECTED.get());
    	String requestedWith = ((Series<Header>) request.getAttributes().get("org.restlet.http.headers")).getFirstValue("X-Requested-With"); 
        IS_AJAX.set("XMLHttpRequest".equals(requestedWith));
    }
    
	default boolean isOptional() {
		return IS_OPTIONAL.get();
	}
	
	default boolean isLoginRequired() {
		return LOGIN_REQUIRED.get();
	}
	
	default boolean isProtected() {
		return PROTECTED.get();
	}
	default boolean isAjax() {
		return IS_AJAX.get();
	}
	default boolean allowsAnonymous() {
		return ALLOWS_ANONYMOUS.get();
	}
	default boolean isAuthorized() {
		return AUTHORIZED.get();
	}
}
