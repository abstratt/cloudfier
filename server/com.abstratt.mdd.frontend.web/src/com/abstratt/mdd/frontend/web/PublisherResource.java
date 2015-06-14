package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.UMLPackage;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.abstratt.mdd.core.IProblem;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.MDDCore;
import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.core.util.TemplateUtils;
import com.abstratt.pluginutils.LogUtils;

/**
 * Publisher resource.
 * <p>
 * URI: http://localhost:8090/services/publisher/{workspace}/
 */
public class PublisherResource extends AbstractWorkspaceResource {
    /**
     * Much like a POST on the {@link ValidatorResource}, with the exception
     * that a workspace is created (or an existing one updated) and will now
     * contain the input and output files of the compilation. This state will be
     * the basis for any runs of the prototype.
     */
    @Post
    public Representation buildWorkspace(Representation entity) {
        File tmpProjectDir = null;
        try {
            tmpProjectDir = new File(ResourceUtils.createTempDir("kirra"), getWorkspace());
            tmpProjectDir.mkdirs();
            MediaType mediaType = getRequest().getEntity().getMediaType();
            List<String[]> ignored = new ArrayList<String[]>();
            if (MediaType.MULTIPART_FORM_DATA.equals(mediaType, true)) {
                RestletFileUpload upload = new RestletFileUpload(new DiskFileItemFactory());
                List<FileItem> items = upload.parseRequest(getRequest());
                for (FileItem fileItem : items)
                    if (!fileItem.isFormField()) {
                        String fileClientPath = fileItem.getName();
                        fileItem.write(new File(tmpProjectDir, fileClientPath));
                    }
            } else if (MediaType.APPLICATION_WWW_FORM.equals(mediaType, true)) {
                String locations = getRequest().getEntityAsText();
                String[] allLocations = locations.split("\n");
                ignored = ResourceUtils.fetchUnits(tmpProjectDir, allLocations);
            } else {
                // POST request with no entity.
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                return new EmptyRepresentation();
            }
            long start = System.currentTimeMillis();
            List<IProblem> results = ResourceUtils.compile(tmpProjectDir, getMDDOptions());
            for (IProblem result : results)
                AbstractWorkspaceResource.log.info(result);
            String secret = getSecret();
            if (secret != null) {
                File secretFile = new File(tmpProjectDir, AbstractWorkspaceResource.SECRET_FILE);
                FileUtils.writeStringToFile(secretFile, secret);
            }
            File workspaceDir = getWorkspaceDir(false);
            FileUtils.deleteQuietly(workspaceDir);
            FileUtils.moveDirectory(tmpProjectDir, workspaceDir);
            long end = System.currentTimeMillis();
            double elapsedTime = (end - start) / 1000d;
            StringBuffer result = ResourceUtils.buildResponse(ignored, results, elapsedTime);
            return new StringRepresentation(result.toString(), MediaType.TEXT_XML);
        } catch (HttpException e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            LogUtils.logWarning(getClass().getPackage().getName(), "HTTP error", e);
            return ResourceUtils.buildErrorResponse(e);
        } catch (IOException e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            LogUtils.logWarning(getClass().getPackage().getName(), "IO error", e);
            return ResourceUtils.buildErrorResponse(e);
        } catch (CoreException e) {
            LogUtils.logWarning(getClass().getPackage().getName(), "Core error", e);
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        } catch (FileUploadException e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        } catch (Exception e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return ResourceUtils.buildErrorResponse(e);
        } finally {
            ResourceUtils.releaseTempProjectDir(tmpProjectDir);
        }
    }

    /**
     * Deletes the given workspace. Returns a 404 if the workspace does not
     * exist.
     * 
     * @throws IOException
     */
    @Delete
    public Representation deleteWorkspace() throws IOException {
        checkSecret();
        FileUtils.deleteQuietly(getWorkspaceDir(false));
        return new EmptyRepresentation();
    }

