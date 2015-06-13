package com.abstratt.kirra.populator;

import java.io.IOException;
import java.io.Reader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DataParser {
    public static JsonNode parse(Reader contents) throws IOException, JsonParseException, JsonProcessingException {
        JsonParser parser = DataParser.jsonFactory.createJsonParser(contents);
        parser.setCodec(new ObjectMapper());
        return parser.readValueAsTree();
    }

    static JsonFactory jsonFactory = new JsonFactory();

    static {
        DataParser.jsonFactory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        DataParser.jsonFactory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        DataParser.jsonFactory.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        DataParser.jsonFactory.configure(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, false);
    }
}