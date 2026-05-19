package org.ulpgc.codestormah.alcampo.control;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlcampoScraperFeederTest {

    @Test
    void should_create_feeder_instance() {
        AlcampoScraperFeeder feeder = new AlcampoScraperFeeder(
                "http://fake-url",
                "fake-path.txt"
        );
        assertNotNull(feeder);
    }

    @Test
    void should_handle_empty_categories() {
        AlcampoScraperFeeder feeder = new AlcampoScraperFeeder(
                "http://fake-url",
                "non_existing_file.txt"
        );
        var result = feeder.fetchProducts();
        assertTrue(result.isEmpty());
    }
}