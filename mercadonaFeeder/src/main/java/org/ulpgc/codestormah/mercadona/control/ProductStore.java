package org.ulpgc.codestormah.mercadona.control;
import org.ulpgc.codestormah.mercadona.model.Product;
import java.util.List;

public interface ProductStore {
    void save(List<Product> products);
}