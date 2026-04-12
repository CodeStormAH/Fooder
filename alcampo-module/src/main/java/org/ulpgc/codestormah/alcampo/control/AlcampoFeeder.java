package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Products;

import java.util.List;

public interface AlcampoFeeder {
    List<Products> fetchProducts();
}
