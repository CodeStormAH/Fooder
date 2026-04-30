package org.ulpgc.codestormah.mercadona.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ulpgc.codestormah.mercadona.model.Event;

import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class EventPublisher {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final Session session;
    private final MessageProducer producer;
    private final ObjectMapper mapper;
    private final String source;

    public EventPublisher(Session session, MessageProducer producer, String source) {
        this.session = session;
        this.producer = producer;
        this.mapper = new ObjectMapper();
        this.source = source;
    }

    public void publish(String topic, Object payload) {
        executeWithRetry(() -> sendEvent(payload), topic);
    }

    private void sendEvent(Object payload) throws Exception {
        Event event = createEvent(payload);
        String json = serialize(event);
        TextMessage message = createMessage(json);
        producer.send(message);
    }

    private Event createEvent(Object payload) {
        return new Event(
                System.currentTimeMillis(),
                source,
                payload
        );
    }

    private String serialize(Event event) throws Exception {
        return mapper.writeValueAsString(event);
    }

    private TextMessage createMessage(String json) throws Exception {
        return session.createTextMessage(json);
    }

    private void executeWithRetry(ThrowingAction action, String topic) {
        int attempt = 0;

        while (true) {
            try {
                action.execute();
                return;
            } catch (Exception e) {
                attempt++;

                if (hasReachedMaxRetries(attempt)) {
                    throw buildException(topic, e);
                }

                logRetry(attempt, topic);
                sleep();
            }
        }
    }

    private boolean hasReachedMaxRetries(int attempt) {
        return attempt >= MAX_RETRIES;
    }

    private RuntimeException buildException(String topic, Exception cause) {
        return new RuntimeException("Publish failed after retries: " + topic, cause);
    }

    private void logRetry(int attempt, String topic) {
        System.err.println("Retry " + attempt + " for topic: " + topic);
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void execute() throws Exception;
    }
}