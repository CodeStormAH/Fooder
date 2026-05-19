package org.ulpgc.codestormah.business.control;

import com.google.gson.Gson;
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
    void shouldProcessValidProduct() {
        FakeProductStore store = new FakeProductStore();
        FakeRecommendationStore recStore = new FakeRecommendationStore(store);
        EventProcessor processor = new EventProcessor(store, recStore);
        Gson gson = new Gson();
        String json = """
                {
                  "id":"12345",
                  "category":"Aceites",
                  "unitPrice":2.5
                }
                """;
        Product validProduct = gson.fromJson(json, Product.class);
        processor.processProduct(validProduct);
        assertEquals(1, store.count);
        assertEquals(1, recStore.updates);
    }

    @Test
    void shouldIgnoreInvalidProduct() {
        FakeProductStore store = new FakeProductStore();
        FakeRecommendationStore recStore = new FakeRecommendationStore(store);
        EventProcessor processor = new EventProcessor(store, recStore);
        Gson gson = new Gson();
        processor.processProduct(null);
        String jsonWithoutId = """
                {
                  "category":"Aceites",
                  "unitPrice":2.5
                }
                """;
        Product productWithoutId = gson.fromJson(jsonWithoutId, Product.class);
        processor.processProduct(productWithoutId);
        assertEquals(0, store.count);
        assertEquals(0, recStore.updates);
    }
}