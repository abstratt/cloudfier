package com.abstratt.mdd.target.engine.gstring;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.target.MappingException;
import com.abstratt.pluginutils.LogUtils;

public class GroovyLanguageMapper<T extends NamedElement> implements ITopLevelMapper<T> {
    static interface ITemplateOperation {
        String perform(GroovyTemplate template);
    }

    private Map<String, String> properties;
    private URI templateUri;

    private ArrayList<URI> baseURIs;

    public GroovyLanguageMapper(Map<String, String> properties, URI baseURI) {
        this.properties = properties;
        try {
            String templateLocation = properties.get("template");
            if (templateLocation == null)
                throw new MappingException("Missing template declaration");
            URI templateLocationURI = URI.create(templateLocation.trim());
            this.templateUri = templateLocationURI.isAbsolute() ? templateLocationURI : URIUtil.append(baseURI, templateLocation.trim());
            String imported = properties.get(IRepository.IMPORTED_PROJECTS);
            this.baseURIs = new ArrayList<URI>();
            baseURIs.add(makeDirURI(baseURI));
            if (imported != null) {
                String[] importedURIs = imported.split(",");
                for (String uri : importedURIs)
                    baseURIs.add(makeDirURI(URI.create(uri)));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String applyChildMapper(String mapperName, Element element) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MapperInfo describeChildMapper(String mapperName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getChildMappers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getFormalArgs(String mapperName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String map(final T toMap) {
        return invokeTemplate(new ITemplateOperation() {
            @Override
            public String perform(GroovyTemplate template) {
                return template.generate(toMap).toString();
            }
        });
    }

    @Override
    public String mapAll(final List<T> toMap) {
        return invokeTemplate(new ITemplateOperation() {
            @Override
            public String perform(GroovyTemplate template) {
                return template.generateAll(toMap).toString();
            }
        });
    }

    @Override
    public String mapFileName(final T toMap) {
        return invokeTemplate(new ITemplateOperation() {
            @Override
            public String perform(GroovyTemplate template) {
                return template.generateFileName(toMap).toString();
            }
        });
    }

    private String invokeTemplate(ITemplateOperation operation) {
        GroovyClassLoader templateLoader = new GroovyClassLoader(getClass().getClassLoader());
        InputStream contents = null;
        try {
            String[] helperUris = StringUtils.split(StringUtils.trimToEmpty(properties.get("helperUris")), ',');
            for (String helperUrl : helperUris) {
                templateLoader.parseClass(new GroovyCodeSource(URI.create(helperUrl).toURL()));
            }
            contents = templateUri.toURL().openStream();
            List<String> allLines = IOUtils.readLines(contents, "UTF-8");
            String prefix = "";
            prefix += "import static com.abstratt.mdd.core.util.MDDUtil.*;";
            prefix += "import org.eclipse.uml2.uml.*;";
            prefix += StringUtils.trimToEmpty(properties.get("imports"));
            prefix += "class UserTemplate extends com.abstratt.mdd.target.engine.gstring.GroovyTemplate {";
            allLines.set(0, prefix + allLines.get(0));
            allLines.set(allLines.size() - 1, allLines.get(allLines.size() - 1) + "}");
            String fileContents = StringUtils.join(allLines, "\n");
            templateLoader.parseClass(fileContents, "UserTemplate");
            Class<GroovyTemplate> parsed = templateLoader.parseClass(fileContents, "UserTemplate");
            return operation.perform(parsed.newInstance());
        } catch (CompilationFailedException e) {
            LogUtils.logError(getClass().getPackage().getName(), null, e);
            throw new MappingException("Error compiling template: " + e.getMessage());
        } catch (GroovyRuntimeException r) {
            LogUtils.logError(getClass().getPackage().getName(), null, r);
            IOUtils.closeQuietly(contents);
            templateLoader.clearCache();

            StackTraceElement[] stack = r.getStackTrace();
            for (StackTraceElement frame : stack)
                if ("UserTemplate".equals(frame.getClassName()))
                    throw new MappingException(r.getMessage() + " at line " + frame.getLineNumber());
            throw r;
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(contents);
            templateLoader.clearCache();
        }
        return null;
    }

    private URI makeDirURI(URI baseURI) throws URISyntaxException {
        if (!baseURI.getPath().endsWith("/"))
            return new URI(baseURI.getScheme(), baseURI.getUserInfo(), baseURI.getHost(), baseURI.getPort(), baseURI.getPath() + "/",
                    baseURI.getQuery(), baseURI.getFragment());
        return baseURI;
    }

}
