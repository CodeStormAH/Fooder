package org.ulpgc.codestormah.eventstore.control;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileEventStoreTest {

    @Test
    void shouldWriteEventToFile() throws Exception {
        Path root = Files.createTempDirectory("eventstore");
        String event = "{\"id\":1,\"name\":\"test\"}";
        new FileEventStore(root.toString()).dispatch(event, "topic-test", "mercadona");
        assertTrue(Files.exists(root.resolve("topic-test").resolve("mercadona")));
        assertTrue(readFirstFile(root, "topic-test", "mercadona").contains(event));
    }

    private String readFirstFile(Path root, String topic, String source) throws Exception {
        Path dir = root.resolve(topic).resolve(source);
        return Files.readString(Files.list(dir).findFirst().orElseThrow());
    }

    @Test
    void shouldAppendEventsToSameFile() throws Exception {
        Path root = Files.createTempDirectory("eventstore");
        FileEventStore store = new FileEventStore(root.toString());
        store.dispatch("event1", "topic", "source");
        store.dispatch("event2", "topic", "source");
        assertContainsAll(readFirstFile(root, "topic", "source"), "event1", "event2");
    }

    private void assertContainsAll(String content, String e1, String e2) {
        assertTrue(content.contains(e1) && content.contains(e2));
    }
}