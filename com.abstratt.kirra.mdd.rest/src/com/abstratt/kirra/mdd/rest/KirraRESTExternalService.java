package com.abstratt.kirra.mdd.rest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.restlet.data.Form;

import com.abstratt.kirra.ExternalService;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Operation.OperationKind;
import com.abstratt.kirra.Parameter;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.Service;
import com.abstratt.kirra.Tuple;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.mdd.rest.TupleJSONRepresentation;
import com.abstratt.kirra.mdd.rest.TupleParser;
import com.abstratt.kirra.mdd.runtime.KirraMDDConstants;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ReferenceUtils;
import com.abstratt.pluginutils.LogUtils;

public class KirraRESTExternalService implements ExternalService {

	@Override
	public List<?> executeOperation(String namespace, String classifierName,
			String operationName, Map<String, Object> arguments) {
		Repository repository = KirraRESTUtils.getRepository();
		Service service = repository.getService(namespace, classifierName);
		Operation operation = service.getOperation(operationName);
		if (operation == null)
			throw new KirraException("Invalid service operation " + service.getTypeRef() + "#" + operationName, null, KirraException.Kind.EXTERNAL);
		List<Parameter> parameters = operation.getParameters();
		int required = parameters.size();
		if (required != arguments.size())
			throw new KirraException("Service operation " + service.getTypeRef() + "#" + operationName + " requires " + required + " parameters, got " + arguments.size(), null, KirraException.Kind.EXTERNAL);
		switch(operation.getKind()) {
		case Retriever:
			return retrieveData(repository, service, operation, arguments);
		case Event:
			pushEvent(repository, service,operation, arguments);
			return Arrays.asList();
		default: throw new KirraException("Invalid service operation " + service.getTypeRef() + "#" + operation.getName() + ": " + operation.getKind(), null, KirraException.Kind.EXTERNAL);
		}
	}
	
	private void pushEvent(Repository repository, Service service, Operation operation,
			Map<String, Object> argumentMap) {
		Tuple event = (Tuple) argumentMap.values().iterator().next();
		TupleType eventType = repository.getTupleType(event.getScopeNamespace(), event.getScopeName());
		URI uri = getOperationURI(service, operation, Collections.<String, Object>emptyMap());
		PostMethod method = new PostMethod(uri.toString());
		HttpClient httpClient = new HttpClient();
		try {
			TupleJSONRepresentation representation = new TupleJSONRepresentation();
			new TupleJSONRepresentationBuilder().build(representation, null, eventType, event);
			String jsonRequest = JsonHelper.renderAsJson(representation);
			method.setRequestEntity(new StringRequestEntity(jsonRequest, "application/json", "UTF-8"));
			int response = httpClient.executeMethod(method);
			if (response != 200)
				LogUtils.logError(LegacyKirraMDDRestletApplication.ID, "Unexpected status for " + uri + ": " + response + "\n" + method.getResponseBodyAsString(64*1024) , null);
			// no use for response, not expected
		} catch (IOException e) {
			throw new KirraException("", e, KirraException.Kind.EXTERNAL);
		} finally {
			method.releaseConnection();
		}

	}

	private URI getOperationURI(Service service, Operation operation,
			Map<String, Object> argumentMap) {
		String qName = service.getTypeRef().toString();
		URI resolved;
		String absoluteURI = KirraRESTUtils.getRepository().getProperties().getProperty(KirraMDDConstants.EXTERNAL_CONNECTOR_URI + "/" + qName + "." + operation.getName());
		if (absoluteURI != null)
			resolved = URI.create(absoluteURI);
		else {
			String uriBase = KirraRESTUtils.getRepository().getProperties().getProperty(KirraMDDConstants.EXTERNAL_CONNECTOR_URI, URI.create(ReferenceUtils.INTERNAL_BASE).resolve("/external/").toString());
			
			URI connectorURI = URI.create(uriBase);
			String endpointType = operation.getKind() == OperationKind.Event ? "events" : "retrievers";
			resolved = connectorURI.resolve(endpointType + "/").resolve(qName + "/").resolve(operation.getName());
		}
		
		Form arguments = new Form();
		for (Map.Entry<String, Object> argument : argumentMap.entrySet())
			if (argument.getValue() != null)
				arguments.set(argument.getKey(), argument.getValue().toString());
		try {
			return new URI(resolved.getScheme(), resolved.getUserInfo(), resolved.getHost(), resolved.getPort(), resolved.getPath(), StringUtils.trimToEmpty(resolved.getQuery()) + "&" + arguments.getQueryString(), resolved.getFragment());
		} catch (URISyntaxException e) {
			throw new KirraException("Unexpected", e, KirraException.Kind.EXTERNAL);
		}
	}

	private List<?> retrieveData(Repository repository, Service service, Operation operation, Map<String, Object> argumentMap) {
		URI uri = getOperationURI(service, operation, argumentMap);
		GetMethod method = new GetMethod(uri.toString());
		LogUtils.logInfo(LegacyKirraMDDRestletApplication.ID, "Sending request to: " + uri, null);
		HttpClient httpClient = new HttpClient();
		try {
			int response = httpClient.executeMethod(method);
			if (response != 200) {
				LogUtils.logError(LegacyKirraMDDRestletApplication.ID, "Unexpected status for " + uri + ": " + response, null);
				return Arrays.asList();
			}
			JsonNode jsonValues = JsonHelper.parse(new InputStreamReader(method.getResponseBodyAsStream(), "UTF-8"));
			return TupleParser.getValuesFromJsonRepresentation(KirraRESTUtils.getRepository(), jsonValues, operation.getTypeRef(), operation.isMultiple());
		} catch (IOException e) {
			throw new KirraException("Error executing retriever " + operation + " (at " + uri + ")", e, KirraException.Kind.EXTERNAL);
		} finally {
			method.releaseConnection();
		}
	}

}
