package org.ulpgc.codestormah.alcampo;

import org.ulpgc.codestormah.alcampo.control.*;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        if (args.length < 4) {
            printUsageError();
            return;
        }

        String targetUrl = args[0];
        String categoriesFilePath = args[1];
        String brokerUrl = args[2];

        // El nombre del topic lo podemos dejar fijo como constante, o pasarlo como args[3] si lo prefieres.
        String topicName = args[3];

        startApplication(targetUrl, categoriesFilePath, brokerUrl, topicName);
    }

    private static void startApplication(String url, String categoriesPath, String brokerUrl, String topic) {
        System.out.println("Starting program...");

        System.out.println("Starting Publisher (Scraper -> ActiveMQ)...");
        System.out.println("Broker: " + brokerUrl + " | Topic: " + topic);

        AlcampoFeeder feeder = new AlcampoScraperFeeder(url, categoriesPath);
        AlcampoStore store = new ActiveMQAlcampoStore(brokerUrl, topic);
        AlcampoController controller = new AlcampoController(feeder, store);

        controller.startScheduled(0, 1, TimeUnit.DAYS);
    }

    private static void printUsageError() {
        System.err.println("Error: Missing configuration parameters.");
        System.err.println("Usage: <URL> <Database_File> <Categories_File>");
    }
}