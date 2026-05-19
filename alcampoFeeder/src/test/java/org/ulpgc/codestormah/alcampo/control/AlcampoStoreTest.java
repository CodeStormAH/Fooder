package org.ulpgc.codestormah.alcampo.control;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AlcampoStoreTest {

    @Test
    void shouldAcceptProducts() {
        AlcampoStore store = Assertions::assertNotNull;
        assertDoesNotThrow(() -> store.store(List.of()));
    }
}