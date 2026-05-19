package org.ulpgc.codestormah.business.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.business.model.Product;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventProcessorTest {

    @Test
    void shouldProcessValidProduct() {
        TestEnv env = setupEnv();
        env.proc().processProduct(createProduct("12345", "Aceites"));
        assertEquals(1, env.store().count);
        assertEquals(1, env.recStore().updates);
    }

    @Test
    void shouldIgnoreInvalidProduct() {
        TestEnv env = setupEnv();
        env.proc().processProduct(null);
        env.proc().processProduct(createProduct(null, "Aceites"));
        assertEquals(0, env.store().count);
        assertEquals(0, env.recStore().updates);
    }

    private TestEnv setupEnv() {
        FakeProductStore s = new FakeProductStore();
        FakeRecommendationStore r = new FakeRecommendationStore(s);
        return new TestEnv(s, r, new EventProcessor(s, r));
    }

    private Product createProduct(String id, String cat) {
        return new Product("ts", "ss", id, "name", "norm", "brand", cat, 2.5, "ud", 1.0, false);
    }

    private record TestEnv(FakeProductStore store, FakeRecommendationStore recStore, EventProcessor proc) {}

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
}