package com.abstratt.kirra.mdd.rest;

import org.apache.commons.lang.StringUtils;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.restlet.security.Authenticator;
import org.restlet.service.LogService;

public class LegacyKirraMDDRestletApplication extends Application {
    public static String ID = LegacyKirraMDDRestletApplication.class.getPackage().getName();
    private static boolean SHOW_TIMING = Boolean.parseBoolean(System.getProperty("kirra.timing", "true"));
    private KirraStatusService customStatusService;
    private LogService customLogService;
    private Component component;

    public LegacyKirraMDDRestletApplication(Component component) {
        customStatusService = new KirraStatusService();
        customLogService = new LogService(false);
        this.component = component;
        this.component.setStatusService(customStatusService);
        this.component.setLogService(customLogService);
        this.getServices().remove(this.getStatusService());
        this.setStatusService(customStatusService);
    }

    @Override
    public Restlet createInboundRoot() {
        getMetadataService().addExtension("multipart", MediaType.MULTIPART_FORM_DATA, true);
        Router router = new Router(getContext());
        attachTo(router, "/{workspace}/", createRestlet(IndexResource.class, true, false));
        // takes path in query
        attachTo(router, "/", createFinder(IndexResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.DATA, createRestlet(DataResource.class, false, true));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.TESTS, createRestlet(TestResource.class, false, true));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.TESTS + "/{testClassName}/{testCaseName}",
                createRestlet(TestRunnerResource.class, false, false));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.SIGNUP, createRestlet(SignupResource.class, false, false));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.PASSWORD_RESET,
                createRestlet(PasswordResetResource.class, false, false));
        Restlet loginLogout = createRestlet(LoginLogoutResource.class, true, false);
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.LOGIN, loginLogout);
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.LOGOUT, loginLogout);
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.PROFILE, createRestlet(ProfileResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.ENTITIES + "/", createRestlet(EntityListResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.ENTITIES + "/{entityNamespace}.{entityName}",
                createRestlet(EntityResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.SERVICES + "/", createRestlet(ServiceListResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.SERVICES + "/{entityNamespace}.{entityName}",
                createRestlet(ServiceResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.INSTANCES + "/{entityNamespace}.{entityName}/",
                createRestlet(InstanceListResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.FINDERS + "/{entityNamespace}.{entityName}/{finderName}",
                createRestlet(FinderResultResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.RETRIEVERS
                + "/{entityNamespace}.{entityName}/{retrieverName}", createRestlet(ServiceInvocationResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.ACTIONS + "/{entityNamespace}.{entityName}/{actionName}",
                createRestlet(StaticActionResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.INSTANCES + "/{entityNamespace}.{entityName}/{objectId}",
                createRestlet(InstanceResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.INSTANCES + "/{entityNamespace}.{entityName}/{objectId}/"
                + com.abstratt.mdd.frontend.web.Paths.RELATIONSHIPS + "/{relationshipName}/" + com.abstratt.mdd.frontend.web.Paths.DOMAIN,
                createRestlet(RelationshipDomainResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.INSTANCES + "/{entityNamespace}.{entityName}/{objectId}/"
                + com.abstratt.mdd.frontend.web.Paths.RELATIONSHIPS + "/{relationshipName}/{relatedId}",
                createRestlet(RelatedInstanceResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.INSTANCES + "/{entityNamespace}.{entityName}/{objectId}/"
                + com.abstratt.mdd.frontend.web.Paths.ACTIONS + "/{actionName}", createRestlet(InstanceActionResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.INSTANCES + "/{entityNamespace}.{entityName}/{objectId}/"
                + com.abstratt.mdd.frontend.web.Paths.ACTIONS + "/{actionName}/" + com.abstratt.mdd.frontend.web.Paths.PARAMETERS
                + "/{parameterName}/" + com.abstratt.mdd.frontend.web.Paths.DOMAIN, createRestlet(ParameterDomainResource.class));
        attachTo(router, "/{workspace}/" + com.abstratt.mdd.frontend.web.Paths.INSTANCES + "/{entityNamespace}.{entityName}/{objectId}/"
                + com.abstratt.mdd.frontend.web.Paths.RELATIONSHIPS + "/{relationshipName}/",
                createRestlet(RelatedInstanceListResource.class));
        return router;
    }

    public Restlet createRestlet(Class<?> clazz) {
        return createRestlet(clazz, true, true);
    }

    public Restlet createRestlet(Class<?> clazz, boolean authenticated, boolean repository) {
        Restlet created = createFinder((Class<? extends ServerResource>) clazz);
        created = new KirraSetExpirationFilter(created);
        if (repository) {
            created = new KirraRepositoryFilter(created);
        }
        if (authenticated) {
            Authenticator guard = new KirraAuthenticator();
            guard.setNext(created);
            created = guard;
        }
        if (LegacyKirraMDDRestletApplication.SHOW_TIMING) {
            created = KirraTimingFilter.monitor(created);
        }
        return created;
    }

    private void attachTo(Router router, String pathTemplate, Restlet resource) {
        router.attach(pathTemplate, resource);
        if (pathTemplate.endsWith("/") && !"/".equals(pathTemplate))
            // so clients that omit the trailing slash don't get burned
            router.attach(StringUtils.stripEnd(pathTemplate, "/"), resource);
    }
}
