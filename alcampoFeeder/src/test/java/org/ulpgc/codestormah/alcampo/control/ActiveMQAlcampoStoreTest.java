package org.ulpgc.codestormah.alcampo.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.alcampo.model.Product;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ActiveMQAlcampoStoreTest {

    @Test
    void shouldBuildValidEventStructure() {
        ActiveMQAlcampoStore store = new ActiveMQAlcampoStore("tcp://localhost:61616", "topic", "alcampo");
        assertDoesNotThrow(() -> store.store(List.of(createTestProduct())));
    }

    private Product createTestProduct() {
        return new Product("1", "agua", "agua", "brand", "drink", 1.2, "l", 1.0, false);
    }
}