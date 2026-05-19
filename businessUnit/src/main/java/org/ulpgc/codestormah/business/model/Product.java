package org.ulpgc.codestormah.business.model;

public record Product(String ts, String ss, String id, String name, String normalizedName,
                      String brand, String category, double unitPrice, String unit,
                      double quantity, boolean isOnSale) {}