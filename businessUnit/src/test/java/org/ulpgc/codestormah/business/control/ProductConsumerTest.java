package org.ulpgc.codestormah.business.control;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductConsumerTest {

    @Test
    void shouldStartWithoutCrash() {
        ProductConsumer consumer = new ProductConsumer("tcp://localhost:61616", "test", null);
        assertNotNull(consumer);
    }
}