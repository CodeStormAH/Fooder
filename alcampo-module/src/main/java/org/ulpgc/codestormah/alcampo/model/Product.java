package org.ulpgc.codestormah.alcampo.model;

public class Product {
    private final String id;
    private final String name;
    private final String normalizedName;
    private final String brand;
    private final String category;
    private final double price;
    private final double unitPrice;
    private final double quantity;
    private final String unit;
    private final boolean isOnSale;

    public Product(String id, String name, String normalizedName, String brand, String category,
                   double price, double unitPrice, String unit, double quantity, boolean isOnSale) {
        this.id = id;
        this.name = name;
        this.normalizedName = normalizedName;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.unitPrice = unitPrice;
        this.unit = unit;
        this.quantity = quantity;
        this.isOnSale = isOnSale;
    }

    @Override
    public String toString() {
        return name + " | " + brand + " | Total: " + price + "€ (" + unitPrice + "€/" + unit + ") | On Sale: " + isOnSale;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public String getBrand() { return brand; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public double getUnitPrice() { return unitPrice; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public boolean isOnSale() { return isOnSale; }
}

