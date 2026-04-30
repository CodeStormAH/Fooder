package org.ulpgc.codestormah.mercadona.controller;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class ActiveMQFactory {

    public static EventPublisher createPublisher(String brokerUrl, String topicName, String source) throws JMSException {

        Connection connection = createConnection(brokerUrl);
        Session session = createSession(connection);
        MessageProducer producer = createProducer(session, topicName);

        return new EventPublisher(session, producer, source);
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