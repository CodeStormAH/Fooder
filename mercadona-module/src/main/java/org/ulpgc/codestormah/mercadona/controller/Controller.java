package org.ulpgc.codestormah.mercadona.controller;

import org.ulpgc.codestormah.mercadona.model.Product;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Controller {

    private final ProductFeeder feeder;
    private final ProductSerializer serializer;

    public Controller(ProductFeeder feeder, ProductSerializer serializer) {
        this.feeder = feeder;
        this.serializer = serializer;
    }

    public void execute(int limit) throws IOException, SQLException {
        List<Product> products = feeder.getProducts(limit);
        System.out.println("Products fetched: " + products.size());
        serializer.save(products);
    }
}