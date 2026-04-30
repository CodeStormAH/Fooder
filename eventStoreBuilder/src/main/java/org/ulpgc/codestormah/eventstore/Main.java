package org.ulpgc.codestormah.eventstore;

import org.ulpgc.codestormah.eventstore.control.*;

public class Main {
     static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: <BrokerURL> <Topic> <Source> <Root>");
            return;
        }

        String brokerUrl = args[0];
        String topic = args[1];
        String source = args[2];
        String root = args[3];

        FileEventStore fileStore = new FileEventStore(root);
        ActiveMQSubscriber subscriber = new ActiveMQSubscriber(brokerUrl, topic, source, fileStore);

        System.out.println("Event Store Builder started for source: " + source);
        subscriber.start();

        Thread.currentThread().join();
    }
}
