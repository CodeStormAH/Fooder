package org.ulpgc.codestormah.eventstore.control;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FileEventStore {
    private final String root;

    public FileEventStore(String root) {
        this.root = root;
    }

    public void dispatch(String event, String topic, String source) {
        try {
            Path path = buildPath(topic, source);
            Files.createDirectories(path);
            File file = path.resolve(getFileName()).toFile();

            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                writer.println(event); // Paso 5: Un evento JSON por línea
            }
        } catch (IOException e) {
            System.err.println("Error writing to Datalake: " + e.getMessage());
        }
    }

    private Path buildPath(String topic, String source) {
        // Paso 4: eventstore/{topic}/{source}
        return Paths.get(root, "eventstore", topic, source);
    }

    private String getFileName() {
        // Paso 4: {YYYYMMDD}.events
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".events";
    }
}
