package org.ulpgc.codestormah.mercadona.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductTextProcessorTest {

    @Test
    void shouldNormalizeNameRemovingBrandAndSymbols() {
        String result = ProductTextProcessor.normalizeName("HACENDADO Vino Tinto!!!");
        assertEquals("vino tinto", result);
    }

    @Test
    void shouldDetectHacendadoBrand() {
        assertEquals("Hacendado", ProductTextProcessor.extractBrand("HACENDADO Cola Zero"));
    }

    @Test
    void shouldReturnOtherBrandIfNotHacendado() {
        assertEquals("Other", ProductTextProcessor.extractBrand("Coca Cola Zero"));
    }
}