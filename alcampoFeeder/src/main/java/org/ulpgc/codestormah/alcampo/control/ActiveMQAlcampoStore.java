package org.ulpgc.codestormah.alcampo.control;

import com.google.gson.Gson;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.ulpgc.codestormah.alcampo.model.Product;

import javax.jms.*;
import java.util.List;

public class ActiveMQAlcampoStore implements AlcampoStore {
    private final String brokerUrl;
    private final String topicName;
    private final Gson gson;

    public ActiveMQAlcampoStore(String brokerUrl, String topicName) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.gson = new Gson();
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
            String json = gson.toJson(product);
            TextMessage message = session.createTextMessage(json);
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
