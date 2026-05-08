package org.ulpgc.codestormah.business.datamart;

import org.ulpgc.codestormah.business.model.Product;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProductStore {
    // Mapa: Clave = "id-source" | Valor = El producto más reciente
    private final Map<String, Product> latestProducts = new ConcurrentHashMap<>();

    private final Comparator<Product> priceComparator = Comparator
            .comparing(Product::getUnitPrice)
            .thenComparing(Product::getId)
            .thenComparing(Product::getSource);

    public void addProduct(Product product) {
        String key = product.getId() + "-" + product.getSource();

        // Aquí podrías añadir una lógica de timestamp (ts) para asegurar
        // que no guardas un evento viejo encima de uno nuevo.
        latestProducts.put(key, product);
    }

    public Set<String> getCategories() {
        Set<String> categories = new TreeSet<>();
        for (Product p : latestProducts.values()) {
            categories.add(p.getCategory());
        }
        return categories;
    }

    public Collection<Product> getProductsByCategory(String category) {
        List<Product> filtered = new ArrayList<>();
        for (Product p : latestProducts.values()) {
            if (p.getCategory().equalsIgnoreCase(category)) {
                filtered.add(p);
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
        List<Product> products = (List<Product>) getProductsByCategory(category);
        return products.isEmpty() ? null : products.get(products.size() - 1);
    }
}