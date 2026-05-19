package org.ulpgc.codestormah.business.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.business.model.Product;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EventProcessorHistoryTest {

    @Test
    void shouldLoadHistoricalData() throws Exception {
        Path dir = createTestDirectoryWithEvent();
        FakeStore store = new FakeStore();
        new EventProcessor(store, new FakeRec(store)).loadHistoricalData(dir.toString());
        assertTrue(store.count > 0);
    }

    private Path createTestDirectoryWithEvent() throws Exception {
        Path dir = Files.createTempDirectory("history");
        Files.writeString(dir.resolve("test.events"), "{\"id\":\"1\",\"category\":\"dairy\",\"unitPrice\":2.0}\n");
        return dir;
    }

    static class FakeStore extends ProductStore {
        int count = 0;
        @Override
        public void addProduct(Product product) {
            count++;
        }
    }

    static class FakeRec extends RecommendationStore {
        public FakeRec(ProductStore store) {
            super(store);
        }
        @Override
        public void update(String category) {
        }
    }
}