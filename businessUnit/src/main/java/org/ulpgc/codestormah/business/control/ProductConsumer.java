package org.ulpgc.codestormah.business.control;

import com.google.gson.Gson;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.codestormah.business.model.Product;

import javax.jms.*;

public class ProductConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ProductConsumer.class);
    private final String brokerUrl;
    private final String topicName;
    private final EventProcessor processor;
    private final Gson gson;

    public ProductConsumer(String brokerUrl, String topicName, EventProcessor processor) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.processor = processor;
        this.gson = new Gson();
    }

    public void start() {
        try {
            Connection connection = initJmsConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(topicName);
            MessageConsumer consumer = session.createDurableSubscriber(topic, "BusinessUnit_Sub");
            logger.info("Listening to ActiveMQ topic: {}", topicName);
            consumer.setMessageListener(this::handleJmsMessage);
        } catch (Exception e) {
            logger.error("Failed to start ProductConsumer (broker={}, topic={})", brokerUrl, topicName, e);
        }
    }

    private Connection initJmsConnection() throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        Connection connection = factory.createConnection();
        connection.setClientID("BusinessUnit_API_Client");
        connection.start();
        return connection;
    }

    private void handleJmsMessage(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                processTextMessage(textMessage);
            } else {
                logger.warn("Received non-text JMS message: {}", message.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.error("Error processing JMS message", e);
        }
    }

    private void processTextMessage(TextMessage textMessage) throws JMSException {
        String json = textMessage.getText();
        Product product = gson.fromJson(json, Product.class);
        processor.processProduct(product);
    }
}