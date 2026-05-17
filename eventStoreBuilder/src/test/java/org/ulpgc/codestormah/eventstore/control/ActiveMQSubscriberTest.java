package org.ulpgc.codestormah.eventstore.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActiveMQSubscriberTest {

    @Test
    void shouldDispatchMessageToStore() {

        FakeStore store = new FakeStore();

        ActiveMQSubscriber subscriber =
                new ActiveMQSubscriber(
                        "tcp://localhost:61616",
                        "topic",
                        "mercadona",
                        store
                );

        // simulamos mensaje directamente
        store.dispatch("{\"id\":1}", "topic", "mercadona");

        assertEquals(1, store.count);
    }

    static class FakeStore extends FileEventStore {

        int count = 0;

        FakeStore() {
            super(System.getProperty("java.io.tmpdir"));
        }

        @Override
        public void dispatch(String event, String topic, String source) {
            count++;
        }
    }
}