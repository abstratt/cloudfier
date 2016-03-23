package resource.{applicationName};

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class ThrowableMapper implements ExceptionMapper<Throwable> {
	public Response toResponse(Throwable exception) {
		if (exception instanceof ClientErrorException) {
			System.out.println("Client error");
			exception.printStackTrace(System.out);
		} else {
			System.err.println("Server error");
			exception.printStackTrace(System.err);
		}
		Map<String, String> errorResponse = new LinkedHashMap<>();
		errorResponse.put("message", exception.toString());
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
	}
}
