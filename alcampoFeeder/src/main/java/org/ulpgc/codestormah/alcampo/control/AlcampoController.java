package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlcampoController {
    private final AlcampoFeeder feeder;
    private final AlcampoStore store;

    public AlcampoController(AlcampoFeeder feeder, AlcampoStore store) {
        this.feeder = feeder;
        this.store = store;
    }

    public void startScheduled(long initialDelay, long interval, TimeUnit unit) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        execute();
                    } catch (Exception e) {
                        System.err.println("Error during scheduled execution: " + e.getMessage());
                    }
                },
                initialDelay,
                interval,
                unit
        );
    }

    public void execute() {
        System.out.println("Starting Alcampo data collection...");
        List<Product> products = feeder.fetchProducts();

        if (!products.isEmpty()) {
            System.out.println("Collected " + products.size() + " products. Storing data...");
            store.store(products);
            System.out.println("Done!");
        } else {
            System.out.println("No data collected.");
        }
    }
}