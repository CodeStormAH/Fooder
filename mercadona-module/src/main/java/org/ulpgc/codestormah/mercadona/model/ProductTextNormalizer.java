package org.ulpgc.codestormah.mercadona.model;

public class ProductTextNormalizer {
    public static String normalizeName(String name) {
        return  name.toLowerCase()
                .replace("hacendado", "")
                .replaceAll("[^\\p{L}0-9 ]", "")
                .trim();
    }

    public static String extractBrand(String name) {
        return name.toLowerCase().contains("hacendado") ? "Hacendado" : "Other";
    }
}
