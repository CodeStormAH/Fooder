package org.ulpgc.codestormah.mercadona.model;

public record Product(String id, String name, String normalizedName, String brand, String category, double unitPrice,
                      String unit, double amount, boolean onOffer) {

    @Override
    public String toString() {
        return name + " | " + brand + " | " + unitPrice + "€ | " + amount + unit + " | " + category + " | On offer: " + onOffer;
    }
}
