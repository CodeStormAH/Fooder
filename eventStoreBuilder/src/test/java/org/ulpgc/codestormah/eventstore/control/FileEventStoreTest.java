package org.ulpgc.codestormah.eventstore.control;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileEventStoreTest {

    @Test
    void shouldWriteEventToFile() throws Exception {

        Path tempDir = Files.createTempDirectory("eventstore");

        FileEventStore store = new FileEventStore(tempDir.toString());

        String event = "{\"id\":1,\"name\":\"test\"}";

        store.dispatch(event, "topic-test", "mercadona");

        Path topicDir = tempDir.resolve("topic-test").resolve("mercadona");

        assertTrue(Files.exists(topicDir));

        Optional<Path> file = Files.list(topicDir).findFirst();

        assertTrue(file.isPresent());

        String content = Files.readString(file.get());

        assertTrue(content.contains(event));
    }

    @Test
    void shouldAppendEventsToSameFile() throws Exception {

        Path tempDir = Files.createTempDirectory("eventstore");

        FileEventStore store = new FileEventStore(tempDir.toString());

        store.dispatch("event1", "topic", "source");
        store.dispatch("event2", "topic", "source");

        Path topicDir = tempDir.resolve("topic").resolve("source");

        Optional<Path> file = Files.list(topicDir).findFirst();

        assertTrue(file.isPresent());

        String content = Files.readString(file.get());

        assertTrue(content.contains("event1"));
        assertTrue(content.contains("event2"));
    }
}