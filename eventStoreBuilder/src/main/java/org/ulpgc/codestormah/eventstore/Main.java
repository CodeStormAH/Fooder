package org.ulpgc.codestormah.eventstore;

import org.ulpgc.codestormah.eventstore.control.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: <BrokerURL> <Topic> <Source>");
            return;
        }

        String brokerUrl = args[0]; // tcp://localhost:61616
        String topic = args[1];     // comparison.Product
        String source = args[2];    // Tú: "alcampo" | Compañero: "mercadona"
        String root = args[3];      // "eventstore"

        FileEventStore fileStore = new FileEventStore(root);
        ActiveMQSubscriber subscriber = new ActiveMQSubscriber(brokerUrl, topic, source, fileStore);

        System.out.println("🚀 Event Store Builder started for source: " + source);
        subscriber.start();

        // Mantener el proceso vivo
        Thread.currentThread().join();
    }
}
