package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;

import java.util.List;

public interface AlcampoSerializer {
    void serialize(List<Product> productos);
}

