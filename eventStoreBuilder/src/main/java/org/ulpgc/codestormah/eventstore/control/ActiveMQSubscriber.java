package org.ulpgc.codestormah.eventstore.control;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class ActiveMQSubscriber {

    public record Config(String brokerUrl, String topic, String source) {}

    private final Config config;
    private final FileEventStore store;
    private final Gson gson = new Gson();
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    public ActiveMQSubscriber(Config config, FileEventStore store) {
        this.config = config;
        this.store = store;
    }

    public void start() {
        while (!attemptConnection()) {
            sleep();
        }
    }

    private boolean attemptConnection() {
        try {
            return executeConnection();
        } catch (Exception e) {
            return handleConnectionError(e);
        }
    }

    private boolean executeConnection() throws JMSException {
        connect();
        listen();
        return true;
    }

    private void connect() throws JMSException {
        connection = createConnection();
        connection.setClientID(buildClientId());
        connection.start();
        createSessionAndConsumer();
    }

    private Connection createConnection() throws JMSException {
        return new ActiveMQConnectionFactory(config.brokerUrl()).createConnection();
    }

    private String buildClientId() {
        return "StoreBuilder_" + config.source() + "_" + config.topic();
    }

    private void createSessionAndConsumer() throws JMSException {
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        consumer = session.createDurableSubscriber(session.createTopic(config.topic()), buildSubscriptionName());
    }

    private String buildSubscriptionName() {
        return "Durable_" + config.source() + "_" + config.topic();
    }

    private void listen() throws JMSException {
        consumer.setMessageListener(this::handleMessage);
    }

    private void handleMessage(Message message) {
        try {
            processMessage(message);
        } catch (Exception e) {
            logError("Error processing message", e);
        }
    }

    private void processMessage(Message m) throws Exception {
        JsonObject json = parse(extractTextMessage(m).getText());
        store.dispatch(json.toString(), config.topic(), config.source());
    }

    private TextMessage extractTextMessage(Message m) {
        if (m instanceof TextMessage tm) return tm;
        throw new IllegalArgumentException("Unsupported JMS type: " + m.getClass());
    }

    private JsonObject parse(String text) {
        return gson.fromJson(text, JsonObject.class);
    }

    private boolean handleConnectionError(Exception e) {
        logError("Connection failed. Retrying...", e);
        return false;
    }

    private void logError(String msg, Exception e) {
        System.err.println(msg);
        System.err.println(e.getMessage());
    }

    private void sleep() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}