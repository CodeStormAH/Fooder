package org.ulpgc.codestormah.mercadona.controller;

import org.ulpgc.codestormah.mercadona.model.Product;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public record Controller(ProductFeeder feeder, ProductStore serializer) {
    public void execute(int maxProducts) throws IOException, SQLException {
        List<Product> products = feeder.getProducts(maxProducts);
        System.out.println("Products fetched: " + products.size());
        serializer.save(products);
    }
}