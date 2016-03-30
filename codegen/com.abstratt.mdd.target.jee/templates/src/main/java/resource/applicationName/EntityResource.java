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

import org.apache.commons.lang3.StringUtils;

import resource.util.EntityResourceHelper;

@Path("/")
@Produces("application/json")
public class EntityResource {
	@Context
    UriInfo uri;
	
    @GET
    @Path("entities")
    public Response getList() {
        String[] entityNames = {
            {entityNameList}
        };
        try {
            List<String> entities = new ArrayList<>();
            for (String entityName : entityNames)
                entities.add(EntityResourceHelper.getEntityRepresentation(entityName, uri.getRequestUri().resolve("..").toString()));
            String result = "[\n" + StringUtils.join(entities, ",\n") + "\n]"; 
            return Response.ok(result, MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            return Response.status(500).build();
        }
    }
    
    @GET
    public Response getIndex() {
        Map<String, String> index = new LinkedHashMap<>();
        index.put("applicationName", "{applicationLabel}");
        index.put("entities", uri.getBaseUri().resolve("entities/").toString());
        index.put("services", uri.getBaseUri().resolve("services/").toString());
        index.put("uri", uri.getRequestUri().toString());
        return Response.ok(index, MediaType.APPLICATION_JSON).build();
    }
}
