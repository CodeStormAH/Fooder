package org.ulpgc.codestormah.business.control;

import org.ulpgc.codestormah.business.model.Product;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ProductStore {

    private final Map<String, List<Product>> history = new ConcurrentHashMap<>();
    private final Comparator<Product> priceComparator = Comparator.comparing(Product::unitPrice).thenComparing(Product::id).thenComparing(Product::ss);

    public void addProduct(Product product) {
        String key = product.id() + "-" + product.ss();
        history.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(product);
    }

    public List<Product> getProductHistory(String id, String source) {
        List<Product> full = history.getOrDefault(id + "-" + source, Collections.emptyList());
        return full.isEmpty() ? full : filterHistory(full);
    }

    private List<Product> filterHistory(List<Product> full) {
        full.sort(Comparator.comparing(Product::ts));
        return extractPriceChanges(full);
    }

    private List<Product> extractPriceChanges(List<Product> full) {
        List<Product> filtered = new ArrayList<>();
        Product prev = null;
        for (Product p : full) prev = addIfChanged(p, prev, filtered);
        return filtered;
    }

    private Product addIfChanged(Product curr, Product prev, List<Product> out) {
        if (prev == null || prev.unitPrice() != curr.unitPrice()) out.add(curr);
        return curr;
    }

    public List<Product> getProductsByCategory(String category) {
        List<Product> filtered = new ArrayList<>();
        for (List<Product> hist : history.values()) addLatestIfMatches(hist, category, filtered);
        filtered.sort(priceComparator);
        return filtered;
    }

    private void addLatestIfMatches(List<Product> hist, String cat, List<Product> out) {
        if (!hist.isEmpty()) checkAndAddLatest(hist, cat, out);
    }

    private void checkAndAddLatest(List<Product> hist, String cat, List<Product> out) {
        Product latest = hist.stream().max(Comparator.comparing(Product::ts)).orElse(null);
        if (latest != null && latest.category().equalsIgnoreCase(cat)) out.add(latest);
    }

    public Product getCheapestProduct(String category) {
        return getProductsByCategory(category).stream().findFirst().orElse(null);
    }

    public Product getMostExpensiveProduct(String category) {
        List<Product> products = getProductsByCategory(category);
        return products.isEmpty() ? null : products.getLast();
    }

    public Set<String> getCategories() {
        return history.values().stream().flatMap(List::stream).map(Product::category).collect(Collectors.toSet());
    }
}