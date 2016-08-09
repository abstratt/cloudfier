package com.abstratt.mdd.frontend.web;

public interface WebFrontEnd {
    String ID = WebFrontEnd.class.getPackage().getName();
    String PORT = System.getProperty("org.eclipse.equinox.http.jetty.http.port");
    String BASE_PATH = "/services";
    String STATUS_SEGMENT = "/status";
    String BUILDER_SEGMENT = "/builder/";
    String DEPLOYER_SEGMENT = "/deployer/";
    String VALIDATOR_SEGMENT = "/validator/";
    String DIAGRAM_SEGMENT = "/diagram/";
    String PUBLISHER_SEGMENT = "/publisher/";
    String IMPORTER_SEGMENT = "/importer/";
    String GENERATOR_SEGMENT = "/generator/";
    String PLATFORM_SEGMENT = "platform";
    String MAPPER_SEGMENT = "mapper";
    String FORMATTER_SEGMENT = "/formatter/";
    String ANALYZER_SEGMENT = "/analyzer/";
    String DEPLOYER_PATH = ":" + WebFrontEnd.PORT + WebFrontEnd.BASE_PATH + WebFrontEnd.DEPLOYER_SEGMENT;
    String BUILDER_PATH = ":" + WebFrontEnd.PORT + WebFrontEnd.BASE_PATH + WebFrontEnd.BUILDER_SEGMENT;
    String VALIDATOR_PATH = ":" + WebFrontEnd.PORT + WebFrontEnd.BASE_PATH + WebFrontEnd.VALIDATOR_SEGMENT;
    String PUBLISHER_PATH = ":" + WebFrontEnd.PORT + WebFrontEnd.BASE_PATH + WebFrontEnd.PUBLISHER_SEGMENT;
    String DIAGRAM_PATH = ":" + WebFrontEnd.PORT + WebFrontEnd.BASE_PATH + WebFrontEnd.DIAGRAM_SEGMENT;
    String GENERATOR_PATH = ":" + WebFrontEnd.PORT + WebFrontEnd.BASE_PATH + WebFrontEnd.GENERATOR_SEGMENT;
    String STATUS_PATH = ":" + WebFrontEnd.PORT + WebFrontEnd.BASE_PATH + WebFrontEnd.STATUS_SEGMENT;

    String APP_API_SEGMENT = BASE_PATH + "/"+ Paths.API + "/";
    String APP_API2_SEGMENT = BASE_PATH + "/"+ Paths.API_V2 + "/";
    String APP_API_PATH = ":" + WebFrontEnd.PORT + WebFrontEnd.APP_API_SEGMENT;
    String APP_API2_PATH = ":" + WebFrontEnd.PORT + WebFrontEnd.APP_API2_SEGMENT;
}
