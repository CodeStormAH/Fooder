package org.ulpgc.codestormah.business.control;

import com.google.gson.Gson;
import org.ulpgc.codestormah.business.datamart.ProductStore;
import org.ulpgc.codestormah.business.model.Event;
import org.ulpgc.codestormah.business.model.Product;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class EventProcessor {
    private final ProductStore store;
    private final Gson gson;

    public EventProcessor(ProductStore store) {
        this.store = store;
        this.gson = new Gson();
    }

    // Lógica UNIFICADA para tiempo real e histórico
    public void processJson(String json) {
        try {
            Event event = gson.fromJson(json, Event.class);
            if (event != null && event.getPayload() != null) {
                Product product = event.getPayload();

                // SOLUCIÓN: Inyectamos el supermercado de origen (ss) al producto
                product.setSource(event.getSs());

                store.addProduct(product);
            }
        } catch (Exception e) {
            System.err.println("Error parseando JSON: " + e.getMessage());
        }
    }

    // REQUISITO: Carga de eventos históricos (Sprint 3)
    public void loadHistoricalData(String eventStorePath) {
        Path root = Paths.get(eventStorePath);
        if (!Files.exists(root)) {
            System.out.println("No se encontró histórico en: " + eventStorePath);
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(p -> p.toString().endsWith(".events"))
                    .forEach(path -> {
                        try (Stream<String> lines = Files.lines(path)) {
                            lines.forEach(this::processJson);
                        } catch (IOException e) { e.printStackTrace(); }
                    });
        } catch (IOException e) { e.printStackTrace(); }
    }
}
