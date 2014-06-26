package com.abstratt.kirra.mdd.rest;

import java.io.IOException;

import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import com.abstratt.kirra.Instance;

public class StaticActionResource extends AbstractKirraRepositoryResource {
    @Post("json")
    public Representation execute(Representation request) throws IOException {
        String actionName = (String) getRequestAttributes().get("actionName");
        this.<Instance> executeOperation(getEntityNamespace(), getEntityName(), actionName, null, false);
        return new EmptyRepresentation();
    }

}
