package org.ulpgc.codestormah.mercadona.controller;
import org.ulpgc.codestormah.mercadona.model.Product;
import java.util.List;

public interface ProductStore {
    void save(List<Product> products);
}