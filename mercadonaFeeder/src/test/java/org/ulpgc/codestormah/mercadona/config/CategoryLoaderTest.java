package org.ulpgc.codestormah.mercadona.config;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CategoryLoaderTest {

    @Test
    void shouldLoadAllowedCategoriesFromJsonFile() throws Exception {
        File temp = createTempJsonFile();
        Set<String> result = CategoryLoader.load(temp.getAbsolutePath());
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Set.of("sidra y cava", "tónica y bitter")));
    }

    private File createTempJsonFile() throws Exception {
        File temp = File.createTempFile("categories", ".json");
        Files.writeString(temp.toPath(), "{\"allowedCategories\": [\"sidra y cava\", \"tónica y bitter\"]}");
        return temp;
    }

    @Test
    void shouldThrowExceptionWhenFileDoesNotExist() {
        assertThrows(RuntimeException.class, () -> CategoryLoader.load("non_existing_file.json"));
    }
}