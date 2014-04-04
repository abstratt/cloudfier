package com.abstratt.kirra.mdd.rest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Filter;

import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.mdd.runtime.KirraOnMDDRuntime;
import com.abstratt.mdd.core.IRepository;
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
		try {
			return KirraRESTUtils.runInKirraRepository(request, new ISharedContextRunnable<IRepository, Integer>() {
				public Integer runInContext(IRepository context) {
					int result = KirraRepositoryFilter.super.doHandle(request, response);
					return result;				
				}
			});
		} catch (ResourceException e) {
			if (e.getCause() != null) {
				Throwable translated = KirraOnMDDRuntime.translateException(e.getCause());
				if (translated instanceof KirraException) {
					handleKirraException(response, (KirraException) translated);
					return STOP;
				} else {
					handleInternalError(response, e.getCause());
					return STOP;
				}
			} else { 
				handleResourceException(response, e);
				return STOP;
			}
		} catch (KirraException e) {
			handleKirraException(response, e);
		} catch (RuntimeException e) {
			handleInternalError(response, e);
		}
		return STOP;
	}
	private void handleKirraException(final Response response,
			KirraException kirraException) {
		LogUtils.logWarning(getClass().getPackage().getName(), "Application error", kirraException);
		response.setEntity(KirraRESTUtils.handleException(kirraException, response));
		response.getEntity().setExpirationDate(new Date(0));
	}
	private void handleInternalError(final Response response, Throwable e) {
		LogUtils.logWarning(getClass().getPackage().getName(), "Internal application error", e);
		response.setStatus(Status.SERVER_ERROR_INTERNAL);
		Map<String, String> error = new HashMap<String, String>();
		error.put("message", e.getMessage());
		response.setEntity(KirraRESTUtils.jsonToStringRepresentation(error));
		response.getEntity().setExpirationDate(new Date(0));
	}
	private void handleResourceException(final Response response, ResourceException e) {
		LogUtils.logWarning(getClass().getPackage().getName(), "application error", e);
		response.setStatus(e.getStatus());
		Map<String, String> error = new HashMap<String, String>();
		error.put("message", e.getMessage());
		response.setEntity(KirraRESTUtils.jsonToStringRepresentation(error));
		response.getEntity().setExpirationDate(new Date(0));
	}
}
