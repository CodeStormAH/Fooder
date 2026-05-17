package org.ulpgc.codestormah.mercadona.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductTextProcessorTest {

    @Test
    void shouldNormalizeNameRemovingBrandAndSymbols() {

        String input = "HACENDADO Vino Tinto!!!";

        String result = ProductTextProcessor.normalizeName(input);

        assertEquals("vino tinto", result);
    }

    @Test
    void shouldDetectHacendadoBrand() {

        String input = "HACENDADO Cola Zero";

        assertEquals("Hacendado",
                ProductTextProcessor.extractBrand(input));
    }

    @Test
    void shouldReturnOtherBrandIfNotHacendado() {

        String input = "Coca Cola Zero";

        assertEquals("Other",
                ProductTextProcessor.extractBrand(input));
    }
}
