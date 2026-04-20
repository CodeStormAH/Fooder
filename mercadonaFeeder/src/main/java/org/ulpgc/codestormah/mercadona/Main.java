package org.ulpgc.codestormah.mercadona;

import org.ulpgc.codestormah.mercadona.config.CategoryLoader;
import org.ulpgc.codestormah.mercadona.controller.*;

import java.util.Set;

public class Main {
    static void main(String[] args) {
        try {
            String dbPath = args.length > 0 ? args[0] : null;
            String apiUrl = args.length > 1 ? args[1] : null;
            String categoriesPath = args.length > 2 ? args[2] : null;
            String categoriesFile = args.length > 3 ? args[3] : null;

            if (dbPath == null || apiUrl == null || categoriesPath == null) {
                System.err.println("Usage: java Main <dbPath> <apiUrl>");
                return;
            }

            Set<String> allowedCategories = CategoryLoader.load(categoriesFile);

            ProductStore productSerializer = new DatabaseProductStore(dbPath);
            ProductFeeder productFeeder = new MercadonaFeeder(apiUrl, categoriesPath, allowedCategories);

            Controller controller = new Controller(productFeeder, productSerializer);
            controller.execute(-1);

        } catch (Exception exception) {
            System.err.println("Application error: " + exception.getMessage());
        }
    }
}
