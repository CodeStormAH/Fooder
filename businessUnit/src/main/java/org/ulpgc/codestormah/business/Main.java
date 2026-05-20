package org.ulpgc.codestormah.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.codestormah.business.control.RecommendationStore;
import org.ulpgc.codestormah.business.view.UIService;
import org.ulpgc.codestormah.business.control.ProductConsumer;
import org.ulpgc.codestormah.business.control.EventProcessor;
import org.ulpgc.codestormah.business.control.ProductStore;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

     static void main(String[] args) {
        if (args.length < 4) exitWithUsage();
        else startApplication(args);
    }

    private static void exitWithUsage() {
        logger.error("Invalid arguments. Usage: <BrokerURL> <TopicName> <EventStorePath> <ApiPort>");
        System.exit(1);
    }

    private static void startApplication(String[] args) {
        ProductStore ps = new ProductStore();
        RecommendationStore rs = new RecommendationStore(ps);
        setupAndStart(args, ps, rs);
    }

    private static void setupAndStart(String[] args, ProductStore ps, RecommendationStore rs) {
        EventProcessor processor = new EventProcessor(ps, rs);
        loadHistory(args[2], processor);
        startConsumer(args[0], args[1], processor);
        startApi(Integer.parseInt(args[3]), ps, rs);
    }

    private static void loadHistory(String path, EventProcessor processor) {
        logger.info("Loading historical data from: {}", path);
        processor.loadHistoricalData(path);
    }

    private static void startConsumer(String broker, String topic, EventProcessor p) {
        logger.info("Starting ProductConsumer (broker={}, topic={})", broker, topic);
        new ProductConsumer(broker, topic, p).start();
    }

    private static void startApi(int port, ProductStore ps, RecommendationStore rs) {
        logger.info("Starting API on port {}", port);
        new UIService(ps, rs, port).start();
        logger.info("Business Unit started successfully (Lambda architecture active)");
    }
}