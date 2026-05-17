package org.ulpgc.codestormah.business.model;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.business.control.ProductStore;
import org.ulpgc.codestormah.business.control.RecommendationStore;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationStoreTest {

    @Test
    void shouldGenerateRecommendationBasedOnLowestAveragePrice() {

        ProductStore store = new ProductStore();
        RecommendationStore recommendationStore = new RecommendationStore(store);

        Product p1 = createProduct("1", "mercadona", "milk", "dairy", 2.0);
        Product p2 = createProduct("2", "mercadona", "milk", "dairy", 3.0);

        Product p3 = createProduct("3", "carrefour", "milk", "dairy", 1.0);
        Product p4 = createProduct("4", "carrefour", "milk", "dairy", 1.5);

        store.addProduct(p1);
        store.addProduct(p2);
        store.addProduct(p3);
        store.addProduct(p4);

        recommendationStore.update("dairy");

        Recommendation rec = recommendationStore.get("dairy");

        assertNotNull(rec);
        assertEquals("dairy", rec.getCategory());

        // Carrefour tiene media más baja (1.25 vs 2.5)
        assertEquals("carrefour", rec.getRecommendedSource());
    }

    private Product createProduct(
            String id,
            String ss,
            String name,
            String category,
            double price
    ) {
        Product p = new Product();

        try {
            var idF = Product.class.getDeclaredField("id");
            var ssF = Product.class.getDeclaredField("ss");
            var nameF = Product.class.getDeclaredField("name");
            var catF = Product.class.getDeclaredField("category");
            var priceF = Product.class.getDeclaredField("unitPrice");

            idF.setAccessible(true);
            ssF.setAccessible(true);
            nameF.setAccessible(true);
            catF.setAccessible(true);
            priceF.setAccessible(true);

            idF.set(p, id);
            ssF.set(p, ss);
            nameF.set(p, name);
            catF.set(p, category);
            priceF.set(p, price);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return p;
    }
}