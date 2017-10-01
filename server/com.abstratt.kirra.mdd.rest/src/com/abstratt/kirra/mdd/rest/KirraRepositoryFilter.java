package com.abstratt.kirra.mdd.rest;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.core.runtime.IStatus;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Filter;
import org.restlet.security.User;

import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.kirra.mdd.core.KirraMDDConstants;
import com.abstratt.kirra.mdd.runtime.KirraOnMDDRuntime;
import com.abstratt.kirra.rest.common.KirraContext;
import com.abstratt.kirra.rest.resources.KirraRestException;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.pluginutils.ISharedContextRunnable;
import com.abstratt.pluginutils.LogUtils;

/**
 * A filter that sets the current runtime.
 */
public class KirraRepositoryFilter extends Filter {
    private String environment;

	public KirraRepositoryFilter(Restlet next, String environment) {
        this.setNext(next);
        this.environment = StringUtils.defaultString(environment, "default");
    }
	public KirraRepositoryFilter(Restlet next) {
        this(next, null);
    }

    @Override
    protected int doHandle(final Request request, final Response response) {
        StopWatch watch = new StopWatch();
        watch.start();
        KirraContext.setEnvironment(environment);
        String workspace = getWorkspace(request);
        try {
            int result = KirraRESTUtils.runInKirraWorkspace(workspace, new ISharedContextRunnable<IRepository, Integer>() {
                @Override
                public Integer runInContext(IRepository context) {
                    Repository kirraRepository = RepositoryService.DEFAULT.getFeature(Repository.class);
                    KirraContext.setInstanceManagement(kirraRepository);
                    KirraContext.setSchemaManagement(kirraRepository);
					KirraContext.setOptions(getApplicationOptions(context.getProperties()));
                    URI baseURI = KirraReferenceUtils.getBaseReference(request, request.getResourceRef(),
                            Paths.API_V2).toUri();
					KirraContext.setBaseURI(baseURI);
                    try {
                        int result = KirraRepositoryFilter.super.doHandle(request, response);
                        return result;
                    } finally {
                        KirraContext.setInstanceManagement(null);
                        KirraContext.setSchemaManagement(null);
                        KirraContext.setBaseURI(null);
                        KirraContext.setOptions(null);
                    }
                }
            }, request.getMethod());
            watch.stop();
            LogUtils.log(IStatus.INFO, Activator.ID, request.toString(), null);
            LogUtils.log(IStatus.INFO, Activator.ID, "Time: " + watch.getTime(), null);
            return result;
        } catch (ResourceException e) {
            if (e.getCause() != null) {
                Throwable translated = KirraOnMDDRuntime.translateException(e.getCause());
                if (translated instanceof KirraException) {
                    handleKirraException(response, (KirraException) translated);
                    return Filter.STOP;
                } else {
                    handleInternalError(response, e.getCause());
                    return Filter.STOP;
                }
            } else {
                handleResourceException(response, e);
                return Filter.STOP;
            }
        } catch (KirraException e) {
            handleKirraException(response, e);
        } catch (KirraRestException e) {
        	handleKirraRESTException(response, e);
        } catch (RuntimeException e) {
            handleInternalError(response, e);
        }
        return Filter.STOP;
    }
    
    private static KirraContext.Options getApplicationOptions(Properties properties) {
        boolean allowAnonymous = Boolean.valueOf(properties.getProperty(KirraMDDConstants.ALLOW_ANONYMOUS));
        boolean isLoginAllowed = Boolean.valueOf(properties.getProperty(KirraMDDConstants.LOGIN_ALLOWED));
        boolean isLoginRequired = !allowAnonymous && isLoginAllowed;
        return new KirraContext.Options(isLoginRequired, isLoginAllowed);
    }

    protected String getWorkspace(Request request) {
        return KirraRESTUtils.getWorkspaceFromProjectPath(request);
    }

    private void handleInternalError(final Response response, Throwable e) {
        LogUtils.logWarning(getClass().getPackage().getName(), "Internal application error", e);
        response.setStatus(Status.SERVER_ERROR_INTERNAL);
        Map<String, String> error = new HashMap<String, String>();
        error.put("message", e.getMessage());
        response.setEntity(KirraRESTUtils.jsonToStringRepresentation(error));
        response.getEntity().setExpirationDate(new Date(0));
    }
    
    private void handleKirraRESTException(final Response response, KirraRestException kirraRestException) {
        LogUtils.logWarning(getClass().getPackage().getName(), "REST error", kirraRestException);
        response.setStatus(Status.valueOf(kirraRestException.getStatus().getStatusCode()));
        Map<String, String> error = new HashMap<String, String>();
        error.put("message", kirraRestException.getMessage());
        response.setEntity(KirraRESTUtils.jsonToStringRepresentation(error));
        response.getEntity().setExpirationDate(new Date(0));

    }

    private void handleKirraException(final Response response, KirraException kirraException) {
        LogUtils.logWarning(getClass().getPackage().getName(), "Application error", kirraException);
        response.setEntity(KirraRESTUtils.handleException(kirraException, response));
        response.getEntity().setExpirationDate(new Date(0));
    }

    private void handleResourceException(final Response response, ResourceException e) {
        LogUtils.logWarning(getClass().getPackage().getName(), "application error", e);
        response.setStatus(e.getStatus());
        Map<String, String> error = new HashMap<String, String>();
        error.put("message", StringUtils.isBlank(e.getStatus().getDescription()) ? e.getStatus().getReasonPhrase() : e.getStatus()
                .getDescription());
        response.setEntity(KirraRESTUtils.jsonToStringRepresentation(error));
        response.getEntity().setExpirationDate(new Date(0));
    }
}
