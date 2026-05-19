package org.ulpgc.codestormah.alcampo.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.alcampo.model.Product;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlcampoControllerTest {

    @Test
    void should_execute_feeder_and_store() {
        AlcampoFeeder feeder = () -> List.of(
                new Product("1", "agua", "agua", "brand", "drink", 1.0, "l", 1.0, false)
        );
        AlcampoStore store = products -> {
            assertEquals(1, products.size());
            assertEquals("agua", products.get(0).getName());
        };
        AlcampoController controller = new AlcampoController(feeder, store);
        controller.execute();
        assertTrue(true);
    }

    @Test
    void should_not_fail_when_no_products() {
        AlcampoFeeder feeder = List::of;
        AlcampoStore store = products -> fail("No debería llamarse");
        AlcampoController controller = new AlcampoController(feeder, store);
        controller.execute();
        assertTrue(true);
    }
}