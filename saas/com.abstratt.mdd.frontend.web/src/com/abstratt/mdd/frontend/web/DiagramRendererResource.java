package com.abstratt.mdd.frontend.web;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.abstratt.graphviz.GraphViz;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.modelrenderer.uml2dot.UML2DOT;
import com.abstratt.modelrenderer.MapBackedSettingsSource;
import com.abstratt.modelrenderer.RenderingSettings;
import com.abstratt.pluginutils.LogUtils;

public class DiagramRendererResource extends AbstractWorkspaceResource {
    private static final String DIAGRAM_SETTING_PREFIX = "mdd.diagram.";

    @Get(value="text/plain|image/png")
    public Representation render() {
		File workspaceDir = getWorkspaceDir();
		if (workspaceDir == null) {
			setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return new EmptyRepresentation();
		}
		
		checkSecret();
		String fileName = (String) getRequestAttributes().get("file");
		URI fileURI = URI.create("/" + fileName);
		File modelFile = new File(workspaceDir, fileURI.getPath());
		if (!modelFile.getParentFile().equals(workspaceDir) || !modelFile.isFile() || !modelFile.getName().endsWith(".uml")) {
			setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		    return new EmptyRepresentation();
		}
		URI modelURI = modelFile.toURI();
    	byte[] dotContents;
		try {
	        Map<String, String> diagramSettings = new HashMap<String, String>();
	        diagramSettings.putAll(getQuery().getValuesMap());
	        // project settings win
            loadDiagramSettings(diagramSettings, new File(modelFile.getParentFile(), IRepository.MDD_PROPERTIES));
            RenderingSettings settings = new RenderingSettings(new MapBackedSettingsSource(diagramSettings));
			dotContents = UML2DOT.generateDOTFromModel(modelURI, settings);
			String expectedContentType = ((HttpRequest) getRequest())
					.getHttpCall().getRequestHeaders().getValues(
							HeaderConstants.HEADER_ACCEPT);
			if (!MediaType.TEXT_PLAIN.getName().equals(expectedContentType))
				return convertDotToImageUsingGraphviz(dotContents);
			return new StringRepresentation(new String(dotContents));
		} catch (CoreException e) {
            LogUtils.logWarning(getClass().getPackage().getName(), "Core error", e);
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
		}
    }

    private void loadDiagramSettings(Map<String, String> diagramSettings, File propertiesFile) {
        if (propertiesFile.isFile()) {
            try {
                Properties properties = new Properties();
                properties.load(new ByteArrayInputStream(FileUtils.readFileToByteArray(propertiesFile)));
                for (Object property : properties.keySet()) {
                    String propertyAsString = (String) property;
                    if (propertyAsString.startsWith(DIAGRAM_SETTING_PREFIX) && propertyAsString.length() > DIAGRAM_SETTING_PREFIX.length()) {
                        String diagramSetting = propertyAsString.substring(DIAGRAM_SETTING_PREFIX.length());
                        diagramSettings.put(diagramSetting, properties.getProperty(propertyAsString));
                    }
                }
            } catch (IOException e) {
                LogUtils.logWarning(WebFrontEnd.ID, "Error loading properties file", e);
            }
		}
    }

    private Representation convertDotToImageUsingGraphviz(byte[] dotContents) {
        Path baseDir = new File(System.getProperty("java.io.tmpdir")).toPath().resolve("kirra");
        Path outputLocation;
        try {
            Files.createDirectories(baseDir);
            outputLocation = Files.createTempFile(baseDir, "graphviz", ".png");
            GraphViz.generate(new ByteArrayInputStream(dotContents), "png", 0, 0, new org.eclipse.core.runtime.Path(outputLocation.toString()));
            byte[] imageContents;
            try {
                imageContents = FileUtils.readFileToByteArray(outputLocation.toFile());
            } finally {
                outputLocation.toFile().delete();
            }
            setStatus(new Status(200));
            return new InputRepresentation(new ByteArrayInputStream(imageContents), MediaType.IMAGE_PNG);
        } catch (IOException | CoreException e) {
            LogUtils.logWarning(getClass().getPackage().getName(), "Core error", e);
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        }
    }
    
	private Representation convertDotToImageUsingGoogleChart(byte[] dotContents) {
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod("http://chart.apis.google.com/chart?chid=" + Math.random());
		
        NameValuePair[] data = {
                new NameValuePair("cht", "gv"),
                new NameValuePair("chl", new String(dotContents))
              };
        post.setRequestBody(data);
        int postStatusCode;
        InputStream responseBodyAsStream;
		try {
			postStatusCode = client.executeMethod(post);
			responseBodyAsStream = post.getResponseBodyAsStream();
		} catch (HttpException e) {
		    LogUtils.logWarning(getClass().getPackage().getName(), "Core error", e);
            setStatus(Status.SERVER_ERROR_INTERNAL);
		    return ResourceUtils.buildErrorResponse(e);
		} catch (IOException e) {
		    LogUtils.logWarning(getClass().getPackage().getName(), "Core error", e);
            setStatus(Status.SERVER_ERROR_INTERNAL);
		    return ResourceUtils.buildErrorResponse(e);
		}
       	setStatus(new Status(postStatusCode));
       	Header postContentType = post.getResponseHeader(HeaderConstants.HEADER_CONTENT_TYPE);
       	MediaType responseContentType = postContentType == null ? null : MediaType.valueOf(postContentType.getValue());
		return new InputRepresentation(responseBodyAsStream, responseContentType);
	}
}