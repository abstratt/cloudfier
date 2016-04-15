package resource.{applicationName};

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;

import kirra_user_profile.UserProfile;
import kirra_user_profile.UserProfileService;
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
    public Response getIndex(@Context SecurityContext securityContext) {
    	URI currentUserURI = null;
    	Principal principal = securityContext.getUserPrincipal();
    	if (principal != null) {
    		UserProfile user = new UserProfileService().findByUsername(securityContext.getUserPrincipal().getName());
    		if (user != null) {
    			currentUserURI = uri.getBaseUriBuilder().segment("entities", "kirra_user_profile.UserProfile", "instances", user.getId().toString()).build(); 
    		}
    	}
    	
        Map<String, Object> index = new LinkedHashMap<>();
        index.put("applicationName", "Expenses Application");
        index.put("entities", uri.getBaseUri().resolve("entities/"));
        index.put("services", uri.getBaseUri().resolve("services/"));
        index.put("uri", uri.getRequestUri());
        index.put("currentUser", currentUserURI);
        return Response.ok(index, MediaType.APPLICATION_JSON).build();
    }
}
