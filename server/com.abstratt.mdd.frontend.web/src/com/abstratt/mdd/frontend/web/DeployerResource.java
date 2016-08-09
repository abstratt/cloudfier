package com.abstratt.mdd.frontend.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.abstratt.mdd.core.IProblem;
import com.abstratt.mdd.core.IProblem.Severity;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.util.MDDUtil;

public class DeployerResource extends AbstractBuildDirectoryResource {

    @Post
    public Representation deploy(Representation request) {
        IPath userPath = getUserPath();
        IFileStore sourcePath = getSourcePath(userPath);
        // project file -> project directory
        File tmpProjectDir = null;
        try {
            tmpProjectDir = ResourceUtils.prepareTempProjectDir(userPath, sourcePath.getChild(IRepository.MDD_PROPERTIES));
            List<IProblem> results = ResourceUtils.compile(tmpProjectDir, getQuery().getValuesMap());
            for (IProblem current : results)
                if (current.getSeverity() == Severity.ERROR) {
                    return new StringRepresentation(ResourceUtils.buildJSONResponse(results), MediaType.APPLICATION_JSON);
                }
            File deployDirectory = BuildDirectoryUtils.getDeployDirectory(userPath);
            RepositoryService.DEFAULT.unregisterRepository(MDDUtil.fromJavaToEMF(deployDirectory.toURI()));
            BuildDirectoryUtils.replaceDirectory(tmpProjectDir, deployDirectory);
            return new StringRepresentation(ResourceUtils.buildJSONResponse(results), MediaType.APPLICATION_JSON);
        } catch (IOException e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        } catch (CoreException e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        } catch (RuntimeException e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        } finally {
            ResourceUtils.releaseTempProjectDir(tmpProjectDir);
        }
    }

    @Get
    public Representation getAppInfo() {
        IPath userPath = getUserPath();
        File deployDirectory = BuildDirectoryUtils.getDeployDirectory(userPath);
        ResourceUtils.ensure(BuildDirectoryUtils.isDeployDirectory(deployDirectory), "No deployed application found",
                Status.CLIENT_ERROR_NOT_FOUND);
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("deployment_date", new Date(deployDirectory.lastModified()).toString());
        List<String> packageNames = new ArrayList<String>();
        String[] filenames = deployDirectory.list();
        for (String filename : filenames)
            if ("uml".equals(FilenameUtils.getExtension(filename)))
                packageNames.add(FilenameUtils.getBaseName(filename));
        info.put("packages", packageNames);

        Properties properties = new Properties();

        File propertiesFile = new File(deployDirectory, IRepository.MDD_PROPERTIES);
        try {
            properties.load(new ByteArrayInputStream(FileUtils.readFileToByteArray(propertiesFile)));
        } catch (IOException e) {
            ResourceUtils.ensure(false, "Error reading project metadata", Status.SERVER_ERROR_INTERNAL);
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet())
            info.put((String) entry.getKey(), entry.getValue());
        return new StringRepresentation(JsonHelper.renderAsJson(info), MediaType.APPLICATION_JSON);
    }

    @Delete
    public Representation undeploy(Representation request) {
        IPath userPath = getUserPath();
        // project file -> project directory
        userPath = userPath.removeLastSegments(1);
        File deployDirectory = BuildDirectoryUtils.getDeployDirectory(userPath);

        ResourceUtils.ensure(BuildDirectoryUtils.isDeployDirectory(deployDirectory), "No deployed application found",
                Status.CLIENT_ERROR_NOT_FOUND);

        RepositoryService.DEFAULT.unregisterRepository(MDDUtil.fromJavaToEMF(deployDirectory.toURI()));
        BuildDirectoryUtils.clearDirectory(deployDirectory);

        return new EmptyRepresentation();
    }

    protected List<IProblem> filterRelevantResults(String contextFileName, List<IProblem> results) {
        List<IProblem> relevantResults = new ArrayList<IProblem>();
        for (IProblem problem : results)
            if (problem.getSeverity() != Severity.INFO) {
                Object fileName = problem.getAttribute(IProblem.FILE_NAME);
                String fileNameString = fileName instanceof IFileStore ? ((IFileStore) fileName).getName() : "" + fileName;

                if (fileName == null || fileNameString.equals(contextFileName))
                    // only include relevant problems
                    relevantResults.add(problem);
            }
        return relevantResults;
    }

    private IFileStore getSourcePath(IPath userPath) {
        IFileStore sourcePath = BuildDirectoryUtils.getSourcePath(userPath);
        ResourceUtils.ensure(sourcePath != null, "No valid source project found for " + userPath, Status.CLIENT_ERROR_NOT_FOUND);
        ResourceUtils.ensure(sourcePath.fetchInfo().exists(), sourcePath.toString(), Status.CLIENT_ERROR_NOT_FOUND);
        ResourceUtils.ensure(sourcePath.fetchInfo().isDirectory(), sourcePath.toString(), Status.CLIENT_ERROR_NOT_FOUND);
        return sourcePath;
    }

    private IPath getUserPath() {
        String pathParam = getQueryValue("path");
        ResourceUtils.ensure(pathParam != null, null, Status.CLIENT_ERROR_BAD_REQUEST);
        IPath userPath = new Path(pathParam.replaceFirst("/file/", "/"));
        return userPath;
    }
}
