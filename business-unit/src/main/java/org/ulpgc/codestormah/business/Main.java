package org.ulpgc.codestormah.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.codestormah.business.control.RecommendationStore;
import org.ulpgc.codestormah.business.view.ApiController;
import org.ulpgc.codestormah.business.control.ProductConsumer;
import org.ulpgc.codestormah.business.control.EventProcessor;
import org.ulpgc.codestormah.business.control.ProductStore;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        if (args.length < 4) {
            logger.error("Invalid arguments. Usage: <BrokerURL> <TopicName> <EventStorePath> <ApiPort>");
            System.exit(1);
        }

        String brokerUrl = args[0];
        String topicName = args[1];
        String eventStorePath = args[2];
        int apiPort = Integer.parseInt(args[3]);

        ProductStore productStore = new ProductStore();
        RecommendationStore recommendationStore = new RecommendationStore(productStore);
        EventProcessor processor = new EventProcessor(productStore, recommendationStore);

        logger.info("Loading historical data from: {}", eventStorePath);
        processor.loadHistoricalData(eventStorePath);

        logger.info("Starting ProductConsumer (broker={}, topic={})", brokerUrl, topicName);
        ProductConsumer consumer = new ProductConsumer(brokerUrl, topicName, processor);
        consumer.start();

        logger.info("Starting API on port {}", apiPort);
        ApiController api = new ApiController(productStore, recommendationStore, apiPort);
        api.start();

        logger.info("Business Unit started successfully (Lambda architecture active)");
    }
}