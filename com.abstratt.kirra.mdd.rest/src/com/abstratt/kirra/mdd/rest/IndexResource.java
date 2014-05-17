package com.abstratt.kirra.mdd.rest;

import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.json.InstanceJSONRepresentation.SingleLink;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class IndexResource extends AbstractKirraRepositoryResource {
	@Get
	public Representation index() {
		if (getQueryValue("path") != null) {
			Reference originalRef = getRequest().getOriginalRef().clone();
			originalRef.setFragment(null);
			originalRef.setQuery(null);
			originalRef.setPath(getRequest().getRootRef().getPath() + "/" + KirraRESTUtils.getWorkspaceFromProjectPath(getRequest()) + "/");
			redirectTemporary(originalRef);
			return null;
		}
		return KirraRESTUtils.serveInResource(getRequest(), new ResourceUtils.ResourceRunnable() {
			@Override
			public Representation runInContext(IRepository context) {
				Repository repo = KirraRESTUtils.getRepository();
				Instance currentUser = repo.getCurrentUser();
				SingleLink currentUserAsJson = null;
				if (currentUser != null) {
					currentUserAsJson = buildLink(currentUser);
				}					
				
				StringBuffer buffer = new StringBuffer();
				buffer.append("{");
				buffer.append("\t\"entities\": \"" + getReferenceBuilder().getEntitiesReference() + "\",\n");
				buffer.append("\t\"services\": \"" + getReferenceBuilder().getServicesReference() + "\",\n");
				buffer.append("\t\"currentUser\": {\n");
				buffer.append("\t\t\"profile\": " + JsonHelper.renderAsJson(currentUserAsJson) + ",\n");
				buffer.append("\t\t\"username\": " + JsonHelper.renderAsJson(KirraRESTUtils.getCurrentUserName()) + "\n");
				buffer.append("\t},\n");
				buffer.append("\t\"applicationTimestamp\": \"" + repo.getBuild() + "\",\n");
				buffer.append("\t\"platformVersion\": \"" + Activator.getInstance().getPlatformVersion() + "\"\n");
				buffer.append("}\n");
				
				return new StringRepresentation(buffer.toString(),
						MediaType.APPLICATION_JSON);
			}
		});
	}
}
