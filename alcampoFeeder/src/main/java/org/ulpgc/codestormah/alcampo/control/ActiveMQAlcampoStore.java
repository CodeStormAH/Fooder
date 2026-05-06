package org.ulpgc.codestormah.alcampo.control;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.ulpgc.codestormah.alcampo.model.Product;

import javax.jms.*;
import java.util.List;
import java.time.Instant;

public class ActiveMQAlcampoStore implements AlcampoStore {
    private final String brokerUrl;
    private final String topicName;
    private final String source;
    private final Gson gson = new Gson();

    public ActiveMQAlcampoStore(String brokerUrl, String topicName, String source) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.source = source;
    }

    @Override
    public void store(List<Product> products) {
        try {
            publishToTopic(products);
        } catch (JMSException e) {
            System.err.println("Error enviando a ActiveMQ: " + e.getMessage());
        }
    }

    private void publishToTopic(List<Product> products) throws JMSException {
        Connection connection = createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic topic = session.createTopic(topicName);
        MessageProducer producer = session.createProducer(topic);

        for (Product product : products) {
            JsonObject event = new JsonObject();
            event.addProperty("ts", Instant.now().toString());
            event.addProperty("ss", this.source);
            event.add("payload", gson.toJsonTree(product));
            TextMessage message = session.createTextMessage(event.toString());
            producer.send(message);
        }

        System.out.println(" Enviados " + products.size() + " productos al topic: " + topicName);
        connection.close();
    }

    private Connection createConnection() throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        Connection connection = factory.createConnection();
        connection.start();
        return connection;
    }
}
