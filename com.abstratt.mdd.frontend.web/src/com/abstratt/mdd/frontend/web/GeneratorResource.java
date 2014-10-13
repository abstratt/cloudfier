package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.UMLPackage;
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
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.core.util.TemplateUtils;
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
		SortedMap<String, byte[]> result = RepositoryService.DEFAULT.runTask(MDDUtil.fromJavaToEMF(workspaceDir.toURI()), new Task<SortedMap<String, byte[]>>() {
		    @Override
		    public SortedMap<String, byte[]> run(Resource<?> resource) {
		        IRepository repository = RepositoryService.DEFAULT.getCurrentRepository();
		        final List<String> toGenerate = Arrays.asList(getQuery().getValuesArray("class"));
		        SortedMap<String, byte[]> result = new TreeMap<String, byte[]>();
	            final List<Classifier> userClasses = repository.findAll(
	                    new EObjectCondition() {
	                        @Override
	                        public boolean isSatisfied(EObject eObject) {
	                            if (UMLPackage.Literals.CLASS != eObject.eClass())
	                                return false;
	                            Classifier asClassifier = (Classifier) eObject;
	                            return asClassifier.getName() != null && !TemplateUtils.isTemplateInstance(asClassifier) && (toGenerate.isEmpty() || toGenerate.contains(asClassifier.getQualifiedName()));
	                        }
	                    }, true);
	            
	            Collection<String> artifactTypesToGenerate = artifactType == null ? platform.getArtifactTypes() : Arrays.asList(artifactType);
	            for (Classifier each : userClasses) {
	                for (String artifactType : artifactTypesToGenerate) {
	                    ITopLevelMapper<Classifier> mapper = platform.getMapper(artifactType);
	                    ResourceUtils.ensure(mapper != null, "No mapper for artifact type: "+ artifactType, null);
	                    if (mapper.canMap(each)) {
    	                    String mapped;
    	                    try {
    	                        mapped = mapper.map(each).toString();
    	                    } catch (RuntimeException re) {
    	                        log.error("Error rendering " + each.getQualifiedName() + " with platform "+ platform.getId() + " for repository " + repository.getBaseURI(), re);
    	                        mapped = re.toString();
    	                    }
    	                    if (mapped != null) {
    	                        String fileName = mapper.mapFileName(each);
    	                        result.put(fileName, mapped.getBytes());
    	                    }
	                    }
	                }
	            }
                return result;
		    }
        });
		
		String expectedContentType = ((HttpRequest) getRequest()).getHttpCall()
		.getRequestHeaders().getValues(
				HeaderConstants.HEADER_CONTENT_TYPE);
		boolean zipFormat = MediaType.APPLICATION_OCTET_STREAM.getName()
				.equals(expectedContentType)
				|| MediaType.APPLICATION_ZIP.getName().equals(
						expectedContentType)
				|| (expectedContentType == null && result.size() > 1);

		if (zipFormat)
			return ResourceUtils.createZip(result, "generated-" +platformId);
		StringBuffer resultString = new StringBuffer();
		for (byte[] each : result.values()) {
			resultString.append("\n");
			resultString.append(new String(each));
		}
		return new StringRepresentation(resultString.toString());
	}
}