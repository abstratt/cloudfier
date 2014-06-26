package com.abstratt.kirra.mdd.rest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Filter;

import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.mdd.runtime.KirraOnMDDRuntime;
import com.abstratt.kirra.rest.resources.KirraContext;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.pluginutils.ISharedContextRunnable;
import com.abstratt.pluginutils.LogUtils;

/**
 * A filter that sets the current runtime.
 */
public class KirraRepositoryFilter extends Filter {
    public KirraRepositoryFilter(Restlet next) {
        this.setNext(next);
    }

    @Override
    protected int doHandle(final Request request, final Response response) {
        String workspace = getWorkspace(request);
        try {
            return KirraRESTUtils.runInKirraWorkspace(workspace, new ISharedContextRunnable<IRepository, Integer>() {
                @Override
                public Integer runInContext(IRepository context) {
                    Repository kirraRepository = RepositoryService.DEFAULT.getFeature(Repository.class);
                    KirraContext.setInstanceManagement(kirraRepository);
                    KirraContext.setSchemaManagement(kirraRepository);
                    KirraContext.setBaseURI(KirraReferenceUtils.getBaseReference(request, request.getResourceRef(),
                            com.abstratt.mdd.frontend.web.Paths.API_V2).toUri());
                    try {
                        int result = KirraRepositoryFilter.super.doHandle(request, response);
                        return result;
                    } finally {
                        KirraContext.setInstanceManagement(null);
                        KirraContext.setSchemaManagement(null);
                        KirraContext.setBaseURI(null);
                    }
                }
            });
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
        } catch (RuntimeException e) {
            handleInternalError(response, e);
        }
        return Filter.STOP;
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
