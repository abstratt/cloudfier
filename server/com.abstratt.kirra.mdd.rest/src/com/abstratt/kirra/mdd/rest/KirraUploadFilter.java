package com.abstratt.kirra.mdd.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IStatus;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.routing.Filter;

import com.abstratt.kirra.rest.common.KirraContext;
import com.abstratt.kirra.rest.common.KirraContext.Upload;
import com.abstratt.pluginutils.LogUtils;

public class KirraUploadFilter extends Filter {
	
	private static ThreadLocal<File> BASE_DIR = new ThreadLocal<>();
	
	public KirraUploadFilter(Restlet wrapped) {
        setNext(wrapped);
    }

	@Override
	protected int beforeHandle(Request request, Response response) {
		MediaType mediaType = request.getEntity().getMediaType();
		if (!MediaType.MULTIPART_FORM_DATA.isCompatible(mediaType)) {
			return super.beforeHandle(request, response);
		}
		try {
			handleAttachment(request);
		} catch (Exception e) {
			LogUtils.log(IStatus.ERROR, Activator.ID, request.toString(), e);
		}
		return super.beforeHandle(request, response);
	}
	@Override
	protected void afterHandle(Request request, Response response) {
		removeAttachments(request);
		super.afterHandle(request, response);
	}

	private void removeAttachments(Request request) {
		if (BASE_DIR.get() != null)
			try {
				FileUtils.deleteDirectory(BASE_DIR.get());
			} catch (IOException e) {
				LogUtils.log(IStatus.ERROR, Activator.ID, request.toString(), e);
			}
	}
	
	private void handleAttachment(Request request) throws Exception {
		File baseDir = Files.createTempDirectory("kirra").toFile();
		BASE_DIR.set(baseDir);
		baseDir.mkdirs();
		List<Upload> uploads = new LinkedList<>();
		RestletFileUpload upload = new RestletFileUpload(new DiskFileItemFactory());
		List<FileItem> items = upload.parseRequest(request);
		for (FileItem fileItem : items)
			if (!fileItem.isFormField()) {
				String fileClientPath = fileItem.getName();
				File localFile = new File(baseDir, fileClientPath);
				fileItem.write(localFile);
				uploads.add(new Upload(fileItem.getFieldName(), fileItem.getContentType(), fileItem.getName(), localFile));
			}
		KirraContext.setUploads(uploads);
	}
}
