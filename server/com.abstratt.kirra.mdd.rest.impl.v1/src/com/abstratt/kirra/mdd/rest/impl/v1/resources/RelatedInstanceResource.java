package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.InstanceRef;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.mdd.rest.KirraReferenceBuilder;
import com.abstratt.kirra.mdd.rest.impl.v1.representation.InstanceJSONRepresentation;
import com.abstratt.kirra.mdd.rest.impl.v1.representation.RelatedInstanceJSONRepresentation;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class RelatedInstanceResource extends InstanceResource {

    protected static InstanceJSONRepresentation buildRelatedInstanceRepresentation(KirraReferenceBuilder refBuilder,
            RelatedInstanceJSONRepresentation jsonRepresentation, Entity baseEntity, Instance relatedInstance, Entity entity,
            String relationshipName, String objectId) {
        RelatedInstanceJSONRepresentation representation = jsonRepresentation;
        Reference baseInstanceRef = refBuilder.buildInstanceReference(new Instance(baseEntity.getTypeRef(), objectId));
        representation.relatedUri = baseInstanceRef.addSegment(com.abstratt.mdd.frontend.web.Paths.RELATIONSHIPS)
                .addSegment(relationshipName).addSegment(relatedInstance.getObjectId()).toString();
        return representation;
    }

    @Delete
    public Representation detach() {
        String objectId = (String) getRequestAttributes().get("objectId");
        String relatedId = (String) getRequestAttributes().get("relatedId");
        String relationshipName = (String) getRequestAttributes().get("relationshipName");
        Entity entity = getRepository().getEntity(getEntityNamespace(), getEntityName());
        Relationship relationship = entity.getRelationship(relationshipName);
        ResourceUtils.ensure(relationship != null, "Relationship not found: " + relationshipName, Status.CLIENT_ERROR_NOT_FOUND);
        getRepository().unlinkInstances(relationship, objectId, new InstanceRef(relationship.getTypeRef().getEntityNamespace(), relationship.getTypeRef().getTypeName(), relatedId));
        return new EmptyRepresentation();
    }

    @Override
    protected InstanceJSONRepresentation createInstanceJSONRepresentation() {
        return new RelatedInstanceJSONRepresentation();
    }

    @Override
    protected InstanceJSONRepresentation getInstanceJSONRepresentation(Instance relatedInstance, Entity entity) {
        String relationshipName = (String) getRequestAttributes().get("relationshipName");
        String objectId = (String) getRequestAttributes().get("objectId");
        RelatedInstanceJSONRepresentation jsonRepresentation = (RelatedInstanceJSONRepresentation) super.getInstanceJSONRepresentation(
                relatedInstance, entity);
        return RelatedInstanceResource.buildRelatedInstanceRepresentation(getReferenceBuilder(), jsonRepresentation,
                super.getTargetEntity(), relatedInstance, entity, relationshipName, objectId);
    }

    @Override
    protected String getObjectId() {
        return (String) getRequestAttributes().get("relatedId");
    }

    @Override
    protected Entity getTargetEntity() {
        return RelatedInstanceListResource.getRelatedEntity(super.getTargetEntity(), this,
                (String) getRequestAttributes().get("relationshipName"));
    }

}
