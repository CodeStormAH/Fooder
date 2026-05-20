package org.ulpgc.codestormah.mercadona.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.ulpgc.codestormah.mercadona.model.Product;

import javax.jms.*;
import java.time.LocalDateTime;
import java.util.List;

public class ActiveMQFactory {

    public static ProductStore createStore(String url, String topic, String source) throws JMSException {
        Connection conn = createConnection(url);
        Session session = createSession(conn);
        return new JmsProductStore(createProducer(session, topic), session, source);
    }

    private static Connection createConnection(String brokerUrl) throws JMSException {
        Connection conn = new ActiveMQConnectionFactory(brokerUrl).createConnection();
        conn.start();
        return conn;
    }

    private static Session createSession(Connection conn) throws JMSException {
        return conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    private static MessageProducer createProducer(Session session, String topic) throws JMSException {
        return session.createProducer(session.createTopic(topic));
    }

    private static class JmsProductStore implements ProductStore {

        private final MessageProducer producer;
        private final Session session;
        private final String source;
        private final ObjectMapper mapper = createMapper();

        public JmsProductStore(MessageProducer p, Session s, String src) {
            this.producer = p;
            this.session = s;
            this.source = src;
        }

        @Override
        public void save(List<Product> products) {
            products.forEach(this::safeSend);
        }

        private void safeSend(Product product) {
            try {
                send(product);
            } catch (Exception e) {
                System.err.println("Error sending product: " + e.getMessage());
            }
        }

        private void send(Product p) throws Exception {
            String json = mapper.writeValueAsString(toEvent(p));
            producer.send(session.createTextMessage(json));
        }

        private ProductEvent toEvent(Product p) {
            return new ProductEvent(LocalDateTime.now(), source, p.id(), p.name(),
                    p.normalizedName(), p.brand(), p.category(), p.unitPrice(),
                    p.unit(), p.quantity(), p.isOnSale());
        }

        private static ObjectMapper createMapper() {
            return new ObjectMapper().findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    private record ProductEvent(
            LocalDateTime ts, String ss, String id, String name, String normalizedName,
            String brand, String category, double unitPrice, String unit, double quantity, boolean isOnSale
    ) {}
}