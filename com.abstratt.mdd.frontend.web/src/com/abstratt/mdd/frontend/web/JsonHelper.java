package com.abstratt.mdd.frontend.web;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

public class JsonHelper {
	
    private static JsonFactory jsonFactory = new JsonFactory();

    static {
        jsonFactory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        jsonFactory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        jsonFactory.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        jsonFactory.configure(JsonParser.Feature.CANONICALIZE_FIELD_NAMES, true);
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
		objectMapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
		objectMapper.setDateFormat(new SimpleDateFormat("yyyy/MM/dd"));
		jsonFactory.setCodec(objectMapper);
    }


	public static String renderAsJson(Object toRender) {
		try {
			StringWriter writer = new StringWriter();
			((ObjectMapper) jsonFactory.getCodec()).defaultPrettyPrintingWriter().writeValue(writer, toRender);
			return writer.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
    public static <T extends JsonNode> T parse(Reader contents) throws IOException, JsonParseException, JsonProcessingException {
        JsonParser parser = jsonFactory.createJsonParser(contents);
        return (T) parser.readValueAsTree();
    }
	
}
