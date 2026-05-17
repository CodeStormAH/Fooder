package org.ulpgc.codestormah.business.model;

import java.util.Map;

public class Recommendation {
    private final String category;
    private final String recommendedSource;
    private final String cheapestProductName;
    private final double cheapestUnitPrice;
    private final String cheapestSource;
    private final Map<String, String> comparison;

    public Recommendation(String category, String recommendedSource,
                          String cheapestProductName, double cheapestUnitPrice,
                          String cheapestSource, Map<String, String> comparison) {
        this.category = category;
        this.recommendedSource = recommendedSource;
        this.cheapestProductName = cheapestProductName;
        this.cheapestUnitPrice = cheapestUnitPrice;
        this.cheapestSource = cheapestSource;
        this.comparison = comparison;
    }

    public String getCategory()            { return category; }
    public String getRecommendedSource()   { return recommendedSource; }
    public String getCheapestProductName() { return cheapestProductName; }
    public double getCheapestUnitPrice()   { return cheapestUnitPrice; }
    public String getCheapestSource()      { return cheapestSource; }
    public Map<String, String> getComparison() { return comparison; }
}
