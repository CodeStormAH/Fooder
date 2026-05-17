package org.ulpgc.codestormah.business.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.business.model.Product;

import static org.junit.jupiter.api.Assertions.*;

class EventProcessorTest {

    static class FakeProductStore extends ProductStore {
        int count = 0;

        @Override
        public void addProduct(Product product) {
            count++;
        }
    }

    static class FakeRecommendationStore extends RecommendationStore {

        int updates = 0;

        public FakeRecommendationStore(ProductStore store) {
            super(store);
        }

        @Override
        public void update(String category) {
            updates++;
        }
    }

    @Test
    void shouldProcessValidJson() {

        FakeProductStore store = new FakeProductStore();
        FakeRecommendationStore recStore = new FakeRecommendationStore(store);

        EventProcessor processor = new EventProcessor(store, recStore);

        String json = """
                {
                  "id":"1",
                  "category":"bebidas",
                  "unitPrice":2.5
                }
                """;

        processor.processJson(json);

        assertEquals(1, store.count);
        assertEquals(1, recStore.updates);
    }

    @Test
    void shouldIgnoreInvalidJson() {

        FakeProductStore store = new FakeProductStore();
        FakeRecommendationStore recStore = new FakeRecommendationStore(store);

        EventProcessor processor = new EventProcessor(store, recStore);

        processor.processJson("invalid-json");

        assertEquals(0, store.count);
        assertEquals(0, recStore.updates);
    }
}