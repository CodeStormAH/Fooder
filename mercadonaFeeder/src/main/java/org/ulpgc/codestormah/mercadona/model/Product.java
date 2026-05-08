package org.ulpgc.codestormah.mercadona.model;

public record Product(String id, String name, String normalizedName, String brand, String category, double unitPrice,
                      String unit, double quantity, boolean isOnSale) {

    @Override
    public String toString() {
        return name + " | " + brand + " | " + unitPrice + "€ | " + quantity + unit + " | " + category + " | On offer: " + isOnSale;
    }
}
