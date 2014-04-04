package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.abstratt.mdd.frontend.core.IProblem;

/**
 * Validator resource.
 * <p>
 * URI: http://localhost:8090/mdd/validator/
 */
public class ValidatorResource extends ServerResource {
	
	/**
	 * Takes either a new-line separated list of URIs to compilation units
	 * (text/plain) or a multipart form (file upload). Returns a XML response
	 * with the result of the compilation. Leaves no trace on the server.
	 */
	@Post
	public Representation validate(Representation entity) {
		File tmpProjectDir = null;
		try {
			tmpProjectDir = new File(ResourceUtils.createTempDir("kirra"), "scratch");
			tmpProjectDir.mkdirs();
			MediaType mediaType = getRequest().getEntity().getMediaType();
			List<String[]> ignored = new ArrayList<String[]>();
			if (MediaType.MULTIPART_FORM_DATA.equals(mediaType, true)) {
				RestletFileUpload upload = new RestletFileUpload(
						new DiskFileItemFactory());
				List<FileItem> items = upload.parseRequest(getRequest());
				for (FileItem fileItem : items)
					if (!fileItem.isFormField()) {
						String fileClientPath = fileItem.getName();
						fileItem.write(new File(tmpProjectDir, fileClientPath));
					}
			} else if (MediaType.APPLICATION_WWW_FORM.equals(mediaType, true)) {
				String locations = (String) getRequest().getEntityAsText();
				String[] allLocations = locations.split("\n");
				ignored = ResourceUtils.fetchUnits(tmpProjectDir, allLocations);
			} else {
				// POST request with no entity.
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			}
			long start = System.currentTimeMillis();
			List<IProblem> results = ResourceUtils.compile(tmpProjectDir,
					getQuery().getValuesMap());
			long end = System.currentTimeMillis();
			double elapsedTime = (end - start) / 1000d;
			StringBuffer result = ResourceUtils.buildResponse(ignored, results,
					elapsedTime);
			return new StringRepresentation(result.toString(),
					MediaType.TEXT_XML);
		} catch (HttpException e) {
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return ResourceUtils.buildErrorResponse(e);
		} catch (IOException e) {
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return ResourceUtils.buildErrorResponse(e);
		} catch (CoreException e) {
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return ResourceUtils.buildErrorResponse(e);
		} catch (FileUploadException e) {
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return ResourceUtils.buildErrorResponse(e);
		} catch (Exception e) {
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return ResourceUtils.buildErrorResponse(e);
		} finally {
			FileUtils.deleteQuietly(tmpProjectDir);
		}
	}
}
