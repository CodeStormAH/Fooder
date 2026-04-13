package org.ulpgc.codestormah.mercadona;

import org.ulpgc.codestormah.mercadona.controller.*;

public class Main {
    static void main() {
        try {
            ProductFeeder productFeeder = new MercadonaFeeder();
            ProductSerializer productSerializer = new DatabaseProductSerializer();

            Controller controller = new Controller(productFeeder, productSerializer);
            controller.execute(-1);

        } catch (Exception exception) {
            System.err.println("Application error: " + exception.getMessage());
        }
    }
}