package org.ulpgc.codestormah.mercadona.controller;

import org.ulpgc.codestormah.mercadona.model.Product;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public record Controller(ProductFeeder feeder, ProductStore store) {

    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public void startScheduler(int maxProducts) {
        scheduler.scheduleAtFixedRate(
                () -> runSafely(maxProducts),
                0,
                5,
                TimeUnit.MINUTES
        );
    }

    private void runSafely(int maxProducts) {
        try {
            List<Product> products = feeder.run(maxProducts);
            store.save(products);

        } catch (Exception e) {
            throw new RuntimeException("Scheduled execution failed", e);
        }
    }
}