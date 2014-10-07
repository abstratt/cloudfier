package com.abstratt.mdd.target.engine.st;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;

public class STUtils {
	public static byte[] readTemplate(URI source) throws IOException {
		InputStream stream = null;
		try {
			URLConnection connection = source.toURL().openConnection();
			connection.setUseCaches(false);
			stream = connection.getInputStream();
			return IOUtils.toByteArray(stream);
		} finally {
			IOUtils.closeQuietly(stream);
		}
	}
}
