package org.ulpgc.codestormah.eventstore.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FileEventStore {

    private static final Logger logger = LoggerFactory.getLogger(FileEventStore.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final Path root;
    private final Clock clock;

    public FileEventStore(String root) {
        this.root = Paths.get(root);
        this.clock = Clock.systemUTC();
    }

    public void dispatch(String event, String topic, String source) {
        try {
            processDispatch(event, topic, source);
        } catch (IOException e) {
            logger.error("Error writing event to store", e);
        }
    }

    private void processDispatch(String e, String t, String s) throws IOException {
        Path directory = buildDirectory(t, s);
        Files.createDirectories(directory);
        appendEvent(directory.resolve(buildFileName()), e);
    }

    private Path buildDirectory(String topic, String source) {
        return root.resolve(topic).resolve(source);
    }

    private String buildFileName() {
        return LocalDate.now(clock).format(FORMATTER) + ".events";
    }

    private void appendEvent(Path file, String event) throws IOException {
        Files.writeString(file, event + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}