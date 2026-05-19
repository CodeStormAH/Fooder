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
        if (!products.isEmpty()) buildAndStoreRecommendation(category, products);
    }

    private void buildAndStoreRecommendation(String cat, List<Product> p) {
        Map<String, DoubleSummaryStatistics> stats = getStats(p);
        recommendations.put(cat, createRec(cat, p.getFirst(), stats));
    }

    private Map<String, DoubleSummaryStatistics> getStats(List<Product> p) {
        return p.stream().collect(Collectors.groupingBy(Product::ss, Collectors.summarizingDouble(Product::unitPrice)));
    }

    private Recommendation createRec(String cat, Product cheap, Map<String, DoubleSummaryStatistics> stats) {
        return new Recommendation(cat, getBest(stats), cheap.name(), cheap.unitPrice(), cheap.ss(), getComp(stats));
    }

    private String getBest(Map<String, DoubleSummaryStatistics> stats) {
        return stats.entrySet().stream().min(Comparator.comparingDouble(e -> e.getValue().getAverage())).map(Map.Entry::getKey).orElse("Desconocido");
    }

    private Map<String, String> getComp(Map<String, DoubleSummaryStatistics> stats) {
        return stats.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> formatStat(e.getValue())));
    }

    private String formatStat(DoubleSummaryStatistics stat) {
        return Math.round(stat.getAverage() * 100.0) / 100.0 + " € de media";
    }

    public Recommendation get(String category) {
        return recommendations.get(category);
    }
}