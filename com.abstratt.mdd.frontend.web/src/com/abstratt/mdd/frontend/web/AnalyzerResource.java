package com.abstratt.mdd.frontend.web;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;

import com.abstratt.mdd.frontend.core.FrontEnd;
import com.abstratt.mdd.frontend.core.spi.ISourceAnalyzer.SourceElement;
import com.abstratt.pluginutils.LogUtils;

public class AnalyzerResource extends AbstractWorkspaceResource {
    @Post
    public Representation analyze(Representation entity) {
        if (entity == null) {
            return new EmptyRepresentation();
        }
        String filename = getQuery().getFirstValue("fileName");
        String defaultExtension = getQuery().getFirstValue("defaultExtension");
        String extension;
        if (filename != null) {
            extension = StringUtils.trimToNull(new Path(filename).getFileExtension());
            if (extension == null)
                extension = defaultExtension;
        } else
            extension = defaultExtension;
        String toAnalyze;
        try {
            toAnalyze = entity.getText();
        } catch (IOException e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        }
        ResourceUtils.ensure(extension != null, "Missing fileName", Status.CLIENT_ERROR_BAD_REQUEST);
        try {
            List<SourceElement> elements = FrontEnd.getCompilationDirector().analyze(extension, toAnalyze);
            return new StringRepresentation(JsonHelper.renderAsJson(elements));
        } catch (CoreException e) {
            LogUtils.logWarning(getClass().getPackage().getName(), "Core error", e);
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        }
    }
}
