package org.ulpgc.codestormah.business.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.business.model.Product;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecommendationStoreTest {

    @Test
    void shouldGenerateRecommendation() {
        ProductStore store = new ProductStore();
        store.addProduct(createProduct("1", "dairy", 1.0));
        RecommendationStore rec = new RecommendationStore(store);
        rec.update("dairy");
        assertNotNull(rec.get("dairy"));
    }

    private Product createProduct(String id, String cat, double price) {
        return new Product("ts", "mercadona", id, "milk", "m", "b", cat, price, "u", 1.0, false);
    }
}