package com.abstratt.kirra.mdd.rest;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.restlet.Request;
import org.restlet.data.Method;
import org.restlet.engine.header.Header;
import org.restlet.util.Series;

import com.abstratt.kirra.EntityCapabilities;
import com.abstratt.kirra.InstanceCapabilities;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.abstratt.kirra.mdd.core.KirraMDDConstants;
import com.abstratt.kirra.rest.common.KirraContext;
import com.abstratt.kirra.rest.common.Paths;

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
    
    static String[] PROTECTED_PATHS = { Paths.INSTANCES, Paths.FINDERS, Paths.SESSION, Paths.LOGOUT, Paths.LOGIN, Paths.SIGNUP };
    
    default void configure(Request request) {
    	IPath path = new Path(request.getResourceRef().getRemainingPart(true, false)).makeRelative();
    	String workspace = path.isEmpty() ? KirraRESTUtils.getWorkspaceFromProjectPath(request) : path.segment(0);
        WORKSPACE_NAME.set(workspace);
        Properties properties = KirraRESTUtils.getProperties(workspace);
        LOGIN_REQUIRED.set(Boolean.valueOf(properties.getProperty(KirraMDDConstants.LOGIN_REQUIRED, Boolean.FALSE.toString())));
        ALLOWS_ANONYMOUS.set(Boolean.valueOf(properties.getProperty(KirraMDDConstants.ALLOW_ANONYMOUS, Boolean.FALSE.toString())));
        PROTECTED.set(isProtectedPath(path.removeFirstSegments(1)));
        IS_OPTIONAL.set(request.getMethod().equals(Method.OPTIONS) || !PROTECTED.get());
    	String requestedWith = ((Series<Header>) request.getAttributes().get("org.restlet.http.headers")).getFirstValue("X-Requested-With"); 
        IS_AJAX.set("XMLHttpRequest".equals(requestedWith));
    }
    
	default boolean isProtectedPath(IPath path) {
//		String[] segments = path.segments();
//		for (String protectedSegment : PROTECTED_PATHS)
//			for (String segment : segments)
//				if (protectedSegment.equals(segment))
//					return true;
		return false;
	}

	default boolean isPrivileged(Request request) {
		return !request.getMethod().equals(Method.OPTIONS);
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
