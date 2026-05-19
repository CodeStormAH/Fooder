package org.ulpgc.codestormah.business.model;
import java.util.Map;

public record Recommendation(String category, String recommendedSource, String cheapestProductName,
                             double cheapestUnitPrice, String cheapestSource, Map<String, String> comparison) {}