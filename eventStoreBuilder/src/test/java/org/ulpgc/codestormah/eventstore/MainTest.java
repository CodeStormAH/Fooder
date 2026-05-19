package org.ulpgc.codestormah.eventstore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MainTest {

    @Test
    void shouldRejectInvalidArgs() {
        String[] args = {"only-one"};
        assertThrows(IllegalArgumentException.class, () -> Main.main(args));
    }
}