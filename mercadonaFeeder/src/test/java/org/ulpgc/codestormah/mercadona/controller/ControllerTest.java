package org.ulpgc.codestormah.mercadona.controller;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.mercadona.model.Product;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {

    @Test
    void shouldExecuteFeederAndStoreWithoutException() {

        ProductFeeder feeder = max -> List.of(
                new Product("1","test","t","b","cat",1.0,"l",1.0,false)
        );

        ProductStore store = products -> {
            assertEquals(1, products.size());
        };

        Controller controller = new Controller(feeder, store);

        assertDoesNotThrow(() -> {
            controller.startScheduler(1);

            Thread.sleep(100);
        });
    }
}
