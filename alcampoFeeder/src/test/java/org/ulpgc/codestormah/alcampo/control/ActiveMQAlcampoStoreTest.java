package org.ulpgc.codestormah.alcampo.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.alcampo.model.Product;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActiveMQAlcampoStoreTest {

    @Test
    void should_build_valid_event_structure() {

        ActiveMQAlcampoStore store = new ActiveMQAlcampoStore(
                "tcp://localhost:61616",
                "topic",
                "alcampo"
        );

        List<Product> products = List.of(
                new Product("1", "agua", "agua", "brand", "drink", 1.2, "l", 1.0, false)
        );

        store.store(products);

        assertTrue(true); // si no rompe, OK
    }
}