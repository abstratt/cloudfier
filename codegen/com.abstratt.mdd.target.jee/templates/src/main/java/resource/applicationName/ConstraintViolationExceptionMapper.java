package resource.{applicationName};

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import {applicationName}.ConstraintViolationException;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
	public Response toResponse(ConstraintViolationException exception) {
		exception.printStackTrace(System.out);
		String errorType = exception.getClass().getSimpleName();
		Map<String, String> errorResponse = new LinkedHashMap<>();
		errorResponse.put("message", exception.getMessage() == null ? errorType : exception.getMessage());
		errorResponse.put("type", errorType);
		return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
	}
}
