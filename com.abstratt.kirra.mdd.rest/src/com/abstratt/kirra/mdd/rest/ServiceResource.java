package com.abstratt.kirra.mdd.rest;

import org.eclipse.core.runtime.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Service;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class ServiceResource extends AbstractKirraRepositoryResource {
	
	@Get("json")
	public Representation getService() {
		String entityName = (String) getRequestAttributes().get("entityName");
		String entityNamespace = (String) getRequestAttributes().get("entityNamespace");
		Service service = getRepository().getService(entityNamespace, entityName);
		ResourceUtils.ensure(service != null, null, Status.CLIENT_ERROR_NOT_FOUND);
		ServiceJSONRepresentation entityRepresentation = new ServiceJSONRepresentation();
		entityRepresentation.name = service.getName();
		entityRepresentation.namespace = service.getNamespace();
		Reference reference = getExternalReference();
		entityRepresentation.uri = reference.toString();
		entityRepresentation.rootUri = reference.getParentRef().toString();
		for (Operation operation : service.getOperations()) {
			switch (operation.getKind()) {
			case Retriever:
				QueryJSONRepresentation queryRepr = new QueryJSONRepresentation();
				queryRepr.name = operation.getName();
				queryRepr.uri = reference.getParentRef().getParentRef().addSegment(Paths.RETRIEVERS).addSegment(entityNamespace + '.' + entityName).addSegment(operation.getName()).toString();
				entityRepresentation.queries.add(queryRepr);
				break;
			case Event:
				ActionJSONRepresentation actionRepr = new ActionJSONRepresentation();
				actionRepr.name = operation.getName();
				actionRepr.uri = reference.getParentRef().getParentRef().addSegment(Paths.EVENTS).addSegment(entityNamespace + '.' + entityName).addSegment(operation.getName()).toString();
				entityRepresentation.events	.add(actionRepr);
				break;
			default:
				// not expected in services
				Assert.isTrue(false, operation.toString());
			}
		}
		return new StringRepresentation(JsonHelper.renderAsJson(entityRepresentation).toString(),
				MediaType.APPLICATION_JSON);
	}

}
