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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.uml2.uml.Package;
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
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.modelrenderer.MapBackedSettingsSource;
import com.abstratt.mdd.modelrenderer.RenderingSettings;
import com.abstratt.mdd.modelrenderer.uml2dot.UML2DOT;
import com.abstratt.pluginutils.LogUtils;
import com.abstratt.resman.Resource;
import com.abstratt.resman.Task;

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
		String packageFile = (String) getRequestAttributes().get("file");
		String packageName = FilenameUtils.removeExtension(packageFile);
		
		return RepositoryService.DEFAULT.runTask(MDDUtil.fromJavaToEMF(workspaceDir.toURI()), new Task<Representation>() {
            @Override
            public Representation run(Resource<?> resource) {
                IRepository repository = RepositoryService.DEFAULT.getCurrentRepository();
                File repositoryLocation = new File(repository.getBaseURI().toFileString());
                Package packageToRender = repository.findPackage(packageName, null);
                ResourceUtils.ensure(packageToRender != null, "Package not found: " + packageName, Status.CLIENT_ERROR_NOT_FOUND);
                Map<String, String> diagramSettings = new HashMap<String, String>();
                diagramSettings.putAll(getQuery().getValuesMap());
                // project settings win
                loadDiagramSettings(diagramSettings, new File(repositoryLocation, IRepository.MDD_PROPERTIES));
                RenderingSettings settings = new RenderingSettings(new MapBackedSettingsSource(diagramSettings));
                URI packageUri = URI.create(packageToRender.getURI());
                byte[] dotContents;
                try {
                    dotContents = UML2DOT.generateDOTFromModel(packageUri, Arrays.asList(packageToRender), settings);
                } catch (CoreException e) {
                    ResourceUtils.fail(e, null);
                    // never runs
                    return null;
                }
                String expectedContentType = ((HttpRequest) getRequest())
                        .getHttpCall().getRequestHeaders().getValues(
                                HeaderConstants.HEADER_ACCEPT);
                boolean textOutputExpected = MediaType.TEXT_PLAIN.getName().equals(expectedContentType);
                if (textOutputExpected)
                    return new StringRepresentation(new String(dotContents));
                return convertDotToImageUsingGraphviz(dotContents);
            }
		});
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