package org.ulpgc.codestormah.business.control;

import com.google.gson.Gson;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.codestormah.business.model.Product;

import javax.jms.*;

public class ProductConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductConsumer.class);
    private final String brokerUrl;
    private final String topicName;
    private final EventProcessor processor;
    private final Gson gson = new Gson();

    public ProductConsumer(String brokerUrl, String topicName, EventProcessor processor) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.processor = processor;
    }

    public void start() {
        try {
            connectAndListen();
        } catch (Exception e) {
            LOGGER.error("Failed to start ProductConsumer", e);
        }
    }

    private void connectAndListen() throws JMSException {
        Session session = establishConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
        attachConsumer(session);
    }

    private Connection establishConnection() throws JMSException {
        Connection c = new ActiveMQConnectionFactory(brokerUrl).createConnection();
        c.setClientID("BusinessUnit_API_Client");
        c.start();
        return c;
    }

    private void attachConsumer(Session session) throws JMSException {
        MessageConsumer consumer = session.createDurableSubscriber(session.createTopic(topicName), "BusinessUnit_Sub");
        LOGGER.info("Listening to ActiveMQ topic: {}", topicName);
        consumer.setMessageListener(this::safeProcessMessage);
    }

    private void safeProcessMessage(Message message) {
        try {
            processMessage(message);
        } catch (Exception e) {
            LOGGER.error("Error processing JMS message", e);
        }
    }

    private void processMessage(Message message) throws JMSException {
        if (message instanceof TextMessage tm) processor.processProduct(gson.fromJson(tm.getText(), Product.class));
        else LOGGER.warn("Received non-text JMS message: {}", message.getClass().getSimpleName());
    }
}