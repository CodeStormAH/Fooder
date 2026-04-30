package org.ulpgc.codestormah.mercadona;

import org.ulpgc.codestormah.mercadona.controller.*;

import java.util.Set;

import static org.ulpgc.codestormah.mercadona.config.CategoryLoader.load;
import static org.ulpgc.codestormah.mercadona.controller.ActiveMQFactory.createPublisher;

public class Main {

     static void main(String[] args) {
        try {
            validateArgs(args);

            String apiUrl = args[0];
            String categoriesPath = args[1];
            String categoriesFile = args[2];
            String connectionPath = args[3];
            String eventTopic = args[4];

            Set<String> allowedCategories = load(categoriesFile);

            EventPublisher publisher =
                    createPublisher(connectionPath, eventTopic, "mercadona");

            MercadonaFeeder feeder = new MercadonaFeeder(
                    apiUrl,
                    categoriesPath,
                    allowedCategories,
                    publisher,
                    eventTopic
            );

            Controller controller = new Controller(feeder);

            controller.startScheduler(-1);

        } catch (Exception e) {
            throw new RuntimeException("Application failed to start", e);
        }
    }

    private static void validateArgs(String[] args) {
        if (args.length < 4) {
            throw new IllegalArgumentException(
                    "Usage: java Main <apiUrl> <categoriesPath> <categoriesFile> <connectionPath>"
            );
        }
    }
}