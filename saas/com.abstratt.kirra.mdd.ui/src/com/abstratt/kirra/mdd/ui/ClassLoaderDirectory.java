package com.abstratt.kirra.mdd.ui;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.Directory;

import com.abstratt.kirra.mdd.rest.KirraRESTUtils;
import com.abstratt.mdd.frontend.web.ResourceUtils;

// http://maxrohde.com/2010/09/29/clap-protocol-in-restlet-and-osgi/
public class ClassLoaderDirectory extends Directory {

    final static ClassLoader classLoader = new ClassLoader(ClassLoaderDirectory.class.getClassLoader()) {
        @Override
        public URL getResource(String name) {
            URL resource = super.getResource(name);
            try {
                return resource == null ? null : FileLocator.resolve(resource);
            } catch (IOException e) {
                return resource;
            }
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            throw new UnsupportedOperationException();
        }
    };

    public ClassLoaderDirectory(Context context, Reference rootLocalReference) {
        super(context, rootLocalReference);
        setIndexName("index.html");
        setListingAllowed(false);
    }

    @Override
    public void handle(Request request, Response response) {
        String workspace = ResourceUtils.getWorkspaceFromProjectPath(request, false);
        ResourceUtils.ensure(StringUtils.isBlank(workspace) || KirraRESTUtils.doesWorkspaceExist(workspace), "Application not found: "
                + workspace, Status.CLIENT_ERROR_NOT_FOUND);
        final ClassLoader saveCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ClassLoaderDirectory.classLoader);
        try {
            super.handle(request, response);
            // let proxy set it
            if (response.getEntity() != null)
                response.getEntity().setExpirationDate(null);
        } finally {
            Thread.currentThread().setContextClassLoader(saveCL);
        }
    }

}