package com.abstratt.kirra.mdd.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.abstratt.kirra.Tuple;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.json.TupleJSONRepresentation;

public class ServiceInvocationResource extends AbstractKirraRepositoryResource {
	@Post("json")
	public Representation sendEvent(Representation request) throws IOException {
		String actionName = (String) getRequestAttributes().get("actionName");
		//Service entity = getRepository().getService(getEntityNamespace(), getEntityName());
		executeOperation(getEntityNamespace(), getEntityName(), actionName, null, false);
		return new EmptyRepresentation();
	}

	@Get("json")
	public Representation runQueryViaGET() throws IOException {
		return runQuery(true);
    }

	@Post("json")
	public Representation runQueryViaPOST(Representation request) throws IOException {
		return runQuery(false);
    }
	
	public Representation runQuery(boolean queryParameters) throws IOException {
		String actionName = (String) getRequestAttributes().get("retrieverName");
		List<Object> result = executeOperation(getEntityNamespace(), getEntityName(), actionName, null, queryParameters);
		List<Object> asTuples = new ArrayList<Object>();
		if (!result.isEmpty()) {
	        TupleType tupleType = null;
			for (Object object : result) {
	            if (object instanceof Tuple) {
	            	TupleJSONRepresentation asTuple = new TupleJSONRepresentation();
	            	new TupleJSONRepresentationBuilder().build(asTuple, null, tupleType, (Tuple) object);
	            	asTuples.add(asTuple);
	            } else
	            	asTuples.add(object); 
			}
		}
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put("data", asTuples);
		return jsonToStringRepresentation(response);
	}

}
