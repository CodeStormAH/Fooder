package org.ulpgc.codestormah.business.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.business.model.Product;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductStoreTest {

    @Test
    void shouldStoreAndRetrieveCategory() {
        ProductStore store = new ProductStore();
        store.addProduct(createProduct("1", "dairy", 1.0));
        assertEquals(1, store.getProductsByCategory("dairy").size());
    }

    @Test
    void shouldReturnCheapestProduct() {
        ProductStore store = new ProductStore();
        store.addProduct(createProduct("1", "dairy", 2.0));
        store.addProduct(createProduct("2", "dairy", 1.0));
        assertEquals(1.0, store.getCheapestProduct("dairy").unitPrice());
    }

    private Product createProduct(String id, String cat, double price) {
        return new Product("ts", "a", id, "milk", "m", "b", cat, price, "u", 1.0, false);
    }
}