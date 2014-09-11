package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import java.util.List;

import org.restlet.data.Status;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Relationship;
import com.abstratt.mdd.frontend.web.ResourceUtils;

/**
 * For a given instance, and relationship, determines which entity instances are
 * valid choices.
 */
public class RelationshipDomainResource extends AbstractInstanceListResource {

    @Override
    protected List<Instance> findInstances(String entityNamespace, String entityName) {
        String objectId = (String) getRequestAttributes().get("objectId");
        Entity entity = getRepository().getEntity(getEntityNamespace(), getEntityName());
        ResourceUtils.ensure(entity != null, "Entity not found: " + getEntityName(), Status.CLIENT_ERROR_NOT_FOUND);
        String relationshipName = (String) getRequestAttributes().get("relationshipName");
        Relationship relationship = entity.getRelationship(relationshipName);
        ResourceUtils.ensure(relationship != null, "Relationship not found: " + relationshipName, Status.CLIENT_ERROR_NOT_FOUND);
        return getRepository().getRelationshipDomain(entity, objectId, relationship);
    }

    @Override
    protected Entity getTargetEntity() {
        String relationshipName = (String) getRequestAttributes().get("relationshipName");
        return RelatedInstanceListResource.getRelatedEntity(super.getTargetEntity(), this, relationshipName);
    }

}
