package com.abstratt.kirra.mdd.rest;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;
import com.abstratt.mdd.frontend.web.JsonHelper;

public class InstanceListResource extends AbstractInstanceListResource {

    @Post("json")
    public Representation create(Representation requestRepresentation) throws IOException {
        JsonNode toCreate = JsonHelper.parse(requestRepresentation.getReader());
        Repository repository = getRepository();
        Instance newInstance = repository.newInstance(getEntityNamespace(), getEntityName());
        Entity entity = repository.getEntity(getEntityNamespace(), getEntityName());
        new TupleParser(repository).updateInstanceFromJsonRepresentation(toCreate, entity, newInstance);
        setStatus(Status.SUCCESS_CREATED);
        return jsonToStringRepresentation(getInstanceJSONRepresentation(repository.createInstance(newInstance), entity));
    }

    @Override
    protected List<Instance> findInstances(String entityNamespace, String entityName) {
        return getRepository().getInstances(entityNamespace, entityName, false);
    }
}
