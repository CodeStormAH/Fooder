package org.ulpgc.codestormah.mercadona.controller;

import org.ulpgc.codestormah.mercadona.model.Product;

import java.sql.SQLException;
import java.util.List;

public interface ProductSerializer {
    void save(List<Product> products) throws SQLException;
}
