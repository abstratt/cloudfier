package com.abstratt.kirra.populator;

import java.io.IOException;
import java.io.Reader;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

public class DataParser {
    public static JsonNode parse(Reader contents) throws IOException, JsonParseException, JsonProcessingException {
        JsonParser parser = DataParser.jsonFactory.createJsonParser(contents);
        parser.setCodec(new ObjectMapper());
        return parser.readValueAsTree();
    }

    static JsonFactory jsonFactory = new JsonFactory();

    static {
        DataParser.jsonFactory.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        DataParser.jsonFactory.configure(Feature.ALLOW_SINGLE_QUOTES, true);
        DataParser.jsonFactory.configure(Feature.ALLOW_COMMENTS, true);
        DataParser.jsonFactory.configure(Feature.CANONICALIZE_FIELD_NAMES, false);
    }
}