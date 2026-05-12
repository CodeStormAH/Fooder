package org.ulpgc.codestormah.business.model;

public class Product {
    private String id;
    private String name;
    private String brand;
    private String category;
    private double price;
    private double unitPrice;
    private String unit;
    private double quantity;
    private boolean isOnSale;

    private transient String source;

    public Product() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public double getUnitPrice() { return unitPrice; }
    public String getUnit() { return unit; }
    public double getQuantity() { return quantity; }
    public boolean isOnSale() { return isOnSale; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
