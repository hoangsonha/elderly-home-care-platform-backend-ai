package com.capstone_project.elderly_platform.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom deserializer for JSON string fields.
 * Handles both JSON string and JSON object formats.
 * If the value is a JSON object, it will be converted to a JSON string.
 * If the value is already a JSON string, it will be kept as is.
 */
public class JsonStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        if (node == null) {
            return null;
        }
        
        // If it's already a text node (string), return it as is
        if (node.isTextual()) {
            return node.asText();
        }
        
        // If it's an object or array, convert it to JSON string
        if (node.isObject() || node.isArray()) {
            return node.toString();
        }
        
        // For other types (number, boolean, null), convert to string
        return node.asText();
    }
}


