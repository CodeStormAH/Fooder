package org.ulpgc.codestormah.mercadona.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class CategoryLoader {

    public static Set<String> load(String path) {
        try {
            return parseCategories(path);
        } catch (Exception e) {
            throw new RuntimeException("Error loading categories", e);
        }
    }

    private static Set<String> parseCategories(String path) throws Exception {
        JsonNode root = new ObjectMapper().readTree(new File(path));
        Set<String> categories = new HashSet<>();
        root.get("allowedCategories").forEach(n -> categories.add(n.asText()));
        return categories;
    }
}