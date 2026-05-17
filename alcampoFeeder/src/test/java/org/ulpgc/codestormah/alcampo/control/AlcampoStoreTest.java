package org.ulpgc.codestormah.alcampo.control;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlcampoStoreTest {

    @Test
    void should_accept_products() {

        AlcampoStore store = products -> {
            assertNotNull(products);
        };

        store.store(List.of());
    }
}