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

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);
    private final ProductStore productStore;
    private final RecommendationStore recommendationStore;
    private final Gson gson = new Gson();

    public EventProcessor(ProductStore productStore, RecommendationStore recommendationStore) {
        this.productStore = productStore;
        this.recommendationStore = recommendationStore;
    }

    public void processProduct(Product product) {
        try {
            executeProductProcessing(product);
        } catch (Exception e) {
            LOGGER.error("Error processing Product object: {}", product, e);
        }
    }

    private void executeProductProcessing(Product p) {
        if (p != null && p.id() != null) storeAndRecommend(p);
    }

    private void storeAndRecommend(Product p) {
        productStore.addProduct(p);
        recommendationStore.update(p.category());
    }

    public void loadHistoricalData(String eventStorePath) {
        Path root = Paths.get(eventStorePath);
        if (Files.exists(root)) tryWalkPath(root);
        else LOGGER.warn("No historical event store found at path: {}", eventStorePath);
    }

    private void tryWalkPath(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            processEventPaths(paths);
        } catch (IOException e) {
            LOGGER.error("Error walking event store directory", e);
        }
    }

    private void processEventPaths(Stream<Path> paths) {
        paths.filter(p -> p.toString().endsWith(".events")).sorted().forEach(this::loadEventsFromFile);
    }

    private void loadEventsFromFile(Path path) {
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines.forEach(this::parseAndProcessLine);
        } catch (IOException e) {
            LOGGER.error("Error reading event file: {}", path, e);
        }
    }

    private void parseAndProcessLine(String line) {
        try {
            processProduct(gson.fromJson(line, Product.class));
        } catch (Exception ex) {
            LOGGER.error("Error parsing historical JSON line: {}", line, ex);
        }
    }
}