package com.abstratt.kirra.mdd.rest;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.mdd.rest.TupleParser;
import com.abstratt.mdd.frontend.web.JsonHelper;

public class InstanceListResource extends AbstractInstanceListResource {

	@Override
	protected List<Instance> findInstances(String entityNamespace,
			String entityName) {
		return getRepository().getInstances(entityNamespace, entityName, false);
	}

	@Post("json")
	public Representation create(Representation requestRepresentation) throws IOException {
		JsonNode toCreate = JsonHelper.parse(requestRepresentation.getReader());
		Instance newInstance = getRepository().newInstance(getEntityNamespace(), getEntityName());
		Entity entity = getRepository().getEntity(getEntityNamespace(), getEntityName());
		TupleParser.updateInstanceFromJsonRepresentation(toCreate, entity, newInstance);
		setStatus(Status.SUCCESS_CREATED);
	    return jsonToStringRepresentation(getInstanceJSONRepresentation(getRepository().createInstance(newInstance), entity));
	}
}
