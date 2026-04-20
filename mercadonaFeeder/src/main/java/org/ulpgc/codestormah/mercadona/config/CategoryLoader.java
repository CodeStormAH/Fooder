package org.ulpgc.codestormah.mercadona.config;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class CategoryLoader {

    public static Set<String> load(String path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            CategoryConfig config = mapper.readValue(
                    new File(path),
                    CategoryConfig.class
            );
            return new HashSet<>(config.getAllowedCategories());
        } catch (Exception e) {
            throw new RuntimeException("Error loading categories", e);
        }
    }
}
