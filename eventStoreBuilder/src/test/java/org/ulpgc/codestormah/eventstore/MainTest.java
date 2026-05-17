package org.ulpgc.codestormah.eventstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void shouldRejectInvalidArgs() {

        String[] args = {"only-one"};

        assertDoesNotThrow(() -> {
            Main.main(args);
        });
    }
}