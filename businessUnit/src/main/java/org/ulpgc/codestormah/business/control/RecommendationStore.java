package org.ulpgc.codestormah.business.control;

import org.ulpgc.codestormah.business.model.Product;
import org.ulpgc.codestormah.business.model.Recommendation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RecommendationStore {
    private final ProductStore productStore;
    private final Map<String, Recommendation> recommendations = new ConcurrentHashMap<>();

    public RecommendationStore(ProductStore productStore) {
        this.productStore = productStore;
    }

    public void update(String category) {
        List<Product> products = productStore.getProductsByCategory(category);
        if (products.isEmpty()) return;

        Map<String, DoubleSummaryStatistics> statsBySource = products.stream()
                .collect(Collectors.groupingBy(Product::getSs,
                        Collectors.summarizingDouble(Product::getUnitPrice)));

        String bestSource = statsBySource.entrySet().stream()
                .min(Comparator.comparingDouble(e -> e.getValue().getAverage()))
                .map(Map.Entry::getKey)
                .orElse("Desconocido");

        Product cheapest = products.get(0);

        Map<String, String> comparison = statsBySource.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Math.round(e.getValue().getAverage() * 100.0) / 100.0 + " € de media"
                ));

        recommendations.put(category, new Recommendation(
                category, bestSource,
                cheapest.getName(), cheapest.getUnitPrice(), cheapest.getSs(),
                comparison
        ));
    }

    public Recommendation get(String category) {
        return recommendations.get(category);
    }
}
