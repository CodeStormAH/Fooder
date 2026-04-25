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

    public ActiveMQSubscriber(String brokerUrl, String topicName, String source, FileEventStore store) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.source = source;
        this.store = store;
    }

    public void start() throws JMSException {
        Connection connection = createConnection();
        // Paso 2: El ClientID es obligatorio para que sea duradera
        connection.setClientID("StoreBuilder_" + source + "_" + topicName);
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(topicName);

        // Paso 2: Suscripción Duradera
        MessageConsumer consumer = session.createDurableSubscriber(topic, "DurableSubscription_" + source);

        consumer.setMessageListener(this::onMessage);
    }

    private void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();

                // Paso 3: Deserializar a JsonObject para validar formato
                JsonObject jsonObject = gson.fromJson(text, JsonObject.class);

                // Enviar al gestor de archivos
                store.dispatch(jsonObject.toString(), topicName, source);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private Connection createConnection() throws JMSException {
        return new ActiveMQConnectionFactory(brokerUrl).createConnection();
    }
}
