package org.ulpgc.codestormah.business.control;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class ProductConsumer {
    private final String brokerUrl;
    private final String topicName;
    private final EventProcessor processor;

    public ProductConsumer(String brokerUrl, String topicName, EventProcessor processor) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.processor = processor;
    }

    public void start() {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            Connection connection = factory.createConnection();

            connection.setClientID("BusinessUnit_API_Client");
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(topicName);

            MessageConsumer consumer = session.createDurableSubscriber(topic, "BusinessUnit_Sub");

            System.out.println("🎧 Escuchando eventos en tiempo real en ActiveMQ...");

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String json = ((TextMessage) message).getText();
                        processor.processJson(json); // Delegamos el trabajo al procesador unificado
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }
}
