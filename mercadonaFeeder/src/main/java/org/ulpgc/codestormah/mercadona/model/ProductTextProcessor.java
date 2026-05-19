package org.ulpgc.codestormah.mercadona.model;

public class ProductTextProcessor {

    private static final String HACENDADO = "hacendado";
    private static final String DEFAULT_BRAND = "Other";

    public static String normalizeName(String rawName) {
        return rawName.toLowerCase()
                .replace(HACENDADO, "")
                .replaceAll("[^\\p{L}0-9 ]", "")
                .trim();
    }

    public static String extractBrand(String rawName) {
        return rawName.toLowerCase().contains(HACENDADO) ? "Hacendado" : DEFAULT_BRAND;
    }
}