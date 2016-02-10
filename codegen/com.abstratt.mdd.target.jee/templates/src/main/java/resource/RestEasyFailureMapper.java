package resource.{applicationName};

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.Failure;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class RestEasyFailureMapper implements ExceptionMapper<Failure> {
	public Response toResponse(Failure exception) {
		// pass-through - just so the overarching ThrowableMapper does not get it
		return exception.getResponse();
	}
}
