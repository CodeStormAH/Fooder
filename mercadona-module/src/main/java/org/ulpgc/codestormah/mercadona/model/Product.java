package org.ulpgc.codestormah.mercadona.model;

public class Product {
    private final String id;
    private final String name;
    private final String normalizedName;
    private final String brand;
    private final String category;
    private final double unitPrice;
    private final double amount;
    private final String unit;
    private final boolean onOffer;

    public Product(String id, String name, String normalizedName, String brand, String category,
                   double unitPrice, String unit, double amount, boolean onOffer) {
        this.id = id;
        this.name = name;
        this.normalizedName = normalizedName;
        this.brand = brand;
        this.category = category;
        this.unitPrice = unitPrice;
        this.unit = unit;
        this.amount = amount;
        this.onOffer = onOffer;
    }

    @Override
    public String toString() {
        return name + " | " + brand + " | " + unitPrice + "€ | " + amount + unit + " | " + category + " | On offer: " + onOffer;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getBrand() {
        return brand;
    }

    public String getCategory() {
        return category;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getAmount() {
        return amount;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isOnOffer() {
        return onOffer;
    }

    public String getId() {
        return id;
    }
}
