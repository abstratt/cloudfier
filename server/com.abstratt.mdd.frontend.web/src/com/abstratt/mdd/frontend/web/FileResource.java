package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

public class FileResource extends AbstractWorkspaceResource {
    /**
     * Returns the unit contents. Returns 404 if the repository or unit has not
     * been created yet.
     * 
     * @throws IOException
     */
    @Get
    public Representation getContents() throws IOException {
        File workspaceDir = getWorkspaceDir(true);
        checkSecret();
        String fileName = (String) getRequestAttributes().get("file");
        File file = new File(workspaceDir, fileName);
        if (!file.getParentFile().equals(workspaceDir) || !file.isFile()) {
            setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return new EmptyRepresentation();
        }
        String contents;
        try {
            contents = FileUtils.readFileToString(file);
            setStatus(Status.SUCCESS_OK);
            return new StringRepresentation(contents, MediaType.TEXT_PLAIN);
        } catch (IOException e) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return new EmptyRepresentation();
        }
    }

}
