package org.ulpgc.codestormah.alcampo.control;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlcampoScraperFeederTest {

    @Test
    void shouldCreateFeederInstance() {
        assertNotNull(new AlcampoScraperFeeder("http://fake-url", "fake-path.txt"));
    }

    @Test
    void shouldHandleEmptyCategories() {
        AlcampoScraperFeeder feeder = new AlcampoScraperFeeder("http://fake", "missing.txt");
        assertTrue(feeder.fetchProducts().isEmpty());
    }
}