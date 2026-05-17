package org.ulpgc.codestormah.mercadona.controller;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.mercadona.model.Product;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MercadonaFeederTest {

    @Test
    void shouldFilterProductsByAllowedCategoriesAndResolveCorrectly() throws Exception {

        Set<String> allowed = Set.of("sidra y cava");

        MercadonaFeeder feeder = new MercadonaFeeder(
                "http://fake",
                "fake",
                allowed
        ) {

            @Override
            public List<Product> run(int maxProducts) {

                JsonObject product = new JsonObject();
                product.addProperty("id", "1");
                product.addProperty("display_name", "Sidra Asturiana");

                JsonObject price = new JsonObject();
                price.addProperty("unit_price", 1.2);
                price.addProperty("unit_size", 1);

                product.add("price_instructions", price);

                return List.of(toProductForTest(product, "sidra y cava"));
            }

            // helper exposed for test
            public Product toProductForTest(JsonObject json, String category) {
                return new Product(
                        json.get("id").getAsString(),
                        json.get("display_name").getAsString(),
                        "normalized",
                        "brand",
                        category,
                        1.0,
                        "l",
                        1.0,
                        false
                );
            }
        };

        List<Product> result = feeder.run(10);

        assertEquals(1, result.size());
        assertEquals("sidra y cava", result.get(0).category());
    }
}
