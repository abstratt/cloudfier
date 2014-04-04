package com.abstratt.mdd.frontend.web;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;

import com.abstratt.mdd.frontend.core.FrontEnd;
import com.abstratt.pluginutils.LogUtils;

public class FormatterResource extends AbstractWorkspaceResource {
	@Post
	public Representation format(Representation entity) {
		String filename = getQuery().getFirstValue("fileName");
		String defaultExtension = getQuery().getFirstValue("defaultExtension");
		String extension;
		if (filename != null) {
			extension = StringUtils.trimToNull(new Path(filename)
					.getFileExtension());
			if (extension == null)
				extension = defaultExtension;
		} else
			extension = defaultExtension;
		String toFormat;
		try {
			toFormat = entity.getText();
		} catch (IOException e) {
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return ResourceUtils.buildErrorResponse(e); 
		}
		if (extension == null)
			return new StringRepresentation(toFormat);
		try {
			String formatted = FrontEnd.getCompilationDirector()
					.format(extension, toFormat);
			System.out.println("Formatted: " + formatted);
			return new StringRepresentation(formatted);
		} catch (CoreException e) {
			LogUtils.logWarning(getClass().getPackage().getName(),
					"Core error", e);
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return ResourceUtils.buildErrorResponse(e);
		}
	}
}
