package com.abstratt.kirra.mdd.ui;

import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.routing.Router;
import org.restlet.routing.TemplateRoute;
import org.restlet.routing.Variable;

import com.abstratt.kirra.mdd.rest.KirraRepositoryFilter;
import com.abstratt.kirra.mdd.rest.impl.v1.LegacyKirraMDDRestletApplication;

public class KirraUIApplication extends LegacyKirraMDDRestletApplication {
    public static String ID = KirraUIApplication.class.getPackage().getName();

    public KirraUIApplication(Component component) {
        super(component);
    }

    @Override
    public Restlet createInboundRoot() {
        getMetadataService().addExtension("multipart", MediaType.MULTIPART_FORM_DATA, true);
        Router router = new Router(getContext());

        Restlet desktopIndex = createFinder(UIApplicationIndexResource.class);
        router.attach("/{workspace}/", desktopIndex);
        // expects path as query parameter
        router.attach("/", desktopIndex);

        KirraRepositoryFilter desktopAppResource = new KirraRepositoryFilter(createFinder(UIApplicationResource.class));
        router.attach("/{workspace}/root/source/class/kirra/Application.js", desktopAppResource);

        KirraRepositoryFilter desktopModuleResource = new KirraRepositoryFilter(createFinder(UIModuleResource.class));
        TemplateRoute builderRoute = router.attach("/{workspace}/root/source/module/{className}.js", desktopModuleResource);
        builderRoute.getTemplate().getVariables().put("entity", new Variable(Variable.TYPE_URI_PATH));

        KirraRepositoryFilter mobileAppResource = new KirraRepositoryFilter(createFinder(MobileUIApplicationResource.class));
        router.attach("/{workspace}/mobile/source/class/kirra/Application.js", mobileAppResource);

        router.attach("/{workspace}/root/source/", new ClassLoaderDirectory(getContext(), new LocalReference(
                "clap://thread/qooxdoo/source/")));
        router.attach("/qooxdoo/framework/source/", new ClassLoaderDirectory(getContext(), new LocalReference(
                "clap://thread/qooxdoo/framework/")));
        router.attach("/{workspace}/mobile/source/", new ClassLoaderDirectory(getContext(), new LocalReference(
                "clap://thread/qooxdoo_mobile/source/")));
        router.attach("/qooxdoo_mobile/framework/source/", new ClassLoaderDirectory(getContext(), new LocalReference(
                "clap://thread/qooxdoo_mobile/framework/")));

        return router;
    }

}
