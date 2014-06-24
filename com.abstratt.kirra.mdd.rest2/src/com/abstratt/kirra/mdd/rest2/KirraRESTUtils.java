package com.abstratt.kirra.mdd.rest2;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.security.User;

import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.Repository;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ResourceUtils;
import com.abstratt.mdd.frontend.web.ResourceUtils.ResourceRunnable;
import com.abstratt.pluginutils.ISharedContextRunnable;

public class KirraRESTUtils {
	public static Representation handleException(KirraException e, Response response) {
		Status status;
		
		switch (e.getKind()) {
		case OBJECT_NOT_FOUND:
			status = Status.CLIENT_ERROR_NOT_FOUND;
			break;
		case VALIDATION:
			status = Status.CLIENT_ERROR_BAD_REQUEST;
			break;
		default:
			status = Status.CLIENT_ERROR_BAD_REQUEST;
		}
		response.setStatus(status);
		Map<String, String> error = new HashMap<String, String>();
		error.put("message", e.getMessage());
		error.put("context", e.getContext());
		return jsonToStringRepresentation(error);
	}
	
	public static Representation jsonToStringRepresentation(Object jsonObject) {
		return new StringRepresentation(JsonHelper.renderAsJson(jsonObject).toString(), MediaType.APPLICATION_JSON);
	}
	
	public static Representation serveInResource(Request request, final ResourceRunnable runnable) {
		return ResourceUtils.serveInResource(request, new ResourceRunnable() {
			@Override
			public Representation runInContext(final IRepository context) {
				return runnable.runInContext(context);
			}
		});
	}
	
	public static <R> R runInKirraRepository(final Request request, final ISharedContextRunnable<IRepository, R> runnable) {
		String workspace = getWorkspaceFromProjectPath(request);
		return runInKirraWorkspace(workspace, runnable);
	}

	public static <R> R runInKirraWorkspace(final String workspace, final ISharedContextRunnable<IRepository, R> runnable) {
		try {
			return RepositoryService.DEFAULT.runInRepository(ResourceUtils.getRepositoryURI(workspace), runnable);
		} catch (KirraException e) {
			ResourceUtils.fail(e, org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST);
			// never runs
			return null;
		} catch (CoreException e) {
			ResourceUtils.fail(e, null);
			// never runs
			return null;
		}
	}
	
	public static Repository getRepository() {
		return RepositoryService.DEFAULT.getCurrentResource().getFeature(Repository.class);
	}
	
	public static String getWorkspaceFromProjectPath(Request request) {
		return ResourceUtils.getWorkspaceFromProjectPath(request);
	}

	protected static org.eclipse.emf.common.util.URI getRepositoryURI(String workspace) {
		return ResourceUtils.getRepositoryURI(workspace);
	}

	public static String getCurrentUserName() {
		final Request request = Request.getCurrent();
		if (request == null)
			return null;
		User user = request.getClientInfo().getUser();
		if (user == null)
			return null;
		return user.getIdentifier();
	}

	public static Properties getProperties(String workspace) {
		if (Runtime.get() != null)
			return Runtime.get().getRepository().getProperties();
		URI workspaceURI = ResourceUtils.getRepositoryURI(workspace);
		return MDDUtil.loadRepositoryProperties(workspaceURI);
	}

	public static boolean doesWorkspaceExist(String workspace) {
		return MDDUtil.doesRepositoryExist(ResourceUtils.getRepositoryURI(workspace));
	}
}
