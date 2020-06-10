package com.abstratt.kirra.mdd.rest.impl.v1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
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
import com.abstratt.kirra.mdd.core.KirraMDDConstants;
import com.abstratt.kirra.mdd.rest.Activator;
import com.abstratt.kirra.mdd.rest.KirraRESTUtils;
import com.abstratt.kirra.mdd.rest.impl.v1.representation.TupleJSONRepresentation;
import com.abstratt.kirra.mdd.rest.impl.v1.representation.TupleJSONRepresentationBuilder;
import com.abstratt.kirra.mdd.rest.impl.v1.representation.TupleParser;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.ReferenceUtils;
import com.abstratt.pluginutils.LogUtils;
import com.fasterxml.jackson.databind.JsonNode;

public class KirraRESTExternalService implements ExternalService {
	
	private Executor executor = Executors.newFixedThreadPool(1);

    @Override
    public List<?> executeOperation(String namespace, String classifierName, String operationName, Map<String, Object> arguments) {
        Repository repository = KirraRESTUtils.getRepository();
        Service service = repository.getService(namespace, classifierName);
        Operation operation = service.getOperation(operationName);
        if (operation == null)
            throw new KirraException("Invalid service operation " + service.getTypeRef() + "#" + operationName, null,
                    KirraException.Kind.EXTERNAL);
        List<Parameter> parameters = operation.getParameters();
        int required = parameters.size();
        if (required != arguments.size())
            throw new KirraException("Service operation " + service.getTypeRef() + "#" + operationName + " requires " + required
                    + " parameters, got " + arguments.size(), null, KirraException.Kind.EXTERNAL);
        switch (operation.getKind()) {
        case Retriever:
            return retrieveData(repository, service, operation, arguments);
        case Event:
            pushEvent(repository, service, operation, arguments);
            return Arrays.asList();
        default:
            throw new KirraException("Invalid service operation " + service.getTypeRef() + "#" + operation.getName() + ": "
                    + operation.getKind(), null, KirraException.Kind.EXTERNAL);
        }
    }

    private URI getOperationURI(Service service, Operation operation, Map<String, Object> argumentMap) {
        String qName = service.getTypeRef().toString();
        URI resolved;
        String absoluteURI = KirraRESTUtils.getRepository().getProperties()
                .getProperty(KirraMDDConstants.EXTERNAL_CONNECTOR_URI + "/" + qName + "." + operation.getName());
        if (absoluteURI != null)
            resolved = URI.create(absoluteURI);
        else {
            String uriBase = KirraRESTUtils
                    .getRepository()
                    .getProperties()
                    .getProperty(KirraMDDConstants.EXTERNAL_CONNECTOR_URI,
                            URI.create(System.getProperty("mdd.external.connector.baseUri", "http://localhost")).resolve("external/").toString());

            URI connectorURI = URI.create(uriBase);
            String endpointType = operation.getKind() == OperationKind.Event ? "events" : "retrievers";
            resolved = connectorURI.resolve(endpointType + "/").resolve(qName + "/").resolve(operation.getName());
        }

        Form arguments = new Form();
        for (Map.Entry<String, Object> argument : argumentMap.entrySet())
            if (argument.getValue() != null)
                arguments.set(argument.getKey(), argument.getValue().toString());
        try {
            StringBuilder queryString = new StringBuilder();
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0)
                    queryString.append('&');
                queryString.append(arguments.get(i).getName());
                queryString.append('=');
                queryString.append(arguments.get(i).getValue());
            }
            return new URI(resolved.getScheme(), resolved.getUserInfo(), resolved.getHost(), resolved.getPort(), resolved.getPath(),
                    queryString.toString(), resolved.getFragment());
        } catch (URISyntaxException e) {
            throw new KirraException("Unexpected", e, KirraException.Kind.EXTERNAL);
        }
    }

    private void pushEvent(Repository repository, Service service, Operation operation, Map<String, Object> argumentMap) {
        Tuple event = (Tuple) argumentMap.values().iterator().next();
        URI uri = getOperationURI(service, operation, Collections.<String, Object> emptyMap());
        TupleType eventType = repository.getTupleType(event.getScopeNamespace(), event.getScopeName());
        executor.execute(() -> sendEvent(event, uri, eventType)); 
    }

	private void sendEvent(Tuple event, URI uri, TupleType eventType) {
		TupleJSONRepresentation representation = new TupleJSONRepresentation();
        new TupleJSONRepresentationBuilder().build(representation, null, eventType, event);
        PostMethod method = new PostMethod(uri.toString());
        HttpClient httpClient = new HttpClient();
        try {
        	String jsonRequest = JsonHelper.renderAsJson(representation);
            LogUtils.logInfo(LegacyKirraMDDRestletApplication.ID,
                    "Sending event to " + uri + " \nbody:\n" + jsonRequest, null);
            method.setRequestEntity(new StringRequestEntity(jsonRequest, "application/json", "UTF-8"));
            int response = executeHttpMethod(httpClient, method);
            if (response != 200)
                LogUtils.logError(LegacyKirraMDDRestletApplication.ID,
                        "Unexpected status for " + uri + ": " + response + "\n" + method.getResponseBodyAsString(), null);
            else
                LogUtils.logInfo(LegacyKirraMDDRestletApplication.ID,
                        "Successfully sent event to " + uri, null);

            // no use for response, not expected
        } catch (Exception e) {
        	LogUtils.logError(LegacyKirraMDDRestletApplication.ID,
                    "Exception sending event to " + uri, e);
        } finally {
            method.releaseConnection();
        }
	}

    private int executeHttpMethod(HttpClient httpClient, HttpMethod method) throws HttpException, IOException {
        int status = httpClient.executeMethod(method);
//        if (status == 301 || status == 302) {
//            Optional<String> location = Optional.ofNullable(method.getResponseHeader("Location")).map(Header::getValue);
//            if (location.isPresent()) {
//                org.apache.commons.httpclient.URI uri = new org.apache.commons.httpclient.URI(location.get(), false);
//                method.recycle();
//                method.setURI(uri);
//                int secondStatus = httpClient.executeMethod(method);
//                return secondStatus;
//            }
//        }
        return status;
    }

    private List<?> retrieveData(Repository repository, Service service, Operation operation, Map<String, Object> argumentMap) {
        URI uri = getOperationURI(service, operation, argumentMap);
        GetMethod method = new GetMethod(uri.toString());
        LogUtils.logInfo(Activator.ID, "Sending request to: " + uri, null);
        HttpClient httpClient = new HttpClient();
        try {
            int response = executeHttpMethod(httpClient, method);
            if (response != 200) {
                LogUtils.logError(Activator.ID, "Unexpected status for " + uri + ": " + response, null);
                return Arrays.asList();
            }
            JsonNode jsonValues = JsonHelper.parse(new InputStreamReader(method.getResponseBodyAsStream(), "UTF-8"));
            LogUtils.logInfo(Activator.ID, "Response: " + jsonValues, null);
            return new TupleParser(KirraRESTUtils.getRepository()).getValuesFromJsonRepresentation(jsonValues, operation.getTypeRef(),
                    operation.isMultiple());
        } catch (IOException e) {
            throw new KirraException("Error executing retriever " + operation + " (at " + uri + ")", e, KirraException.Kind.EXTERNAL);
        } finally {
            method.releaseConnection();
        }
    }

}
