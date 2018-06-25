package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.restlet.Request;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import com.abstratt.mdd.core.IProblem;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.UnclassifiedProblem;
import com.abstratt.mdd.core.IProblem.Severity;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.frontend.core.ICompilationDirector;
import com.abstratt.mdd.frontend.core.LocationContext;
import com.abstratt.mdd.frontend.internal.core.CompilationDirector;
import com.abstratt.pluginutils.ISharedContextRunnable;
import com.abstratt.pluginutils.LogUtils;

public class ResourceUtils {

    public static abstract class ResourceRunnable implements ISharedContextRunnable<IRepository, Representation> {
        @Override
        public abstract Representation runInContext(IRepository context);
    }

    public static Representation buildErrorResponse(Throwable e) {
        LogUtils.logWarning(WebFrontEnd.ID, "Failure serving resource", e);
        return new StringRepresentation("<results status=\"failure\" message=\"" + e.getMessage() + "\">\n");
    }

    public static Representation buildJSONErrorResponse(Exception e) {
        return new StringRepresentation("{ \"message\": \"" + e.getMessage() + "\" }");
    }

    public static Representation buildJSONErrorResponse(ResourceException e) {
        return new StringRepresentation("{ \"message\": \"" + e.getStatus().getDescription() + "\" }");
    }

    
    /**
     * Encodes problems as a JSON representation (who am I kidding, this is the
     * Orion format).
     */
    public static String buildJSONResponse(List<IProblem> results) {
        List<String> problemMarkup = new ArrayList<String>();
        for (IProblem current : results) {
            Object localFileName = current.getAttribute(IProblem.FILE_NAME);
            Integer lineNumber = (Integer) current.getAttribute(IProblem.LINE_NUMBER);
            String fileName = localFileName == null ? "" : localFileName instanceof IFileStore ? ((IFileStore) localFileName).getName()
                    : localFileName.toString();
            String lineNumberString = lineNumber == null ? "1" : lineNumber.toString();
            String message = StringUtils.trimToEmpty(current.getMessage()).replace("\"", "\\\"");
			problemMarkup.add("{ \"reason\": \"" + message + "\", \"end\": 1, \"character\": 1, \"line\": " + lineNumberString
                    + ", \"severity\": \"" + current.getSeverity().name().toLowerCase() + "\", \"file\": \"" + fileName + "\"}");
        }
        return "{\"problems\": [" + StringUtils.join(problemMarkup, ',') + "]}";
    }

    public static StringBuffer buildResponse(List<String[]> ignored, List<IProblem> results, double elapsedTime) {
        StringBuffer problemsMarkup = new StringBuffer("\t<problems>\n");
        boolean hasErrors = false;
        for (IProblem current : results) {
            hasErrors |= current.getSeverity() == Severity.ERROR;
            String tag = current.getSeverity().toString().toLowerCase();
            Object localFileName = current.getAttribute(IProblem.FILE_NAME);
            Integer lineNumber = (Integer) current.getAttribute(IProblem.LINE_NUMBER);
            String fileName = localFileName == null ? null : localFileName instanceof IFileStore ? ((IFileStore) localFileName).getName()
                    : localFileName.toString();
            String lineNumberString = lineNumber == null ? "" : lineNumber.toString();
            problemsMarkup.append("\t\t<" + tag + " file=\"" + fileName + "\" line=\"" + lineNumberString + "\" message=\"");
            problemsMarkup.append(StringEscapeUtils.escapeXml(current.getMessage().trim()));
            problemsMarkup.append("\"/>\n");
        }
        problemsMarkup.append("\t</problems>\n");
        StringBuffer ignoredMarkup = new StringBuffer("\t<ignored>\n");
        for (String[] current : ignored)
            ignoredMarkup.append("\t\t<location uri=\"" + current[0] + "\" reason=\"" + current[1] + "\"/>\n");
        ignoredMarkup.append("\t</ignored>\n");
        StringBuffer result = new StringBuffer();
        result.append("<results status=\"" + (hasErrors ? "failure" : "success") + "\" duration=\"" + elapsedTime + "\">\n");
        if (!results.isEmpty())
            result.append(problemsMarkup);
        if (!ignored.isEmpty())
            result.append(ignoredMarkup);
        result.append("</results>");
        return result;
    }

    public static List<IProblem> compile(File baseDir, Map<String, String> customRepositoryProperties) throws CoreException {
        IFileSystem localFS = EFS.getLocalFileSystem();
        final IFileStore basePath = localFS.getStore(baseDir.toURI());
        ResourceUtils.overrideRepositoryProperties(basePath, customRepositoryProperties);
        final LocationContext context = new LocationContext(basePath);
        context.addSourcePath(basePath, basePath);
        final List<IProblem> allProblems = new ArrayList<IProblem>();
        RepositoryService.DEFAULT.runInRepository(MDDUtil.fromJavaToEMF(baseDir.toURI()),
                new ISharedContextRunnable<IRepository, Object>() {
                    @Override
                    public Object runInContext(IRepository repository) {
                        int mode = ICompilationDirector.CLEAN | ICompilationDirector.FULL_BUILD | ICompilationDirector.DEBUG;
                        long startCompilation = System.currentTimeMillis();
                        try {
                            IProblem[] compilationProblems = CompilationDirector.getInstance().compile(null, repository, context, mode,
                                    null);
                            allProblems.addAll(Arrays.asList(compilationProblems));
                        } catch (CoreException e) {
                            throw new RuntimeException(e);
                        }
                        long endCompilation = System.currentTimeMillis();
                        for (IProblem compilationProblem : allProblems)
                            if (compilationProblem.getSeverity() == IProblem.Severity.ERROR)
                                return null;
                        allProblems.add(ResourceUtils.generateInfoMessage(startCompilation, endCompilation, "Model compiled successfully"));
                        return null;
                    }
                });
        return allProblems;
    }