    /**
     * Returns information on the workspace, including number of packages and
     * timestamp. Returns 404 if the workspace does not exist. Use ?classes=true
     * to include information about classes in the model.
     * 
     * @throws IOException
     */
    @Get("xml")
    public Representation getWorkspaceInfo() throws IOException {
        File[] workspaceFiles = listWorkspaceFiles();
        ResourceUtils.ensure(workspaceFiles.length > 0, null, Status.CLIENT_ERROR_NOT_FOUND);
        checkSecret();

        boolean includeClasses = getQuery().removeAll("classes");

        int packageCount = 0;
        long timestamp = Long.MAX_VALUE;
        List<String> fileElements = new ArrayList<String>();
        Reference baseReference = ReferenceUtils.getExternal(getReference());
        String secret = getSecret();
        if (!secret.isEmpty()) {
            baseReference.setQuery(AbstractWorkspaceResource.SECRET_PARAMETER + "=" + getSecret());
        }
        ArrayList<File> umlFiles = new ArrayList<>(); 
        for (File file : workspaceFiles) {
            if (file.getName().equals(AbstractWorkspaceResource.SECRET_FILE))
                continue;
            String additionalAttributes = "";
            String element = "source";
            String fileURI = baseReference.clone().addSegment(file.getName()).toString();
            if (file.getName().equals(IRepository.MDD_PROPERTIES))
                element = "properties";
            else if (FilenameUtils.isExtension(file.getName(), "uml")) {
                packageCount++;
                umlFiles.add(file);
                timestamp = Math.min(timestamp, file.lastModified());
                element = "model";
            }

            fileElements.add("<" + element + " name='" + file.getName() + "' uri='" + fileURI + "'" + additionalAttributes + "/>");
        }
        Collections.sort(fileElements);
        StringBuffer workspaceMarkup = new StringBuffer("<workspace packageCount='").append(packageCount).append("' timestamp='")
                .append(timestamp).append("'>\n");
        for (String element : fileElements)
            workspaceMarkup.append("\t").append(element).append('\n');
        Properties repoProperties = MDDUtil.loadRepositoryProperties(MDDUtil.fromJavaToEMF(getWorkspaceDir(false).toURI()));
        for (String platformId : TargetCore.getPlatformIds(repoProperties)) {
            String generatorURI = baseReference.clone().addSegment(WebFrontEnd.PLATFORM_SEGMENT).addSegment(platformId).toString()
                    .replace(WebFrontEnd.PUBLISHER_SEGMENT, WebFrontEnd.GENERATOR_SEGMENT);
            workspaceMarkup.append("\t<generator platform=\"" + platformId + "\" uri=\"" + generatorURI + "\"/>\n");
            ITargetPlatform platform = TargetCore.getPlatform(repoProperties, platformId);
            for (String artifactType : platform.getArtifactTypes()) {
                String artifactTypeGeneratorURI = generatorURI + "/" + WebFrontEnd.MAPPER_SEGMENT + "/" + artifactType;
                workspaceMarkup.append("\t<generator mapper=\"" + artifactType + "\" platform=\"" + platformId + "\" uri=\"" + artifactTypeGeneratorURI + "\"/>\n");
            }
        }
        String archiveURI = baseReference.getParentRef().addSegment(getWorkspaceDir(false).getName() + ".zip").toString();
        workspaceMarkup.append("<archive uri=\"" + archiveURI + "\"/>");
        
        String baseDiagramURI = baseReference.clone().toString().replace(WebFrontEnd.PUBLISHER_SEGMENT, WebFrontEnd.DIAGRAM_SEGMENT);
        umlFiles.forEach(f -> workspaceMarkup.append("<diagram uri=\"" + baseDiagramURI + "package/" + f.getName() + "\"/>"));
        if (includeClasses)
            try {
                workspaceMarkup.append(getClasses(getWorkspaceDir(false)));
            } catch (CoreException e) {
                workspaceMarkup.append("<!-- " + e.getMessage() + "-->\n");
            }
        workspaceMarkup.append("</workspace>");
        return new StringRepresentation(workspaceMarkup, MediaType.TEXT_XML);
    }

    protected Map<String, String> getMDDOptions() {
        Map<String, String> allParameters = getQuery().getValuesMap();
        Set<Entry<String, String>> allEntries = allParameters.entrySet();
        for (Iterator<Entry<String, String>> iterator = allEntries.iterator(); iterator.hasNext();)
            if (!iterator.next().getKey().startsWith("mdd."))
                iterator.remove();
        return allParameters;
    }

    private String getClasses(File workspaceDir) throws CoreException {
        StringBuffer response = new StringBuffer();
        IRepository repository = MDDCore.createRepository(URI.createURI(workspaceDir.toURI().toString()));
        try {
            final List<Classifier> userClasses = repository.findAll(new EObjectCondition() {
                @Override
                public boolean isSatisfied(EObject eObject) {
                    if (!UMLPackage.Literals.CLASSIFIER.isInstance(eObject))
                        return false;
                    if (UMLPackage.Literals.ACTIVITY.isInstance(eObject))
                        return false;
                    if (UMLPackage.Literals.ASSOCIATION.isInstance(eObject))
                        return false;
                    Classifier asClassifier = (Classifier) eObject;
                    return !StringUtils.isBlank(asClassifier.getQualifiedName()) && !TemplateUtils.isTemplateInstance(asClassifier);
                }
            }, true);
            for (Classifier each : userClasses) {
                response.append("\t<class name='" + each.getQualifiedName() + "' ");
                EAnnotation unitAnnotation = each.getEAnnotation(MDDUtil.UNIT);
                if (unitAnnotation != null)
                    for (Entry<String, String> entry : unitAnnotation.getDetails().entrySet())
                        response.append("unit." + entry.getKey() + "=\"" + entry.getValue() + "\" ");
                response.append("/>\n");
            }
        } finally {
            repository.dispose();
        }
        return response.toString();
    }
}
