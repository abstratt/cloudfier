package com.abstratt.kirra.mdd.rest.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.abstratt.kirra.Service;
import com.abstratt.kirra.mdd.rest.representation.ServiceLinkJSONRepresentation;
import com.abstratt.mdd.frontend.web.JsonHelper;

public class ServiceListResource extends AbstractKirraRepositoryResource {

    @Get
    public Representation list() throws IOException {
        List<Service> services = getRepository().getServices(null);
        List<ServiceLinkJSONRepresentation> links = new ArrayList<ServiceLinkJSONRepresentation>();
        Reference reference = getExternalReference();
        for (Service service : services) {
            ServiceLinkJSONRepresentation link = new ServiceLinkJSONRepresentation();
            link.name = service.getName();
            link.namespace = service.getNamespace();
            link.uri = reference.clone().addSegment(service.getNamespace() + '.' + service.getName()).toString();
            links.add(link);
        }
        return new StringRepresentation(JsonHelper.renderAsJson(links).toString(), MediaType.APPLICATION_JSON);
    }
}
