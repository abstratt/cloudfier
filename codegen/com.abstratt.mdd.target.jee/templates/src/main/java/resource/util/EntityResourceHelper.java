package resource.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

public class EntityResourceHelper {
    public static String getEntityRepresentation(String entityName, String baseUri) throws IOException {
        Map<String, String> substitutions = new LinkedHashMap<>();
        substitutions.put("baseUri", baseUri);
        InputStream stream = EntityResourceHelper.class.getResourceAsStream("/schema/entities/" + entityName + ".json");
        if (stream == null)
            return null;
        try {
            String contents = IOUtils.toString(stream, "UTF-8");
            return StrSubstitutor.replace(contents, substitutions);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
