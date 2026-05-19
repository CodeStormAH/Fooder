package org.ulpgc.codestormah.alcampo.control;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.codestormah.alcampo.model.Product;
import javax.jms.*;
import java.time.Instant;
import java.util.List;

public class ActiveMQAlcampoStore implements AlcampoStore {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQAlcampoStore.class);
    private final String brokerUrl;
    private final String topicName;
    private final String source;
    private final Gson gson;

    public ActiveMQAlcampoStore(String brokerUrl, String topicName, String source) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.source = source;
        this.gson = new Gson();
    }

    @Override
    public void store(List<Product> products) {
        try {
            publishToTopic(products);
        } catch (JMSException e) {
            logger.error("Error enviando a ActiveMQ: {}", e.getMessage(), e);
        }
    }

    private void publishToTopic(List<Product> products) throws JMSException {
        Connection connection = createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(topicName);
        MessageProducer producer = session.createProducer(topic);
        for (Product product : products) {
            String productJson = createProductJson(product);
            producer.send(session.createTextMessage(productJson));
        }
        logger.info("Sent {} to topic: {}", products.size(), topicName);
        connection.close();
    }

    private String createProductJson(Product product) {
        JsonObject event = new JsonObject();
        event.addProperty("ts", Instant.now().toString());
        event.addProperty("ss", this.source);
        gson.toJsonTree(product).getAsJsonObject().entrySet().forEach(e -> event.add(e.getKey(), e.getValue()));
        return event.toString();
    }

    private Connection createConnection() throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        Connection connection = factory.createConnection();
        connection.start();
        return connection;
    }
}