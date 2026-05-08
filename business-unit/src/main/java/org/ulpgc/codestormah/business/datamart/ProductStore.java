package org.ulpgc.codestormah.business.datamart;

import org.ulpgc.codestormah.business.model.Product;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ProductStore {
    // El mapa de historial es el único almacén de datos
    private final Map<String, List<Product>> history = new ConcurrentHashMap<>();

    // Definimos el comparador aquí para que pueda usarse en toda la clase
    private final Comparator<Product> priceComparator = Comparator
            .comparing(Product::getUnitPrice)
            .thenComparing(Product::getId)
            .thenComparing(Product::getSource);

    public void addProduct(Product product) {
        String key = product.getId() + "-" + product.getSource();
        history.computeIfAbsent(key, k -> new ArrayList<>()).add(product);
    }

    public List<Product> getProductHistory(String productId, String source) {
        return history.getOrDefault(productId + "-" + source, Collections.emptyList());
    }

    // CORRECCIÓN AQUÍ:
    public Collection<Product> getProductsByCategory(String category) {
        List<Product> filtered = new ArrayList<>();

        // Recorremos el historial y sacamos el último precio de cada producto
        for (List<Product> productHistory : history.values()) {
            if (!productHistory.isEmpty()) {
                Product latest = productHistory.get(productHistory.size() - 1);
                if (latest.getCategory().equalsIgnoreCase(category)) {
                    filtered.add(latest);
                }
            }
        }
        // Usamos el comparador que definimos arriba
        filtered.sort(priceComparator);
        return filtered;
    }

    public Product getCheapestProduct(String category) {
        return getProductsByCategory(category).stream()
                .findFirst()
                .orElse(null);
    }

    public Product getMostExpensiveProduct(String category) {
        List<Product> products = (List<Product>) getProductsByCategory(category);
        return products.isEmpty() ? null : products.get(products.size() - 1);
    }

    public Set<String> getCategories() {
        return history.values().stream()
                .flatMap(List::stream)
                .map(Product::getCategory)
                .collect(Collectors.toSet());
    }

    // Este método ya lo hace bien usando stream, lo mantenemos por si la API lo usa
    public List<Product> getLatestProductsByCategory(String category) {
        return (List<Product>) getProductsByCategory(category);
    }

    public Map<String, Object> getRecommendation(String category) {
        List<Product> products = (List<Product>) getProductsByCategory(category);

        if (products.isEmpty()) return Collections.emptyMap();

        Map<String, DoubleSummaryStatistics> statsBySource = products.stream()
                .collect(Collectors.groupingBy(
                        Product::getSource,
                        Collectors.summarizingDouble(Product::getUnitPrice)
                ));

        String bestSource = statsBySource.entrySet().stream()
                .min(Comparator.comparingDouble(e -> e.getValue().getAverage()))
                .map(Map.Entry::getKey)
                .orElse("Desconocido");

        Map<String, Object> result = new HashMap<>();
        result.put("category", category);
        result.put("recommendedSource", bestSource);

        // Añadimos el "Más barato individual" como pediste
        Product cheapest = products.get(0);
        result.put("cheapestProduct", Map.of(
                "name", cheapest.getName(),
                "price", cheapest.getUnitPrice(),
                "source", cheapest.getSource()
        ));

        result.put("comparison", statsBySource.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> Math.round(e.getValue().getAverage() * 100.0) / 100.0 + " € de media"
        )));

        return result;
    }
}