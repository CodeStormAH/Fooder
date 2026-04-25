package org.ulpgc.codestormah.eventstore;

import org.ulpgc.codestormah.eventstore.control.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: <BrokerURL> <Topic> <Source>");
            return;
        }

        String brokerUrl = args[0]; // tcp://localhost:61616
        String topic = args[1];     // prediction.Product
        String source = args[2];    // Tú: "alcampo" | Compañero: "mercadona"

        FileEventStore fileStore = new FileEventStore("datalake");
        ActiveMQSubscriber subscriber = new ActiveMQSubscriber(brokerUrl, topic, source, fileStore);

        System.out.println("🚀 Event Store Builder started for source: " + source);
        subscriber.start();

        // Mantener el proceso vivo
        Thread.currentThread().join();
    }
}
