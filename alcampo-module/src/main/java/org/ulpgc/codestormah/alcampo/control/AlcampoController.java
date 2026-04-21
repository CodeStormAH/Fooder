package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;

import java.util.List;

public class AlcampoController {
    private final AlcampoFeeder feeder;
    private final AlcampoStore store;

    public AlcampoController(AlcampoFeeder feeder, AlcampoStore store) {
        this.feeder = feeder;
        this.store = store;
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