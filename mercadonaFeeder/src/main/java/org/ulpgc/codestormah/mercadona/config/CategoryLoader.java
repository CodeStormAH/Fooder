package org.ulpgc.codestormah.mercadona.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class CategoryLoader {

    public static Set<String> load(String path) {

        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.readTree(new File(path));

            Set<String> categories = new HashSet<>();

            root.get("allowedCategories")
                    .forEach(node -> categories.add(node.asText()));

            return categories;

        } catch (Exception e) {
            throw new RuntimeException("Error loading categories", e);
        }
    }
}