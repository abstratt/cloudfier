package com.abstratt.mdd.core.target.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.TemplateableElement;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.util.TemplateUtils;

public class TargetUtils {
    public static <T extends NamedElement> Map<String, CharSequence> map(IRepository repository, final ITopLevelMapper<T> mapper, final EClass supportedEClass, final List<String> toGenerate) {
        final List<T> userClasses = repository.findAll(
                new EObjectCondition() {
                    @Override
                    public boolean isSatisfied(EObject eObject) {
                        if (!supportedEClass.isInstance(eObject))
                            return false;
                        T named = (T) eObject;
                        if (named.getName() == null)
                            return false;
                        if (named instanceof TemplateableElement && TemplateUtils.isTemplateInstance((TemplateableElement) named))
                            return false;
                        if (!toGenerate.isEmpty() && !toGenerate.contains(named.getQualifiedName()))
                            return false;
                        return mapper.canMap(named);
                    }
                }, true);
        
        Map<String, CharSequence> result = new LinkedHashMap<String, CharSequence>();
        for (T each : userClasses) {
            String fileName = mapper.mapFileName(each);
            String mapped = mapper.map(each).toString();
            CharSequence existing = result.get(fileName);
            if (existing == null)
                result.put(fileName, mapped);
            else
                result.put(fileName, existing.toString() + '\n' + mapped);
        }
        return result;
    }
    
    public static String merge(InputStream templateContents, String toEmbed, String placeholder) throws IOException {
        List<String> generated = new ArrayList<String>();
        try {
            generated.addAll(IOUtils.readLines(templateContents));
        } finally {
            IOUtils.closeQuietly(templateContents);
        }
        int insertionIndex = -1;
        String insertionTag = null;
        for (int i = 0; i < generated.size(); i++) {
            if (generated.get(i).trim().equals(placeholder)) {
                insertionIndex = i;
                insertionTag = generated.get(i);
                generated.remove(insertionIndex);
                break;
            }
        }
        if (insertionTag == null)
            throw new IllegalArgumentException();
        Matcher matcher = Pattern.compile("^(\\s*)?.*").matcher(insertionTag);
        matcher.find();
        String linePrefix = matcher.group(1);
        String[] linesToEmbed = StringUtils.split(toEmbed, '\n');
        for (int i = 0; i < linesToEmbed.length; i++)
            generated.add(insertionIndex + i, linePrefix + linesToEmbed[i]);
        return StringUtils.join(generated, '\n');

    }
    
    public static String renderException(Throwable t) {
        StringWriter out = new StringWriter();
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
    
    public static CharSequence merge(InputStream templateContents, Map<String, String> replacements) throws IOException {
        List<String> read;
        try {
            read = IOUtils.readLines(templateContents);
        } finally {
            IOUtils.closeQuietly(templateContents);
        }
        Pattern pattern = Pattern.compile("(\\{([a-zA-Z_]+)\\})");
        for (int i = 0; i < read.size(); i++) {
            String line = read.get(i);
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String placeholder = matcher.group(1);
                String key = matcher.group(2);
                if (replacements.containsKey(key))
                    read.set(i, line = line.replaceAll(Pattern.quote(placeholder), replacements.get(key)));
            }
        }
        return StringUtils.join(read, '\n');
    }
    
    public static CharSequence renderStaticResource(InputStream templateContents) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(templateContents, writer);
            return writer.getBuffer();
        } finally {
            IOUtils.closeQuietly(templateContents);
        }
    }
}
