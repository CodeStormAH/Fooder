package org.ulpgc.codestormah.mercadona.control;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public record Controller(ProductFeeder feeder, ProductStore store) {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    public void startScheduler(int maxProducts) {
        SCHEDULER.scheduleAtFixedRate(() -> runSafely(maxProducts), 0, 5, TimeUnit.MINUTES);
    }

    private void runSafely(int maxProducts) {
        try {
            executeRun(maxProducts);
        } catch (Exception e) {
            throw new RuntimeException("Scheduled execution failed", e);
        }
    }

    private void executeRun(int max) throws Exception {
        store.save(feeder.run(max));
    }
}