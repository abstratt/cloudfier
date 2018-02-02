package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.abstratt.kirra.Repository;
import com.abstratt.kirra.populator.DataPopulator;
import com.abstratt.kirra.populator.DataRenderer;
import com.abstratt.kirra.populator.DataRenderer.LazyReference;
import com.abstratt.kirra.rest.common.CommonHelper;
import com.abstratt.mdd.frontend.web.ResourceUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DataResource extends AbstractKirraRepositoryResource {
    @Post
    public Representation create(Representation noneExpected) {
        Path snapshotFileName = Optional.ofNullable(getQueryValue("snapshot")).map(it -> Paths.get(it)).orElse(DataPopulator.DEFAULT_SNAPSHOT_PATH);
        ResourceUtils.ensure(!snapshotFileName.isAbsolute(), "Relative path expected: " + snapshotFileName, Status.CLIENT_ERROR_BAD_REQUEST);
        Repository repo = getRepository();
        if (Boolean.parseBoolean(repo.getProperties().getProperty("mdd.isLibrary"))) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return new StringRepresentation("{\"success\": false, \"message\": \"Cannot deploy database for a library project\" }",
                    MediaType.APPLICATION_JSON);
        }
        repo.initialize();
        DataPopulator populator = new DataPopulator(repo, snapshotFileName);
        int status = populator.populate();
        ResourceUtils.ensure(status >= 0, "Failure populating the database, check the data sample file", Status.CLIENT_ERROR_BAD_REQUEST);
        return new StringRepresentation("{\"success\": true, \"processed\": " + status + " }", MediaType.APPLICATION_JSON);
    }

    @Get
    public Representation snapshot() {
        Repository repo = getRepository();
        repo.setFiltering(false);
        Map<String, Map<String, ?>> rendered = new DataRenderer(repo).render();
        GsonBuilder basicGsonBuilder = CommonHelper.buildBasicGson();
        basicGsonBuilder.registerTypeAdapter(LazyReference.class, new JsonSerializer<LazyReference>() {
        	@Override
        	public JsonElement serialize(LazyReference lazyReference, Type arg1, JsonSerializationContext arg2) {
        		String asString = lazyReference.getReference().getEntityNamespace() + '.' + lazyReference.getReference().getEntityName() + '@' + lazyReference.getSequenceNumber();
				return new JsonPrimitive(asString);
        	}
		});
		return new StringRepresentation(basicGsonBuilder.create().toJson(rendered), MediaType.APPLICATION_JSON);
    }
}
