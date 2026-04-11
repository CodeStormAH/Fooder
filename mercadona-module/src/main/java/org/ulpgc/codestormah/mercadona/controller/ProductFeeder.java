package org.ulpgc.codestormah.mercadona.controller;

import org.ulpgc.codestormah.mercadona.model.Product;
import java.io.IOException;
import java.util.List;

public interface ProductFeeder {
    List<Product> getProducts(int limit) throws IOException;
}
