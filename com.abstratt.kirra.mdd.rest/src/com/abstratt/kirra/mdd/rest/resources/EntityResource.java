package com.abstratt.kirra.mdd.rest.resources;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.mdd.rest.KirraReferenceBuilder;
import com.abstratt.kirra.mdd.rest.representation.ActionJSONRepresentation;
import com.abstratt.kirra.mdd.rest.representation.EntityJSONRepresentation;
import com.abstratt.kirra.mdd.rest.representation.QueryJSONRepresentation;
import com.abstratt.kirra.mdd.rest.representation.RelationshipJSONRepresentation;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class EntityResource extends AbstractKirraRepositoryResource {

    @Get("json")
    public Representation getEntity() {
        String entityName = (String) getRequestAttributes().get("entityName");
        String entityNamespace = (String) getRequestAttributes().get("entityNamespace");
        Entity entity = getRepository().getEntity(entityNamespace, entityName);
        ResourceUtils.ensure(entity != null, null, Status.CLIENT_ERROR_NOT_FOUND);
        EntityJSONRepresentation entityRepresentation = new EntityJSONRepresentation();
        entityRepresentation.name = entity.getName();
        entityRepresentation.namespace = entity.getEntityNamespace();
        entityRepresentation.label = entity.getLabel();
        entityRepresentation.description = entity.getDescription();
        entityRepresentation.topLevel = entity.isTopLevel();
        Reference reference = getExternalReference();
        entityRepresentation.uri = reference.toString();
        KirraReferenceBuilder referenceBuilder = getReferenceBuilder();
        entityRepresentation.rootUri = referenceBuilder.getEntitiesReference().toString();
        entityRepresentation.instances = referenceBuilder.buildInstanceListReference(entityNamespace, entityName).toString();
        entityRepresentation.template = reference.getParentRef().getParentRef().addSegment(com.abstratt.mdd.frontend.web.Paths.INSTANCES)
                .addSegment(entityNamespace + '.' + entityName).addSegment("_template").toString();

        for (Property property : entity.getProperties()) {
            entityRepresentation.properties.add(property);
        }

        for (Relationship relationship : entity.getRelationships()) {
            RelationshipJSONRepresentation relationshipRepr = new RelationshipJSONRepresentation();
            relationshipRepr.name = relationship.getName();
            TypeRef relatedTypeRef = relationship.getTypeRef();
            relationshipRepr.type = relatedTypeRef.getTypeName();
            relationshipRepr.typeUri = entityRepresentation.uri = referenceBuilder.buildEntityReference(
                    relatedTypeRef.getEntityNamespace(), relatedTypeRef.getTypeName()).toString();
            entityRepresentation.relationships.add(relationshipRepr);
        }

        List<Operation> enabledStaticOperations = getRepository().getEnabledEntityActions(entity);

        for (Operation operation : entity.getOperations()) {
            switch (operation.getKind()) {
            case Finder:
                QueryJSONRepresentation queryRepr = new QueryJSONRepresentation();
                queryRepr.name = operation.getName();
                queryRepr.uri = reference.getParentRef().getParentRef().addSegment(com.abstratt.mdd.frontend.web.Paths.FINDERS)
                        .addSegment(entityNamespace + '.' + entityName).addSegment(operation.getName()).toString();
                entityRepresentation.finders.add(queryRepr);
                break;
            case Action:
                ActionJSONRepresentation actionRepr = new ActionJSONRepresentation();
                actionRepr.name = operation.getName();
                actionRepr.instance = operation.isInstanceOperation();
                if (!operation.isInstanceOperation()) {
                    actionRepr.uri = reference.getParentRef().getParentRef().addSegment(com.abstratt.mdd.frontend.web.Paths.ACTIONS)
                            .addSegment(entityNamespace + '.' + entityName).addSegment(operation.getName()).toString();
                    actionRepr.enabled = enabledStaticOperations.contains(operation);
                }
                entityRepresentation.actions.add(actionRepr);
                break;
            default:
                // ignore events, not possible
                Assert.isTrue(false, operation.toString());
            }
        }
        return new StringRepresentation(JsonHelper.renderAsJson(entityRepresentation).toString(), MediaType.APPLICATION_JSON);
    }

}
