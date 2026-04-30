package org.ulpgc.codestormah.eventstore.control;

import java.io.*;
import java.nio.file.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FileEventStore {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Path root;
    private final Clock clock;

    public FileEventStore(String root) {
        this.root = Paths.get(root);
        this.clock = Clock.systemUTC();
    }

    public void dispatch(String event, String topic, String source) {
        try {
            Path directory = buildDirectory(topic, source);
            Files.createDirectories(directory);

            Path file = directory.resolve(buildFileName());

            appendEvent(file, event);

        } catch (IOException e) {
            logError(e);
        }
    }

    private Path buildDirectory(String topic, String source) {
        return root.resolve(topic).resolve(source);
    }

    private String buildFileName() {
        return LocalDate.now(clock).format(FORMATTER) + ".events";
    }

    private void appendEvent(Path file, String event) throws IOException {
        Files.writeString(
                file,
                event + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    private void logError(IOException e) {
        System.err.println("Error writing to EventStore: " + e.getMessage());
    }
}