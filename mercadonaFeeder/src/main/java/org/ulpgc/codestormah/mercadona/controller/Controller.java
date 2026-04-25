package org.ulpgc.codestormah.mercadona.controller;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public record Controller(ProductFeeder feeder) {
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public void execute(int maxProducts) throws Exception {
        feeder.getProducts(maxProducts);
    }

    public void startScheduler(int maxProducts) {
        scheduler.scheduleAtFixedRate(
                () -> runSafely(maxProducts),
                0,
                1,
                TimeUnit.DAYS
        );
    }

    private void runSafely(int maxProducts) {
        try {
            execute(maxProducts);
        } catch (Exception e) {
            throw new RuntimeException("Scheduled execution failed", e);
        }
    }
}