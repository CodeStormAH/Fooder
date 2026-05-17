package org.ulpgc.codestormah.eventstore.control;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class ActiveMQSubscriber {
    private final String brokerUrl;
    private final String topicName;
    private final String source;
    private final FileEventStore store;

    private final Gson gson = new Gson();

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    public ActiveMQSubscriber(String brokerUrl, String topicName, String source, FileEventStore store) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.source = source;
        this.store = store;
    }

    public void start() {
        while (true) {
            try {
                connect();
                listen();
                break;
            } catch (Exception e) {
                logError("Connection failed. Retrying...", e);
                sleep();
            }
        }
    }

    private void connect() throws JMSException {
        connection = createConnection();
        connection.setClientID(buildClientId());
        connection.start();

        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(topicName);

        consumer = session.createDurableSubscriber(topic, buildSubscriptionName());
    }

    private void listen() throws JMSException {
        consumer.setMessageListener(this::handleMessage);
    }

    private void handleMessage(Message message) {
        try {
            TextMessage textMessage = extractTextMessage(message);

            JsonObject json = parse(textMessage.getText());
            store.dispatch(json.toString(), topicName, source);

        } catch (Exception e) {
            logError("Error processing message", e);
        }
    }

    private TextMessage extractTextMessage(Message message) {
        if (message instanceof TextMessage textMessage) {
            return textMessage;
        }
        throw new IllegalArgumentException(
                "Unsupported JMS message type: " + message.getClass()
        );
    }

    private JsonObject parse(String text) {
        return gson.fromJson(text, JsonObject.class);
    }

    private Connection createConnection() throws JMSException {
        return new ActiveMQConnectionFactory(brokerUrl).createConnection();
    }

    private String buildClientId() {
        return "StoreBuilder_" + source + "_" + topicName;
    }

    private String buildSubscriptionName() {
        return "Durable_" + source + "_" + topicName;
    }

    private void sleep() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void logError(String msg, Exception e) {
        System.err.println(msg);
        System.err.println(e.getMessage());
    }

    public void close() {
        try {
            if (consumer != null) consumer.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            logError("Error closing JMS resources", e);
        }
    }
}
