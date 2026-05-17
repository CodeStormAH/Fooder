package org.ulpgc.codestormah.alcampo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.codestormah.alcampo.control.*;

import java.util.concurrent.TimeUnit;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 5) {
            printUsageError();
            return;
        }

        String targetUrl = args[0];
        String categoriesFilePath = args[1];
        String brokerUrl = args[2];
        String topicName = args[3];
        String source = args[4];

        startApplication(targetUrl, categoriesFilePath, brokerUrl, topicName, source);
    }

    private static void startApplication(String url, String categoriesPath, String brokerUrl, String topic, String source) {

        logger.info("Starting program...");
        logger.info("Starting Publisher (Scraper -> ActiveMQ)...");
        logger.info("Broker: {} | Topic: {}", brokerUrl, topic);

        AlcampoFeeder feeder = new AlcampoScraperFeeder(url, categoriesPath);
        AlcampoStore store = new ActiveMQAlcampoStore(brokerUrl, topic, source);
        AlcampoController controller = new AlcampoController(feeder, store);

        controller.startScheduled(0, 24, TimeUnit.HOURS);
    }

    private static void printUsageError() {
        logger.error("Error: Missing configuration parameters.");
        logger.error("Usage: <URL> <Database_File> <Categories_File>");
    }
}