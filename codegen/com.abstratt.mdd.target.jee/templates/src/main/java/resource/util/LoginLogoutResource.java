package resource.util;

import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import util.SecurityHelper;

/**
 * This class only exists so the requests don't fail with 404. Handling of login/logout are done elsewhere.
 */
@Produces("application/json")
@Path("session")
public class LoginLogoutResource {
	@Context
    UriInfo uri;
	
    @GET
    @Path("login")
    public Response loginAsGET() {
    	return doLogin();
    }

	@POST
    @Consumes("application/json")
	@Path("login")
    public Response loginAsPOST(String argumentMapRepresentation) {
		return doLogin();
    }

    @GET
    @Path("logout")
    public Response logoutAsGET() {
		return doLogout();
    }
	@POST
    @Consumes("application/json")
	@Path("logout")
    public Response logoutAsPOST() {
		return doLogout();
    }
	@GET
    public Response current() {
		return Response.ok("{}").build();
    }
	
	private Response doLogout() {
		NewCookie cookie = new NewCookie("Custom-Authentication", null, "/", uri.getRequestUri().getHost(), null, -1000, false);
		return Response.ok("{ \"loggedIn\" : false}").cookie(cookie).expires(new Date(System.currentTimeMillis() - (60*60*24*1000))).build();
	}

	private Response doLogin() {
		String currentUsername = SecurityHelper.getCurrentUsername();
		NewCookie cookie = new NewCookie("Custom-Authentication", currentUsername, "/", uri.getRequestUri().getHost(), null, 24*60*60, false);
		return Response.ok("{ \"loggedId\" : true}").cookie(cookie).build();
	}
}
