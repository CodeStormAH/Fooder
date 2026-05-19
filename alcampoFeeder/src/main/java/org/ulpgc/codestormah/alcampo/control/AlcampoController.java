package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AlcampoController {
    private static final Logger logger = Logger.getLogger(AlcampoController.class.getName());
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
                        logger.log(Level.SEVERE,
                                "Error during scheduled execution: " + e.getMessage(), e);
                    }
                },
                initialDelay,
                interval,
                unit
        );
    }

    public void execute() {
        logger.info("Starting Alcampo data collection...");
        List<Product> products = feeder.fetchProducts();
        if (!products.isEmpty()) {
            logger.info("Collected " + products.size() + " products. Storing data...");
            store.store(products);
            logger.info("Done!");
        } else {
            logger.info("No data collected.");
        }
    }
}