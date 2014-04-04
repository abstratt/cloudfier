package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import com.abstratt.pluginutils.LogUtils;

/**
 * Archiver resource.
 * <p>
 * URI: http://localhost:8090/mdd/publisher/{workspace}.zip
 */
public class ArchiverResource extends AbstractWorkspaceResource {
	/**
	 * Returns information on the workspace, including number of packages and
	 * timestamp. Returns 404 if the workspace does not exist. Use ?classes=true
	 * to include information about classes in the model.
	 * @throws IOException 
	 */
	@Get("zip")
	public Representation getWorkspaceInfo() throws IOException {
		File workspaceDir = getWorkspaceDir(true);
		File[] workspaceFiles = listWorkspaceFiles();
		if (workspaceFiles.length == 0) {
			setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return new EmptyRepresentation();
		}
		checkSecret();
		return createZip(workspaceDir.getName(), workspaceFiles);
	}

	private Representation createZip(String project, File[] workspaceFiles) {
		Map<String, byte[]> files = new HashMap<String, byte[]>();
		try {
			for (File file : workspaceFiles)
				if (!file.getName().equals(SECRET_FILE))
					files.put(file.getName(),
							FileUtils.readFileToByteArray(file));
			return ResourceUtils.createZip(files, project);
		} catch (IOException e) {
			setStatus(Status.SERVER_ERROR_INTERNAL);
			LogUtils.logWarning(getClass().getPackage().getName(), "IO error",
					e);
			return ResourceUtils.buildErrorResponse(e);
		}
	}
}
