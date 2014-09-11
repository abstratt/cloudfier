package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import java.io.IOException;
import java.util.List;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.abstratt.kirra.Instance;

public class FinderResultResource extends AbstractKirraRepositoryResource {
    @Get("json")
    public Representation runFinderViaGET() throws IOException {
        return runFinder(true);
    }

    @Post("json")
    public Representation runFinderViaPOST(Representation request) throws IOException {
        return runFinder(false);
    }

    private Representation runFinder(boolean queryParameters) {
        String finderName = (String) getRequestAttributes().get("finderName");
        List<Instance> results = this.<Instance> executeOperation(getEntityNamespace(), getEntityName(), finderName, null, queryParameters);
        return buildInstanceList(getTargetEntity(), results);
    }
}
