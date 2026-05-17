package org.ulpgc.codestormah.business.control;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

public class ProductConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ProductConsumer.class);

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

            logger.info("Listening to ActiveMQ topic: {}", topicName);

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMessage) {
                        String json = textMessage.getText();
                        processor.processJson(json);
                    } else {
                        logger.warn("Received non-text JMS message: {}", message.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    logger.error("Error processing JMS message", e);
                }
            });

        } catch (Exception e) {
            logger.error("Failed to start ProductConsumer (broker={}, topic={})", brokerUrl, topicName, e);
        }
    }
}