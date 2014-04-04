package com.abstratt.mdd.frontend.web;

public interface WebFrontEnd {
	String ID = WebFrontEnd.class.getPackage().getName();
	String PORT = System
			.getProperty("org.eclipse.equinox.http.jetty.http.port");
	String BASE_PATH = "/mdd";
	String STATUS_SEGMENT = "/status";
	String BUILDER_SEGMENT = "/builder/";
	String DEPLOYER_SEGMENT = "/deployer/";
	String VALIDATOR_SEGMENT = "/validator/";
	String DIAGRAM_SEGMENT = "/diagram/";
	String PUBLISHER_SEGMENT = "/publisher/";
	String GENERATOR_SEGMENT = "/generator/";
	String FORMATTER_SEGMENT = "/formatter/";
	String ANALYZER_SEGMENT = "/analyzer/";
	String DEPLOYER_PATH = ":" + PORT + BASE_PATH + DEPLOYER_SEGMENT;
	String BUILDER_PATH = ":" + PORT + BASE_PATH + BUILDER_SEGMENT;
	String VALIDATOR_PATH = ":" + PORT + BASE_PATH + VALIDATOR_SEGMENT;
	String PUBLISHER_PATH = ":" + PORT + BASE_PATH + PUBLISHER_SEGMENT;
	String DIAGRAM_PATH = ":" + PORT + BASE_PATH + DIAGRAM_SEGMENT;
	String GENERATOR_PATH = ":" + PORT + BASE_PATH + GENERATOR_SEGMENT;
	String STATUS_PATH = ":" + PORT + BASE_PATH + STATUS_SEGMENT;

	
	String APP_API_SEGMENT = "/mdd/" + Paths.API + "/";
	String APP_API_PATH = ":" + WebFrontEnd.PORT + APP_API_SEGMENT;
}
