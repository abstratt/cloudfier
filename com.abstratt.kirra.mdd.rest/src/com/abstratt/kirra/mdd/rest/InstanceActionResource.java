package com.abstratt.kirra.mdd.rest;

import java.io.IOException;

import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;

public class InstanceActionResource extends AbstractKirraRepositoryResource {
    @Post("json")
    public Representation execute(Representation request) throws IOException {
        String actionName = (String) getRequestAttributes().get("actionName");
        String objectId = (String) getRequestAttributes().get("objectId");
        executeOperation(getEntityNamespace(), getEntityName(), actionName, objectId, false);

        Instance instance = getRepository().getInstance(getEntityNamespace(), getEntityName(), objectId, false);
        if (instance == null) {
            // it is possible invoking an action deleted the resource
            setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return new EmptyRepresentation();
        }
        Entity entity = getRepository().getEntity(getEntityNamespace(), getEntityName());
        return jsonToStringRepresentation(getInstanceJSONRepresentation(instance, entity));
    }
}
