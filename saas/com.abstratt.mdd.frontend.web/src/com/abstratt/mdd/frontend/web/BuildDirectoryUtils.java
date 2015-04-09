package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.pluginutils.LogUtils;

public class BuildDirectoryUtils {
    public static void clearDirectory(File buildDir) {
        if (BuildDirectoryUtils.isDeployDirectory(buildDir))
            FileUtils.deleteQuietly(buildDir);
    }

    public static File getBaseDeployDirectory() {
        IFileStore instancePath = BuildDirectoryUtils.getInstancePath();
        String deployBase = System.getProperty("kirra.deploy.base");
        File parentDir = deployBase == null ? new File(instancePath.toString()).getParentFile() : new File(deployBase);
        return new File(new File(parentDir, ".kirra"), "deploy");
    }

    public static File getDeployDirectory(IPath projectPath) {
        return BuildDirectoryUtils.getDeployDirectory(BuildDirectoryUtils.getWorkspaceNameFromPath(projectPath));
    }

    /**
     * For a unique project source path, determines a corresponding unique build
     * directory.
     */
    public static File getDeployDirectory(String workspace) {
        return new File(BuildDirectoryUtils.getBaseDeployDirectory(), File.separatorChar + workspace);
    }

    /**
     * Maps the path as used in the API to the local file system path.
     * 
     * Can read Orion-based file system configuration to map a user/project to a
     * filesystem location.
     * 
     * @param originalPath
     * @return
     */
    public static IFileStore getSourcePath(IPath originalPath) {
        IFileStore rootInstanceDir = BuildDirectoryUtils.getInstancePath();
        if (originalPath.segmentCount() < 2)
            return null;
        IPath workspaceRelativePath = originalPath.removeFirstSegments(1).makeRelative(); 
        String userName = originalPath.segments()[0];
        String userBucket = userName.substring(0, 2);
        URI result = rootInstanceDir.getChild(userBucket).getChild(userName).getChild("OrionContent").getChild(workspaceRelativePath.toString()).toURI();
        return EFS.getLocalFileSystem().getStore(result);
//        IFileStore serverSettings = rootInstanceDir.getChild(".metadata/.plugins/org.eclipse.orion.server.core/.settings/");
//        Properties workspaceProperties = BuildDirectoryUtils.readProperties(serverSettings.getChild("Workspaces.prefs"));
//        Properties projectProperties = BuildDirectoryUtils.readProperties(serverSettings.getChild("Projects.prefs"));
//        if (workspaceProperties != null && projectProperties != null)
//            try {
//                for (JsonNode jsonNode : ((ArrayNode) JsonHelper.parse(new StringReader((String) workspaceProperties.get(userName
//                        + "/Projects"))))) {
//                    ObjectNode entry = (ObjectNode) jsonNode;
//                    String projectKey = entry.get("Id").getTextValue();
//                    if (projectName.equals(projectProperties.getProperty(projectKey + "/Name"))) {
//                        String workspaceLocation = projectProperties.getProperty(projectKey + "/ContentLocation");
//                        URI result = URI.create(workspaceLocation + projectPath);
//                        if (!result.isAbsolute()) {
//                            // it is unclear why, but Orion may use absolute or
//                            // workspace relative project URLs
//                            result = URIUtil.append(rootInstanceDir.toURI(), result.getPath());
//                        }
//                        StringBuffer buffer = new StringBuffer();
//                        buffer.append("\n\tOriginal path: " + originalPath);
//                        buffer.append("\n\tWorkspace location: " + workspaceLocation);
//                        buffer.append("\n\tProject path: " + projectPath);
//                        buffer.append("\n\tSource URI: " + result);
//                        LogUtils.logInfo(WebFrontEnd.ID, buffer.toString(), null);
//                        return EFS.getLocalFileSystem().getStore(result);
//                    }
//                }
//            } catch (IOException e) {
//                //
//            }
//        return null;
    }

    public static String getWorkspaceNameFromPath(IPath projectPath) {
        projectPath = projectPath.removeTrailingSeparator().makeRelative();
        return projectPath.toString().replaceAll("[/%?#]", "-");
    }

    public static boolean isDeployDirectory(File toCheck) {
        if (!toCheck.isDirectory())
            return false;
        URI toCheckAsUri = toCheck.toURI();
        URI relative = BuildDirectoryUtils.getBaseDeployDirectory().toURI().relativize(toCheckAsUri);
        if (relative.isAbsolute() || relative.getPath().isEmpty())
            return false;
        return new File(toCheck, IRepository.MDD_PROPERTIES).isFile();
    }

    public static File[] listDeployedFiles(IPath projectPath) {
        File buildDir = BuildDirectoryUtils.getDeployDirectory(projectPath);
        if (!buildDir.isDirectory())
            return new File[0];
        File[] buildFiles = buildDir.listFiles();
        if (buildFiles == null)
            return new File[0];
        return buildFiles;
    }

    public static void replaceDirectory(File tmpBaseDir, File buildDir) throws IOException {
        FileUtils.deleteQuietly(buildDir);
        FileUtils.moveDirectory(tmpBaseDir, buildDir);
    }

    protected static IFileStore getInstancePath() {
        return EFS.getLocalFileSystem().getStore(URI.create(Platform.getInstanceLocation().getURL().toString()));
    }

    protected static Properties readProperties(IFileStore propertiesLocation) {
        try {
            Properties userPrefs = new Properties();
            userPrefs.load(new StringReader(FileUtils.readFileToString(propertiesLocation.toLocalFile(EFS.NONE, null))));
            return userPrefs;
        } catch (IOException e) {
            return null;
        } catch (CoreException e) {
            return null;
        }
    }
}
