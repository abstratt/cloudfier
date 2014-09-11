package com.abstratt.kirra.mdd.rest.resources;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.mdd.rest.KirraRESTUtils;
import com.abstratt.kirra.mdd.rest.representation.TupleParser;
import com.abstratt.kirra.mdd.runtime.KirraActorSelector;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class ProfileResource extends AbstractKirraRepositoryResource {

    /**
     * Request entity is the same as you would use to create a contact in the
     * application.
     */
    @Post("json")
    public Representation createProfile(Representation requestRepresentation) throws IOException {
        String currentUserName = KirraRESTUtils.getCurrentUserName();
        ResourceUtils.ensure(currentUserName != null && !"guest".equalsIgnoreCase(currentUserName.trim()),
                "A user needs to be logged in when creating a profile", Status.CLIENT_ERROR_UNAUTHORIZED);

        RuntimeObject existingProfile = KirraActorSelector.findUserInstance(currentUserName, Runtime.get());
        ResourceUtils.ensure(existingProfile == null, "Logged in user already has a profile, delete it first",
                Status.CLIENT_ERROR_BAD_REQUEST);

        JsonNode toCreate = JsonHelper.parse(requestRepresentation.getReader());

        String typeURI = toCreate.get("type").getTextValue();
        ResourceUtils.ensure(!StringUtils.isBlank(typeURI), "Type is required ", Status.CLIENT_ERROR_BAD_REQUEST);

        Entity profileEntity = getEntityFromURI(typeURI);
        ResourceUtils.ensure(profileEntity != null, "Could not determine entity from type URI", Status.CLIENT_ERROR_BAD_REQUEST);
        ResourceUtils.ensure(profileEntity.isUser(), profileEntity.getTypeRef().getFullName() + " is not a user entity",
                Status.CLIENT_ERROR_BAD_REQUEST);

        Repository repository = getRepository();
        Instance newInstance = repository.newInstance(profileEntity.getEntityNamespace(), profileEntity.getName());
        new TupleParser(repository).updateInstanceFromJsonRepresentation(toCreate, profileEntity, newInstance);
        newInstance.setValue("username", currentUserName);
        setStatus(Status.SUCCESS_CREATED);
        return jsonToStringRepresentation(getInstanceJSONRepresentation(repository.createInstance(newInstance), profileEntity));
    }

}
