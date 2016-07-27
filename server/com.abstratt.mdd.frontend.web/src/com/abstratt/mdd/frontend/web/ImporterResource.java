package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.IOException;	
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.importer.jdbc.JDBCImporter;
import com.abstratt.resman.Resource;
import com.abstratt.resman.ResourceException;
import com.abstratt.resman.Task;

public class ImporterResource extends AbstractWorkspaceResource {
	@Get
	public Representation importProject() throws IOException {
		final String sourceId = (String) getRequestAttributes().get("source");
		if (sourceId == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation("source parameter is required");
		}
		String snapshotPath = getQueryValue("snapshot");
		if (StringUtils.isBlank(snapshotPath)) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation("snapshotPath parameter is required");
		}
		//TODO-RC support for multiple kinds of importers
		if (!"schema-crawler".equals(sourceId)) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return new StringRepresentation("importer not found: " + sourceId);
		}
		File workspaceDir = getWorkspaceDir(true);
		SortedMap<String, byte[]> result = RepositoryService.DEFAULT.runTask(MDDUtil.fromJavaToEMF(workspaceDir.toURI()), new Task<SortedMap<String,  byte[]>>() {
		    @Override
		    public SortedMap<String,  byte[]> run(Resource<?> resource) {
		        IRepository repository = RepositoryService.DEFAULT.getCurrentRepository();
		        
		        Properties properties = repository.getProperties();
				JDBCImporter importer = new JDBCImporter(properties);
		        
		        IFileStore sourcePath = BuildDirectoryUtils.getSourcePath(new Path(snapshotPath));
		        
		        if (!sourcePath.fetchInfo().exists())
		        	throw new ResourceException("No snapshot file at " + snapshotPath);
		        
		        if (sourcePath.fetchInfo().isDirectory())
		        	throw new ResourceException("A directory was found at " + snapshotPath);
		        
		        try {
					Map<String, CharSequence> imported = importer.importModelFromSnapshot(sourcePath.toLocalFile(0, null));
					return imported.entrySet().stream().collect(Collectors.toMap(
							entry -> entry.getKey(), 
							entry -> entry.getValue().toString().getBytes(),
							(a, b) -> a,
							() -> new TreeMap<>()
					));
				} catch (CoreException e) {
					throw new ResourceException(e);
				}
		    }
        });
		

		String fileName = "imported";
		return ResourceUtils.buildMultiFileResult(getRequest(), fileName ,result);
	}
}
