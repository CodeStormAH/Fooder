package org.ulpgc.codestormah.mercadona;

import org.ulpgc.codestormah.mercadona.control.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.ulpgc.codestormah.mercadona.config.CategoryLoader.load;
import static org.ulpgc.codestormah.mercadona.control.ActiveMQFactory.createStore;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

     static void main(String[] args) {
        try {
            executeApp(args);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Application failed", e);
        }
    }

    private static void executeApp(String[] args) throws Exception {
        validateArgs(args);
        ProductStore store = createStore(args[3], args[4], args[5]);
        MercadonaFeeder feeder = new MercadonaFeeder(args[0], args[1], load(args[2]));
        startController(feeder, store);
    }

    private static void validateArgs(String[] args) {
        if (args.length < 6) throw new IllegalArgumentException("Invalid args");
    }

    private static void startController(MercadonaFeeder feeder, ProductStore store) {
        logger.info("Starting Mercadona feeder system");
        new Controller(feeder, store).startScheduler(-1);
    }
}