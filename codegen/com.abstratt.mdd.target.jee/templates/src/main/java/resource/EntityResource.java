package resource.{applicationName};

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

@Path("/")
@Produces("application/json")
public class EntityResource {
    @Context
    UriInfo uri;
    private String getEntityRepresentation(String entityName, String baseUri) throws IOException {
        Map<String, String> substitutions = new LinkedHashMap<>();
        substitutions.put("baseUri", baseUri);
        InputStream stream = EntityResource.class.getResourceAsStream("/schema/entities/" + entityName + ".json");
        if (stream == null)
            return null;
        try {
            String contents = IOUtils.toString(stream, "UTF-8");
            return StrSubstitutor.replace(contents, substitutions);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
    @GET
    @Path("entities/{entityName}")
    public Response getSingle(@PathParam("entityName") String entityName) {
        try {
            String contents = getEntityRepresentation(entityName, uri.getRequestUri().resolve("..").toString());
            if (contents == null) {
                return Response.status(404).build();
            }
            return Response.ok(contents, MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            return Response.status(500).build();
        }
    }
    @GET
    @Path("entities")
    public Response getList() {
        String[] entityNames = {
            {entityNameList}
        };
        try {
            List<String> entities = new ArrayList<>();
            for (String entityName : entityNames)
                entities.add(getEntityRepresentation(entityName, uri.getRequestUri().resolve("..").toString()));
            String result = "[\n" + StringUtils.join(entities, ",\n") + "\n]"; 
            return Response.ok(result, MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            return Response.status(500).build();
        }
    }
    
    @GET
    public Response getIndex() {
        Map<String, String> index = new LinkedHashMap<>();
        index.put("applicationName", "{applicationName}");
        index.put("entities", uri.getBaseUri().resolve("entities/").toString());
        index.put("services", uri.getBaseUri().resolve("services/").toString());
        index.put("uri", uri.getRequestUri().toString());
        return Response.ok(index, MediaType.APPLICATION_JSON).build();
    }
}
