package org.ulpgc.codestormah.mercadona.model;

public class ProductTextProcessor {

    private static final String HACENDADO = "hacendado";
    private static final String DEFAULT_BRAND = "Other";

    public static String normalizeName(String rawName) {
        String normalized = rawName.toLowerCase();

        return normalized
                .replace(HACENDADO, "")
                .replaceAll("[^\\p{L}0-9 ]", "")
                .trim();
    }

    public static String extractBrand(String rawName) {
        String normalized = rawName.toLowerCase();

        return normalized.contains(HACENDADO)
                ? "Hacendado"
                : DEFAULT_BRAND;
    }
}
