package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.abstratt.mdd.core.IProblem;
import com.abstratt.mdd.core.UnclassifiedProblem;
import com.abstratt.mdd.core.IProblem.Severity;
import com.abstratt.pluginutils.LogUtils;

public class BuilderResource extends AbstractBuildDirectoryResource {

    /**
     * Validates a local project (as opposed to contents provided by the
     * request). Will only report problems on the relevant file.
     */
    @Get("json")
    public Representation build() {
        File tmpProjectDir = null;
        try {
            Path contextFilePath = new Path(URLDecoder.decode((String) getRequestAttributes().get("contextFile"), "UTF-8"));

            IFileStore contextFile = BuildDirectoryUtils.getSourcePath(contextFilePath);
            IPath userPath = new Path(contextFilePath.toString().replaceFirst("/file/", "/"));
            ResourceUtils.ensure(contextFile != null, "No valid project found for " + userPath, Status.SERVER_ERROR_INTERNAL);
            ResourceUtils.ensure(contextFile.fetchInfo().exists(), contextFile.toString(), Status.CLIENT_ERROR_NOT_FOUND);
            ResourceUtils.ensure(!contextFile.fetchInfo().isDirectory(), contextFile.toString(), Status.CLIENT_ERROR_BAD_REQUEST);

            String contextFileName = contextFile.getName();
            tmpProjectDir = ResourceUtils.prepareTempProjectDir(null, contextFile);
            List<IProblem> results = ResourceUtils.compile(tmpProjectDir, getQuery().getValuesMap());
            List<IProblem> relevantResults = filterRelevantResults(contextFileName, results);
            String result = ResourceUtils.buildJSONResponse(relevantResults);
            return new StringRepresentation(result, MediaType.APPLICATION_JSON);
        } catch (IOException e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        } catch (CoreException e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        } catch (RuntimeException e) {
            LogUtils.logError(getClass().getPackage().getName(), null, e);
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        } finally {
            ResourceUtils.releaseTempProjectDir(tmpProjectDir);
        }
    }

    protected List<IProblem> filterRelevantResults(String contextFileName, List<IProblem> results) {
        List<IProblem> relevantResults = new ArrayList<IProblem>();
        List<IProblem> projectWarnings = new ArrayList<IProblem>();
        for (IProblem problem : results)
            if (problem.getSeverity() != Severity.INFO) {
                Object fileName = problem.getAttribute(IProblem.FILE_NAME);
                String fileNameString = fileName instanceof IFileStore ? ((IFileStore) fileName).getName() : "" + fileName;

                if (fileName != null && !fileNameString.equals(contextFileName)) {
                    IProblem related = new UnclassifiedProblem(Severity.WARNING, "Problem in related file '" + fileNameString + "': "
                            + problem.getMessage());
                    projectWarnings.add(related);
                } else
                    relevantResults.add(problem);
            }
        relevantResults.addAll(projectWarnings);
        return relevantResults;
    }
}
