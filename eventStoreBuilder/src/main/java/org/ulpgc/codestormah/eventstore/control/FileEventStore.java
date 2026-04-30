package org.ulpgc.codestormah.eventstore.control;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FileEventStore {
    private final String root;

    public FileEventStore(String root) {
        this.root = root; // Ahora root será directamente "eventstore"
    }

    public void dispatch(String event, String topic, String source) {
        try {
            // Paso 4 ajustado: {root}/{topic}/{source}
            Path path = Paths.get(root, topic, source);
            Files.createDirectories(path);

            File file = path.resolve(getFileName()).toFile();

            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                writer.println(event);
            }
        } catch (IOException e) {
            System.err.println("Error writing to EventStore: " + e.getMessage());
        }
    }

    private String getFileName() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".events";
    }
}