    public static File createTempDir(String prefix) throws IOException {
        File baseDir;
        baseDir = File.createTempFile(prefix, null);
        baseDir.delete();
        baseDir.mkdirs();
        return baseDir;
    }

    public static Representation createZip(Map<String, byte[]> files, String fileName) throws IOException {
        File workingFile = File.createTempFile("cloudfier-work", null);
        boolean success = false;
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(workingFile));
            for (Entry<String, byte[]> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                out.putNextEntry(zipEntry);
                out.write(entry.getValue());
                out.closeEntry();
            }
            success = true;
        } finally {
            IOUtils.closeQuietly(out);
            if (!success)
                FileUtils.deleteQuietly(workingFile);
        }
        FileRepresentation representation = new FileRepresentation(workingFile, MediaType.APPLICATION_OCTET_STREAM);
        representation.setAutoDeleting(true);
        Series<Parameter> parameters = new Series<Parameter>(Parameter.class, Arrays.asList(new Parameter("filename", fileName + ".zip")));
        representation.setDisposition(new Disposition(Disposition.TYPE_INLINE, parameters));
        return representation;
    }
    
	public static Representation buildMultiFileResult(Request request, final String zipFileName,
			SortedMap<String, byte[]> result) throws IOException {
		String expectedContentType = ((HttpRequest) request).getHttpCall().getRequestHeaders().getValues(
				HeaderConstants.HEADER_ACCEPT);
		boolean zipFormat = (MediaType.ALL.getName().equals(expectedContentType) && result.size() > 1) 
		        || MediaType.APPLICATION_OCTET_STREAM.getName()
				    .equals(expectedContentType)
				|| MediaType.APPLICATION_ZIP.getName().equals(
						expectedContentType)
				|| expectedContentType == null
				|| result.size() > 1;

		if (zipFormat) {
            return ResourceUtils.createZip(result, zipFileName);
		}
		StringBuffer resultString = new StringBuffer();
		for (byte[] each : result.values()) {
			resultString.append("\n");
			resultString.append(new String(each));
		}
		return new StringRepresentation(resultString.toString());
	}
    

    public static void ensure(boolean condition, String message, org.restlet.data.Status status) {
        if (!condition) {
            if (status == null)
                status = org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST;
            ResourceException resourceException = new ResourceException(status, message);
            LogUtils.logWarning(WebFrontEnd.ID, message, resourceException);
            throw resourceException;
        }
    }

    public static void fail(Throwable exception, org.restlet.data.Status status) {
        ResourceException resourceException = new ResourceException(
                status == null ? org.restlet.data.Status.SERVER_ERROR_INTERNAL : status, exception);
        LogUtils.logError(WebFrontEnd.ID, exception.getMessage(), exception);
        throw resourceException;
    }
    
    public static void fail(String message, org.restlet.data.Status status) {
        ResourceException resourceException = new ResourceException(
                status == null ? org.restlet.data.Status.SERVER_ERROR_INTERNAL : status);
        LogUtils.logError(WebFrontEnd.ID, message, null);
        throw resourceException;
    }

    public static List<String[]> fetchUnits(File baseDir, String[] allLocations) throws IOException, HttpException {
        HttpClient client = new HttpClient();
        Map<String, String> localToRemoteLocations = new HashMap<String, String>();
        List<String[]> ignored = new ArrayList<String[]>();
        for (String location : allLocations) {
            if (location.endsWith("/") || location.indexOf("/") < 0) {
                ignored.add(new String[] { location, "URI must end with a file name" });
                continue;
            }
            String fileName = location.substring(location.lastIndexOf('/') + 1);
            if (fileName.indexOf('.') < 0) {
                ignored.add(new String[] { location, "URI must end with a file name that has an extension" });
                continue;
            }
            if (localToRemoteLocations.containsKey(fileName)) {
                ignored.add(new String[] { location, "File name already in use" });
                continue;
            }
            GetMethod get = new GetMethod(location);
            int status = client.executeMethod(get);
            if (status != 200) {
                ignored.add(new String[] { location, get.getStatusText() });
                continue;
            }
            localToRemoteLocations.put(fileName, location);
            File localFile = new File(baseDir, fileName);
            InputStream unitContents = null;
            FileOutputStream output = null;
            try {
                unitContents = get.getResponseBodyAsStream();
                output = FileUtils.openOutputStream(localFile);
                IOUtils.copy(unitContents, output);
            } finally {
                IOUtils.closeQuietly(unitContents);
                IOUtils.closeQuietly(output);
            }
        }
        return ignored;
    }

    public static URI getRepositoryURI(String workspace) {
        File deployDirectory = BuildDirectoryUtils.getDeployDirectory(workspace);
        boolean projectExists = deployDirectory.isDirectory() && deployDirectory.listFiles() != null
                && deployDirectory.listFiles().length > 0;
        if (!projectExists) {
            LogUtils.logWarning(ResourceUtils.class.getPackage().getName(), "Could not find project at " + deployDirectory, null);
            ResourceUtils.ensure(false, "Project not found: " + workspace, org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
        }
        return MDDUtil.fromJavaToEMF(deployDirectory.toURI());
    }

    public static String getWorkspaceFromProjectPath(Request request) {
        return ResourceUtils.getWorkspaceFromProjectPath(request, true);
    }

    public static String getWorkspaceFromProjectPath(Request request, boolean required) {
        String workspace = (String) request.getAttributes().get("workspace");
        if (workspace == null) {
            Parameter pathParam = request.getResourceRef().getQueryAsForm().getFirst("path");
            if (pathParam == null || StringUtils.isBlank(pathParam.getValue())) {
                ResourceUtils.ensure(!required, "A path parameter is required", org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST);
                return null;
            }
            IPath path = new Path(pathParam.getValue().replaceFirst("/file/", "/").replace("/mdd.properties", "/"));
            workspace = BuildDirectoryUtils.getWorkspaceNameFromPath(path);
        }
        return workspace;
    }

    /**
     * Prepares the temp project dir based on a given project file.
     * 
     * @param contextFile
     * @return the temp directory provisioned, it is up to the caller to delete
     *         it
     */
    public static File prepareTempProjectDir(IPath projectPath, IFileStore contextFile) throws CoreException, IOException {
        Assert.isTrue(contextFile.fetchInfo().exists());
        Assert.isTrue(!contextFile.fetchInfo().isDirectory());
        File projectDir = contextFile.getParent().toLocalFile(EFS.NONE, null);
        String projectName = projectPath == null ? projectDir.getName() : BuildDirectoryUtils.getWorkspaceNameFromPath(projectPath);
        ResourceUtils.ensure(projectDir.isDirectory(), null, org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
        File tmpBaseDir = ResourceUtils.createTempDir("kirra");
        File tmpProjectDir = new File(tmpBaseDir, "tmp-" + projectName);
        FileUtils.copyDirectory(projectDir, tmpProjectDir);
        return tmpProjectDir;
    }

    public static void releaseTempProjectDir(File tmpProjectDir) {
        if (tmpProjectDir != null)
            FileUtils.deleteQuietly(tmpProjectDir.getParentFile());
    }

    /**
     * Runs the given runnable in the repository corresponding to the given
     * request.
     */
    public static <R> R runInRepository(Request request, ISharedContextRunnable<IRepository, R> runnable) {
        String workspace = ResourceUtils.getWorkspaceFromProjectPath(request);
        try {
            return RepositoryService.DEFAULT.runInRepository(ResourceUtils.getRepositoryURI(workspace), runnable);
        } catch (CoreException e) {
            ResourceUtils.fail(e, null);
            // never runs
            return null;
        }
    }

    public static Representation serveInResource(Request request, ResourceRunnable runnable) {
        String workspace = ResourceUtils.getWorkspaceFromProjectPath(request);
        try {
            return RepositoryService.DEFAULT.runInRepository(ResourceUtils.getRepositoryURI(workspace), runnable);
        } catch (CoreException e) {
            ResourceUtils.fail(e, null);
            // never runs
            return null;
        }
    }

    private static IProblem generateInfoMessage(long start, long end, String phase) {
        return new UnclassifiedProblem(Severity.INFO, phase + " in " + (end - start) / 1000d + "s");
    }

    /**
     * Override is a misnomer, 'enhance' would be more appropriate, as existing
     * properties are preserved.
     */
    private static void overrideRepositoryProperties(final IFileStore basePath, Map<String, String> customRepositoryProperties)
            throws CoreException {
        File basePathAsFile = basePath.toLocalFile(EFS.NONE, null);
        URI repositoryURI = MDDUtil.fromJavaToEMF(basePath.toURI());
        Properties properties = MDDUtil.loadRepositoryProperties(repositoryURI);
        for (Entry<String, String> entry : customRepositoryProperties.entrySet()) {
            if (!properties.containsKey(entry.getKey()))
                properties.setProperty(entry.getKey(), entry.getValue());
        }
        StringWriter output = new StringWriter();
        try {
            properties.store(output, "Generated by " + ResourceUtils.class.getPackage().getName());
            // properties are always ISO-8859-1
            FileUtils.writeStringToFile(new File(basePathAsFile, IRepository.MDD_PROPERTIES), output.toString(), "ISO-8859-1");
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, WebFrontEnd.ID, "Error updating properties", e));
        }
    }
}
