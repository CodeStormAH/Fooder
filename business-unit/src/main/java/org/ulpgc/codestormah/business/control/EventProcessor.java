package org.ulpgc.codestormah.business.control;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.codestormah.business.model.Product;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

public class EventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);

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
                recommendationStore.update(product.getCategory());
            }

        } catch (Exception e) {
            logger.error("Error parsing JSON event: {}", json, e);
        }
    }

    public void loadHistoricalData(String eventStorePath) {
        Path root = Paths.get(eventStorePath);

        if (!Files.exists(root)) {
            logger.warn("No historical event store found at path: {}", eventStorePath);
            return;
        }

        try (Stream<Path> paths = Files.walk(root)) {

            paths.filter(p -> p.toString().endsWith(".events"))
                    .forEach(path -> {

                        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
                            lines.forEach(this::processJson);

                        } catch (IOException e) {
                            logger.error("Error reading event file: {}", path, e);
                        }
                    });

        } catch (IOException e) {
            logger.error("Error walking event store directory: {}", eventStorePath, e);
        }
    }
}