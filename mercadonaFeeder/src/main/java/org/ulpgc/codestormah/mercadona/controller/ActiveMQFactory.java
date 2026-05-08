package org.ulpgc.codestormah.mercadona.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.ulpgc.codestormah.mercadona.model.Event;
import org.ulpgc.codestormah.mercadona.model.Product;

import javax.jms.*;
import java.time.LocalDateTime;
import java.util.List;

public class ActiveMQFactory {

    public static ProductStore createStore(String brokerUrl, String topicName, String source) throws JMSException {

        Connection connection = createConnection(brokerUrl);
        Session session = createSession(connection);
        MessageProducer producer = createProducer(session, topicName);

        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new ProductStore() {
            @Override
            public void save(List<Product> products) {

                for (Product product : products) {
                    try {
                        Event event = new Event(
                                LocalDateTime.now().withNano(0),
                                source,
                                product
                        );

                        String json = mapper.writeValueAsString(event);
                        TextMessage message = session.createTextMessage(json);
                        producer.send(message);

                    } catch (Exception e) {
                        System.err.println("Error sending product: " + e.getMessage());
                    }
                }
            }
        };
    }

    private static Connection createConnection(String brokerUrl) throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        Connection connection = factory.createConnection();
        connection.start();
        return connection;
    }

    private static Session createSession(Connection connection) throws JMSException {
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    private static MessageProducer createProducer(Session session, String topicName) throws JMSException {
        return session.createProducer(session.createTopic(topicName));
    }
}