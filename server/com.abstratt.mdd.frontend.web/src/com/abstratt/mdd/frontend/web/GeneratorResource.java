package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.uml2.uml.Classifier;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.target.OutputHolder;
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.resman.Resource;
import com.abstratt.resman.Task;

public class GeneratorResource extends AbstractWorkspaceResource {
	@Get
	public Representation generateWorkspace() throws IOException {
		final String platformId = (String) getRequestAttributes().get("platform");
		if (platformId == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation("platform parameter is required");
		}
        final ITargetPlatform platform = TargetCore.getPlatform(new Properties(), platformId);
        if (platform == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return new StringRepresentation("platform not found: " + platformId);
        }
        // it is optional - default is all mappers supported
        final String artifactType = (String) getRequestAttributes().get("artifact");
		File workspaceDir = getWorkspaceDir(true);
		SortedMap<String, byte[]> result = RepositoryService.DEFAULT.runTask(MDDUtil.fromJavaToEMF(workspaceDir.toURI()), new Task<SortedMap<String,  byte[]>>() {
		    @Override
		    public SortedMap<String,  byte[]> run(Resource<?> resource) {
		        IRepository repository = RepositoryService.DEFAULT.getCurrentRepository();
		        SortedMap<String,  byte[]> result = new TreeMap<String, byte[]>();
	            Collection<String> artifactTypesToGenerate = artifactType == null ? platform.getArtifactTypes() : Arrays.asList(artifactType);
                for (String artifactType : artifactTypesToGenerate) {
                    ITopLevelMapper<Classifier> mapper = platform.getMapper(artifactType);
                    try {
                        for (Map.Entry<String, OutputHolder<?>> entry : mapper.mapMultiple(repository).entrySet())
                            result.put(entry.getKey(), entry.getValue().getBytes());
                    } catch (RuntimeException re) {
                        log.error("Error mapping to platform "+ platform.getId() + " and artifact type " + artifactType + " for repository " + repository.getBaseURI(), re);
                        StringWriter out = new StringWriter();
                        re.printStackTrace(new PrintWriter(out));
                        result.put("_error_" + artifactType, out.toString().getBytes());
                    }
	            }
                return result;
		    }
        });
		
		String expectedContentType = ((HttpRequest) getRequest()).getHttpCall()
		.getRequestHeaders().getValues(
				HeaderConstants.HEADER_ACCEPT);
		boolean zipFormat = (MediaType.ALL.getName().equals(expectedContentType) && result.size() > 1) 
		        || MediaType.APPLICATION_OCTET_STREAM.getName()
				    .equals(expectedContentType)
				|| MediaType.APPLICATION_ZIP.getName().equals(
						expectedContentType)
				|| (expectedContentType == null && result.size() > 1);

		if (zipFormat) {
			String fileName = "generated-" +platformId;
			if (artifactType != null)
			    fileName += "-" + artifactType;
            return ResourceUtils.createZip(result, fileName);
		}
		StringBuffer resultString = new StringBuffer();
		for (byte[] each : result.values()) {
			resultString.append("\n");
			resultString.append(new String(each));
		}
		return new StringRepresentation(resultString.toString());
	}
}