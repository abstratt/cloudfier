package com.abstratt.mdd.frontend.web;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.routing.Router;
import org.restlet.routing.TemplateRoute;
import org.restlet.routing.Variable;

public class PublisherApplication extends Application {
    public static String ID = PublisherApplication.class.getPackage().getName();

    @Override
    public Restlet createInboundRoot() {
        getMetadataService().addExtension("multipart", MediaType.MULTIPART_FORM_DATA, true);
        Router router = new Router(getContext());
        router.attach(WebFrontEnd.PUBLISHER_SEGMENT + "{workspace}/", PublisherResource.class);
        router.attach(WebFrontEnd.PUBLISHER_SEGMENT + "{workspace}.zip", ArchiverResource.class);
        router.attach(WebFrontEnd.PUBLISHER_SEGMENT + "{workspace}/{file}", FileResource.class);
        router.attach(WebFrontEnd.IMPORTER_SEGMENT + "{workspace}/source/{source}", ImporterResource.class);        
        router.attach(WebFrontEnd.GENERATOR_SEGMENT + "{workspace}/platform/{platform}", GeneratorResource.class);
        router.attach(WebFrontEnd.GENERATOR_SEGMENT + "{workspace}/platform/{platform}/mapper/{artifact}", GeneratorResource.class);
        router.attach(WebFrontEnd.DIAGRAM_SEGMENT + "{workspace}/package/{file}", DiagramRendererResource.class);
               
        router.attach(WebFrontEnd.VALIDATOR_SEGMENT, ValidatorResource.class);
        router.attach(WebFrontEnd.FORMATTER_SEGMENT, FormatterResource.class);
        router.attach(WebFrontEnd.ANALYZER_SEGMENT, AnalyzerResource.class);
        router.attach(WebFrontEnd.DEPLOYER_SEGMENT, DeployerResource.class);
        router.attach(WebFrontEnd.STATUS_SEGMENT, StatusResource.class);

        TemplateRoute builderRoute = router.attach(WebFrontEnd.BUILDER_SEGMENT + "file{contextFile}", BuilderResource.class);
        builderRoute.getTemplate().getVariables().put("contextFile", new Variable(Variable.TYPE_URI_PATH));

        return router;
    }

}
