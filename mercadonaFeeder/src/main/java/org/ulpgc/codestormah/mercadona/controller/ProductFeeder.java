package org.ulpgc.codestormah.mercadona.controller;

import java.io.IOException;

public interface ProductFeeder {
    void run(int maxProducts) throws IOException;
}
