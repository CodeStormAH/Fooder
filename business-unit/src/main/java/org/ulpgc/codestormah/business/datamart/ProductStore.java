package org.ulpgc.codestormah.business.datamart;


import org.ulpgc.codestormah.business.model.Product;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProductStore {
    private final Map<String, TreeSet<Product>> productsByCategory;
    private final Comparator<Product> comparator;

    public ProductStore() {
        // SOLUCIÓN: El comparador se instancia solo una vez.
        // Ordena por precio unitario, desempata por ID y luego por Supermercado.
        this.comparator = Comparator.comparing(Product::getUnitPrice)
                .thenComparing(Product::getId)
                .thenComparing(Product::getSource);

        this.productsByCategory = new ConcurrentHashMap<>();
    }

    public void addProduct(Product product) {
        productsByCategory
                .computeIfAbsent(product.getCategory(), k -> new TreeSet<>(comparator))
                .add(product);
    }

    public Set<String> getCategories() {
        return productsByCategory.keySet();
    }

    public Collection<Product> getProductsByCategory(String category) {
        return productsByCategory.getOrDefault(category, new TreeSet<>());
    }

    public Product getCheapestProduct(String category) {
        TreeSet<Product> products = productsByCategory.get(category);
        return (products == null || products.isEmpty()) ? null : products.first();
    }

    public Product getMostExpensiveProduct(String category) {
        TreeSet<Product> products = productsByCategory.get(category);
        return (products == null || products.isEmpty()) ? null : products.last();
    }
}
