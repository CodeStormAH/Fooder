package org.ulpgc.codestormah.mercadona.control;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.mercadona.model.Product;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MercadonaFeederTest {

    @Test
    void shouldFilterProductsByAllowedCategoriesAndResolveCorrectly() throws Exception {
        MercadonaFeeder feeder = new FakeFeeder(Set.of("sidra y cava"));
        List<Product> result = feeder.run(10);
        assertEquals(1, result.size());
        assertEquals("sidra y cava", result.getFirst().category());
    }

    private static class FakeFeeder extends MercadonaFeeder {

        public FakeFeeder(Set<String> allowed) {
            super("http://fake", "fake", allowed);
        }

        @Override
        public List<Product> run(int maxProducts) {
            JsonObject json = buildJson("1", "Sidra Asturiana");
            return List.of(toTestProduct(json, "sidra y cava"));
        }

        private JsonObject buildJson(String id, String name) {
            JsonObject p = new JsonObject();
            p.addProperty("id", id);
            p.addProperty("display_name", name);
            return addPriceInfo(p);
        }

        private JsonObject addPriceInfo(JsonObject p) {
            JsonObject price = new JsonObject();
            price.addProperty("unit_price", 1.2);
            price.addProperty("unit_size", 1);
            p.add("price_instructions", price);
            return p;
        }

        private Product toTestProduct(JsonObject json, String cat) {
            String id = json.get("id").getAsString();
            String name = json.get("display_name").getAsString();
            return new Product(id, name, "norm", "brand", cat, 1.0, "l", 1.0, false);
        }
    }
}