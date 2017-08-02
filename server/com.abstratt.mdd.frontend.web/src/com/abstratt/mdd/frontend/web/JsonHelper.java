package com.abstratt.mdd.frontend.web;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonHelper {

    public static <T extends JsonNode> T parse(Reader contents) throws IOException, JsonParseException, JsonProcessingException {
        com.fasterxml.jackson.core.JsonParser parser = JsonHelper.jsonFactory.createJsonParser(contents);
        return (T) parser.readValueAsTree();
    }

    public static String renderAsJson(Object toRender) {
        try {
            StringWriter writer = new StringWriter();
            ((ObjectMapper) JsonHelper.jsonFactory.getCodec()).writerWithDefaultPrettyPrinter().writeValue(writer, toRender);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static JsonNode traverse(JsonNode node, String... path) {
        JsonNode result = node;
        for (int i = 0; i < path.length; i++) {
            if (!result.has(path[i])) {
                throw new IllegalArgumentException("Unknown path: " + path[i]);
            }
            result = result.get(path[i]);
        }
        return result;
    }

    private static JsonFactory jsonFactory = new JsonFactory();

    static {
        JsonHelper.jsonFactory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        JsonHelper.jsonFactory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        JsonHelper.jsonFactory.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        JsonHelper.jsonFactory.configure(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, true);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy/MM/dd"));
        JsonHelper.jsonFactory.setCodec(objectMapper);
    }

}
