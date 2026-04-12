package org.ulpgc.codestormah.mercadona;

import org.ulpgc.codestormah.mercadona.controller.*;

public class Main {
    static void main() {
        try {
            ProductFeeder feeder = new MercadonaFeeder();
            ProductSerializer serializer = new DatabaseProductSerializer();

            Controller controller = new Controller(feeder, serializer);
            controller.execute(-1);

        } catch (Exception e) {
            System.err.println("Application error: " + e.getMessage());
        }
    }
}