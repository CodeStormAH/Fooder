package org.ulpgc.codestormah.business.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductConsumerTest {

    @Test
    void shouldStartWithoutCrash() {

        EventProcessor processor = null;

        ProductConsumer consumer = new ProductConsumer(
                "tcp://localhost:61616",
                "test",
                processor
        );

        assertNotNull(consumer);
    }
}