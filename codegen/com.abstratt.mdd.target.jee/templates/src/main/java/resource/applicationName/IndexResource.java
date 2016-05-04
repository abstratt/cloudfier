package resource.{applicationName};

import java.net.URI;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import userprofile.Profile;
import userprofile.ProfileService;

@Path("/")
@Produces("application/json")
public class IndexResource {
	@Context
    UriInfo uri;
	
    @GET
    public Response getIndex(@Context SecurityContext securityContext) {
    	URI currentUserURI = null;
    	Principal principal = securityContext.getUserPrincipal();
    	if (principal != null && principal.getName() != null) {
    		Profile user = new ProfileService().findByUsername(securityContext.getUserPrincipal().getName());
    		if (user != null) {
    			currentUserURI = uri.getBaseUriBuilder().segment("entities", "userprofile.Profile", "instances", user.getId().toString()).build(); 
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
