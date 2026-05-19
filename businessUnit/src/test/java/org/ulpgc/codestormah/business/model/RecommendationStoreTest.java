package org.ulpgc.codestormah.business.model;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.business.control.ProductStore;
import org.ulpgc.codestormah.business.control.RecommendationStore;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationStoreTest {

    @Test
    void shouldGenerateRecommendationBasedOnLowestAveragePrice() {
        RecommendationStore recStore = setupStoreWithProducts();
        recStore.update("dairy");
        assertEquals("dairy", recStore.get("dairy").category());
        assertEquals("carrefour", recStore.get("dairy").recommendedSource());
    }

    private RecommendationStore setupStoreWithProducts() {
        ProductStore store = new ProductStore();
        populateMercadona(store);
        populateCarrefour(store);
        return new RecommendationStore(store);
    }

    private void populateMercadona(ProductStore s) {
        s.addProduct(createProduct("1", "mercadona", 2.0));
        s.addProduct(createProduct("2", "mercadona", 3.0));
    }

    private void populateCarrefour(ProductStore s) {
        s.addProduct(createProduct("3", "carrefour", 1.0));
        s.addProduct(createProduct("4", "carrefour", 1.5));
    }

    private Product createProduct(String id, String ss, double price) {
        return new Product("ts", ss, id, "milk", "m", "b", "dairy", price, "u", 1.0, false);
    }
}