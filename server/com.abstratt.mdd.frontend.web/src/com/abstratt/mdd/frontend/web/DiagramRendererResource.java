package com.abstratt.mdd.frontend.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
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

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.target.doc.DiagramGenerator;
import com.abstratt.pluginutils.LogUtils;
import com.abstratt.resman.Resource;
import com.abstratt.resman.Task;

public class DiagramRendererResource extends AbstractWorkspaceResource {
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
                Map<String, String> diagramSettings = new HashMap<String, String>();
                diagramSettings.putAll(getQuery().getValuesMap());
                Package packageToRender;
                if (packageName == null) {
                	Package[] ownPackages = repository.getOwnPackages(null);
                	packageToRender = Arrays.stream(ownPackages).filter(it -> MDDExtensionUtils.isApplication(it)).findAny().orElse(null);
                	if (packageToRender == null && ownPackages.length > 0)
                		packageToRender = ownPackages[0];
                } else
                	packageToRender = repository.findPackage(packageName, null);
                ResourceUtils.ensure(packageToRender != null, "Unknown package", Status.CLIENT_ERROR_NOT_FOUND);
                	
				LogUtils.logInfo(PublisherApplication.ID, "Package selected: " + Optional.ofNullable(packageToRender).map(it -> it.getName()).orElse(null), null);
                DiagramGenerator diagramGenerator = new DiagramGenerator(repository);

                String expectedContentType = ((HttpRequest) getRequest())
                        .getHttpCall().getRequestHeaders().getValues(
                                HeaderConstants.HEADER_ACCEPT);
                boolean textOutputExpected = MediaType.TEXT_PLAIN.getName().equals(expectedContentType);

                try {
	                if (textOutputExpected) {
	                	byte[] dotContents = diagramGenerator.generateDiagramAsDot(diagramSettings, packageToRender);
	                    return new StringRepresentation(new String(dotContents));
	                }
	                byte[] imageContents = diagramGenerator.generateDiagramAsImage(diagramSettings, packageToRender);
	                return new InputRepresentation(new ByteArrayInputStream(imageContents), MediaType.IMAGE_PNG);
                } catch (CoreException e) {
                	setStatus(Status.SERVER_ERROR_INTERNAL);
					String message = (e.getStatus().isMultiStatus() ? ((MultiStatus) (e.getStatus())).getChildren()[0] : e.getStatus()).getMessage();
                	if (textOutputExpected) {
            			return new StringRepresentation(message);
                	}
                	try {
	                	byte[] imageContents = diagramGenerator.generateTextAsImage(message);
		                InputRepresentation errorRepresentation = new InputRepresentation(new ByteArrayInputStream(imageContents), MediaType.IMAGE_PNG);
						return errorRepresentation;
                	} catch (CoreException e2) {
                		return new StringRepresentation(message);
                	}
                }
            }
		});
    }

    /*
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
	*/
}