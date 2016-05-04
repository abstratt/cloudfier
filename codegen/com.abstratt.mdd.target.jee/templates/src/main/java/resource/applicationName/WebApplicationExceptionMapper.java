package resource.{applicationName};

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
	public Response toResponse(WebApplicationException exception) {
		// pass-through - just so the overarching ThrowableMapper does not get it
		return exception.getResponse();
	}
}