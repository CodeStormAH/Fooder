package org.ulpgc.codestormah.business.control;

import com.google.gson.Gson;
import org.ulpgc.codestormah.business.model.Product;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

public class EventProcessor {
    private final ProductStore productStore;
    private final RecommendationStore recommendationStore;
    private final Gson gson;

    public EventProcessor(ProductStore productStore, RecommendationStore recommendationStore) {
        this.productStore = productStore;
        this.recommendationStore = recommendationStore;
        this.gson = new Gson();
    }

    public void processJson(String json) {
        try {
            Product product = gson.fromJson(json, Product.class);
            if (product != null && product.getId() != null) {
                productStore.addProduct(product);
                recommendationStore.update(product.getCategory()); // recálculo inmediato
            }
        } catch (Exception e) {
            System.err.println("Error parseando JSON: " + e.getMessage());
        }
    }

    public void loadHistoricalData(String eventStorePath) {
        Path root = Paths.get(eventStorePath);
        if (!Files.exists(root)) {
            System.out.println("No se encontró histórico en: " + eventStorePath);
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(p -> p.toString().endsWith(".events"))
                    .forEach(path -> {
                        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
                            lines.forEach(this::processJson);
                        } catch (IOException e) { e.printStackTrace(); }
                    });
        } catch (IOException e) { e.printStackTrace(); }
    }
}
