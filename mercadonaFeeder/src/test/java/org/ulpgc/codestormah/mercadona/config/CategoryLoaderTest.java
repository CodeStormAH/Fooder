package org.ulpgc.codestormah.mercadona.config;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CategoryLoaderTest {

    @Test
    void shouldLoadAllowedCategoriesFromJsonFile() throws Exception {

        File tempFile = File.createTempFile("categories", ".json");

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("""
                {
                  "allowedCategories": [
                    "sidra y cava",
                    "tónica y bitter"
                  ]
                }
            """);
        }

        Set<String> result = CategoryLoader.load(tempFile.getAbsolutePath());

        assertEquals(2, result.size());
        assertTrue(result.contains("sidra y cava"));
        assertTrue(result.contains("tónica y bitter"));
    }

    @Test
    void shouldThrowExceptionWhenFileDoesNotExist() {

        assertThrows(RuntimeException.class, () ->
                CategoryLoader.load("non_existing_file.json")
        );
    }
}
