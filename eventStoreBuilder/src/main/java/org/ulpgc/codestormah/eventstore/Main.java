package org.ulpgc.codestormah.eventstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.codestormah.eventstore.control.ActiveMQSubscriber;
import org.ulpgc.codestormah.eventstore.control.FileEventStore;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        if (args.length < 4) {
            logger.error("Usage: <BrokerURL> <Topic> <Source> <Root>");
            return;
        }

        String brokerUrl = args[0];
        String topic = args[1];
        String source = args[2];
        String root = args[3];

        FileEventStore fileStore = new FileEventStore(root);
        ActiveMQSubscriber subscriber =
                new ActiveMQSubscriber(brokerUrl, topic, source, fileStore);

        logger.info("Event Store Builder started for source={}", source);

        subscriber.start();

        Thread.currentThread().join();
    }
}