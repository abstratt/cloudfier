package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.mdd.rest.impl.v1.representation.EntityLinkJSONRepresentation;
import com.abstratt.mdd.frontend.web.JsonHelper;

public class EntityListResource extends AbstractKirraRepositoryResource {

    @Get
    public Representation list() throws IOException {
        List<Entity> entities = getRepository().getEntities(null);
        List<EntityLinkJSONRepresentation> links = new ArrayList<EntityLinkJSONRepresentation>();
        Reference reference = getExternalReference();
        for (Entity entity : entities) {
            EntityLinkJSONRepresentation link = new EntityLinkJSONRepresentation();
            link.name = entity.getName();
            link.namespace = entity.getEntityNamespace();
            link.label = entity.getLabel();
            link.description = entity.getDescription();
            link.topLevel = entity.isTopLevel();

            link.uri = reference.clone().addSegment(entity.getEntityNamespace() + '.' + entity.getName()).toString();
            link.extentUri = reference.getParentRef().clone().addSegment("instances")
                    .addSegment(entity.getEntityNamespace() + '.' + entity.getName()).addSegment("").toString();
            links.add(link);
        }
        return new StringRepresentation(JsonHelper.renderAsJson(links).toString(), MediaType.APPLICATION_JSON);
    }
}
