package org.ulpgc.codestormah.business.model;

public class Product {
    private String ts;
    private String ss;
    private String id;
    private String name;
    private String normalizedName;
    private String brand;
    private String category;
    private double unitPrice;
    private String unit;
    private double quantity;
    private boolean isOnSale;

    public Product() {}

    public String getTs() {return ts; }
    public String getSs() {return ss; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getNormalizedName() {return normalizedName;}
    public String getBrand() { return brand; }
    public String getCategory() { return category; }
    public double getUnitPrice() { return unitPrice; }
    public String getUnit() { return unit; }
    public double getQuantity() { return quantity; }
    public boolean isOnSale() { return isOnSale; }
}
