package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.abstratt.kirra.Repository;
import com.abstratt.kirra.populator.DataPopulator;
import com.abstratt.kirra.populator.DataRenderer;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class DataResource extends AbstractKirraRepositoryResource {
    @Post
    public Representation create(Representation noneExpected) {
        Repository repo = getRepository();
        if (Boolean.parseBoolean(repo.getProperties().getProperty("mdd.isLibrary"))) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return new StringRepresentation("{\"success\": false, \"message\": \"Cannot deploy database for a library project\" }",
                    MediaType.APPLICATION_JSON);
        }
        repo.initialize();
        DataPopulator populator = new DataPopulator(repo);
        int status = populator.populate();
        ResourceUtils.ensure(status >= 0, "Failure populating the database, check the data sample", Status.CLIENT_ERROR_BAD_REQUEST);
        return new StringRepresentation("{\"success\": true, \"processed\": " + status + " }", MediaType.APPLICATION_JSON);
    }

    @Get
    public Representation snapshot() {
        Repository repo = getRepository();
        repo.setFiltering(false);
        Map<String, Map<String, ?>> rendered = new DataRenderer(repo).render();
        return new StringRepresentation(JsonHelper.renderAsJson(rendered), MediaType.APPLICATION_JSON);
    }
}
