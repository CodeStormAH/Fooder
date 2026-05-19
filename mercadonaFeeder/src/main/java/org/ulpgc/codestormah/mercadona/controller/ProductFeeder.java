package org.ulpgc.codestormah.mercadona.controller;
import org.ulpgc.codestormah.mercadona.model.Product;
import java.util.List;

public interface ProductFeeder {
    List<Product> run(int maxProducts) throws Exception;
}