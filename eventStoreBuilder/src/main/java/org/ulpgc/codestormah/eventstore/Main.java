package org.ulpgc.codestormah.eventstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.codestormah.eventstore.control.ActiveMQSubscriber;
import org.ulpgc.codestormah.eventstore.control.FileEventStore;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        validateArgs(args);
        startApplication(args);
    }

    private static void validateArgs(String[] args) {
        if (args.length < 4) throw new IllegalArgumentException("Usage: <BrokerURL> <Topic> <Source> <Root>");
    }

    private static void startApplication(String[] args) throws Exception {
        logger.info("Event Store Builder started for source={}", args[2]);
        ActiveMQSubscriber.Config config = new ActiveMQSubscriber.Config(args[0], args[1], args[2]);
        new ActiveMQSubscriber(config, new FileEventStore(args[3])).start();
        Thread.currentThread().join();
    }
}