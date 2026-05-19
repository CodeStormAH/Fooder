package org.ulpgc.codestormah.business.control;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EventProcessorHistoryTest {

    @Test
    void shouldLoadHistoricalData() throws Exception {

        Path dir = Files.createTempDirectory("history");

        Path file = dir.resolve("test.events");

        Files.writeString(file,
                "{\"id\":\"1\",\"category\":\"dairy\",\"unitPrice\":2.0}\n");

        FakeStore store = new FakeStore();
        FakeRec rec = new FakeRec(store);

        EventProcessor processor = new EventProcessor(store, rec);

        processor.loadHistoricalData(dir.toString());

        assertTrue(store.count > 0);
    }

    static class FakeStore extends ProductStore {
        int count = 0;

        @Override
        public void addProduct(org.ulpgc.codestormah.business.model.Product product) {
            count++;
        }
    }

    static class FakeRec extends RecommendationStore {
        int updates = 0;

        public FakeRec(ProductStore store) {
            super(store);
        }

        @Override
        public void update(String category) {
            updates++;
        }
    }
}