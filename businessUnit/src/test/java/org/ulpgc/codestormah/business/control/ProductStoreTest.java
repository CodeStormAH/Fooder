package org.ulpgc.codestormah.business.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.business.model.Product;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductStoreTest {

    @Test
    void shouldStoreAndRetrieveCategory() {

        ProductStore store = new ProductStore();

        Product p = new Product();
        set(p, "1", "mercadona", "milk", "dairy", 1.0);

        store.addProduct(p);

        List<Product> result = store.getProductsByCategory("dairy");

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnCheapestProduct() {

        ProductStore store = new ProductStore();

        Product p1 = new Product();
        set(p1, "1", "a", "milk", "dairy", 2.0);

        Product p2 = new Product();
        set(p2, "2", "a", "milk", "dairy", 1.0);

        store.addProduct(p1);
        store.addProduct(p2);

        Product cheapest = store.getCheapestProduct("dairy");

        assertEquals(1.0, cheapest.getUnitPrice());
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
