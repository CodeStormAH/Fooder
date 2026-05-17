package org.ulpgc.codestormah.business.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.business.model.Product;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationStoreTest {

    @Test
    void shouldGenerateRecommendation() {

        ProductStore store = new ProductStore();

        Product p = new Product();
        set(p, "1", "mercadona", "milk", "dairy", 1.0);

        store.addProduct(p);

        RecommendationStore rec = new RecommendationStore(store);

        rec.update("dairy");

        assertNotNull(rec.get("dairy"));
    }

    private void set(Product p, String id, String ss, String name, String cat, double price) {
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
            catF.set(p, cat);
            priceF.set(p, price);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}