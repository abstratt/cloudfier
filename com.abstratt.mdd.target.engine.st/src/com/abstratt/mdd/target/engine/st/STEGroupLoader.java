package com.abstratt.mdd.target.engine.st;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplateGroupInterface;
import org.antlr.stringtemplate.StringTemplateGroupLoader;
import org.eclipse.core.runtime.URIUtil;

import com.abstratt.pluginutils.LogUtils;

public class STEGroupLoader implements StringTemplateGroupLoader {
	private final class STEGroup extends StringTemplateGroup {
        private STEGroup(Reader r, StringTemplateErrorListener errors) {
            super(r, errors);
            // this circumvents the caching of groups in StringTemplate
            nameToGroupMap.clear();
        }

        /* This implementation uses a short-lived, thread safe cache of groups, and circumvents the one in STG. */
        @Override
        public void setSuperGroup(String superGroupName) {
            StringTemplateGroup superGroup = groupCache.get().get(superGroupName);
            if (superGroup == null) {
                superGroup = loadGroup(superGroupName);
                if (superGroup != null)
                    groupCache.get().put(superGroupName, superGroup);
            }
            setSuperGroup(superGroup);
        }
    }

    private static ThreadLocal<List<URI>> baseURIs = new ThreadLocal<List<URI>>();
	private static ThreadLocal<Map<String, StringTemplateGroup>> groupCache = new ThreadLocal<Map<String, StringTemplateGroup>>();
	
	private static STEGroupLoader instance = new STEGroupLoader();
	
	public static STEGroupLoader getInstance() {
		return instance ;
	}
	
	private STEGroupLoader() {
	    StringTemplateGroup.registerGroupLoader(this);
	}
	
	public static void registerBaseURIs(URI... baseURIs) {
		STEGroupLoader.baseURIs.set(Arrays.asList(baseURIs));
		STEGroupLoader.groupCache.set(new HashMap<String, StringTemplateGroup>());
	}
	
	public static void clearBaseURI() {
		STEGroupLoader.baseURIs.set(null);
		STEGroupLoader.groupCache.set(null);
	}

	@Override
	public StringTemplateGroup loadGroup(String groupName) {
		try {
			if (!groupName.endsWith(".stg"))
				groupName = groupName + ".stg";
			List<URI> toLookup = baseURIs.get();
			byte[] templateContents = null;
			for (URI uri : toLookup) {
			    try {
			        templateContents = STUtils.readTemplate(URIUtil.append(uri, groupName));
			    } catch (FileNotFoundException e) {
			        // keep looking
			    }
            }
			// not found...
			if (templateContents == null) {
			    STException exception = new STException("Unknown group: " + groupName, null);
			    LogUtils.logInfo(STEGroupLoader.class.getPackage().getName(), "Could not find group " + groupName + " in path: " + toLookup, exception);
	            // this deviates from STGL's contract but if not done unknown groups are not reported
                throw exception;
			}	            
			StringTemplateGroup loaded = new STEGroup(new InputStreamReader(new ByteArrayInputStream(templateContents)), new StringTemplateErrorListener() {
				@Override
				public void warning(String msg) {
					// nothing to do
				}
				@Override
				public void error(String msg, Throwable e) {
					throw new STException(msg, e);
				}
			});
			return loaded;
		} catch (MalformedURLException e) {
			throw new STException(null, e);
        } catch (FileNotFoundException e) {
            // this deviates from STGL's contract but if not done unknown groups are not reported
            throw new STException("Unknown group: " + groupName, null);
		} catch (IOException e) {
			throw new STException(null, e);
		}
	}

	@Override
	public StringTemplateGroup loadGroup(String groupName,
			StringTemplateGroup superGroup) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StringTemplateGroup loadGroup(String groupName, Class templateLexer,
			StringTemplateGroup superGroup) {
		return loadGroup(groupName);
	}

	@Override
	public StringTemplateGroupInterface loadInterface(String interfaceName) {
		throw new UnsupportedOperationException();
	}

}
