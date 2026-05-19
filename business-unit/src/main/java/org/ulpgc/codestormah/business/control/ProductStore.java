package org.ulpgc.codestormah.business.control;

import org.ulpgc.codestormah.business.model.Product;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ProductStore {
    private final Map<String, List<Product>> history = new ConcurrentHashMap<>();

    private final Comparator<Product> priceComparator = Comparator
            .comparing(Product::getUnitPrice)
            .thenComparing(Product::getId)
            .thenComparing(Product::getSs);

    public void addProduct(Product product) {
        String key = product.getId() + "-" + product.getSs();
        history.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(product);
    }

    public List<Product> getProductHistory(String productId, String source) {
        List<Product> fullHistory = history.getOrDefault(productId + "-" + source, Collections.emptyList());
        if (fullHistory.isEmpty()) return fullHistory;

        fullHistory.sort(Comparator.comparing(Product::getTs));
        List<Product> filteredHistory = new ArrayList<>();
        Product previous = null;

        for (Product current : fullHistory) {
            if (previous == null || previous.getUnitPrice() != current.getUnitPrice()) {
                filteredHistory.add(current);
                previous = current;
            }
        }
        return filteredHistory;
    }

    public List<Product> getProductsByCategory(String category) {
        List<Product> filtered = new ArrayList<>();
        for (List<Product> productHistory : history.values()) {
            if (!productHistory.isEmpty()) {
                Product latest = productHistory.stream()
                        .max(Comparator.comparing(Product::getTs))
                        .orElse(null);
                if (latest != null && latest.getCategory().equalsIgnoreCase(category)) {
                    filtered.add(latest);
                }
            }
        }
        filtered.sort(priceComparator);
        return filtered;
    }

    public Product getCheapestProduct(String category) {
        return getProductsByCategory(category).stream()
                .findFirst()
                .orElse(null);
    }

    public Product getMostExpensiveProduct(String category) {
        List<Product> products = getProductsByCategory(category);
        return products.isEmpty() ? null : products.get(products.size() - 1);
    }

    public Set<String> getCategories() {
        return history.values().stream()
                .flatMap(List::stream)
                .map(Product::getCategory)
                .collect(Collectors.toSet());
    }
}