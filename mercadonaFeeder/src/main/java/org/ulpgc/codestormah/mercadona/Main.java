package org.ulpgc.codestormah.mercadona;

import org.ulpgc.codestormah.mercadona.controller.*;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.ulpgc.codestormah.mercadona.config.CategoryLoader.load;
import static org.ulpgc.codestormah.mercadona.controller.ActiveMQFactory.createStore;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

     static void main(String[] args) {

        try {

            validateArgs(args);

            String apiUrl = args[0];
            String categoriesPath = args[1];
            String categoriesFile = args[2];
            String brokerUrl = args[3];
            String topicName = args[4];

            logger.info("Starting Mercadona feeder");

            Set<String> allowedCategories = load(categoriesFile);

            logger.info("Connecting to broker: " + brokerUrl);

            ProductStore store = createStore(
                    brokerUrl,
                    topicName,
                    "mercadona"
            );

            MercadonaFeeder feeder = new MercadonaFeeder(
                    apiUrl,
                    categoriesPath,
                    allowedCategories
            );

            Controller controller = new Controller(feeder, store);

            logger.info("Scheduler started");

            controller.startScheduler(-1);

        } catch (Exception e) {

            logger.log(
                    Level.SEVERE,
                    "Application failed",
                    e
            );
        }
    }

    private static void validateArgs(String[] args) {

        if (args.length < 5) {

            throw new IllegalArgumentException(
                    "Usage: java Main <apiUrl> <categoriesPath> <categoriesFile> <brokerUrl> <topic>"
            );
        }
    }
}