package org.ulpgc.codestormah.alcampo;

import org.ulpgc.codestormah.alcampo.control.*;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            printUsageError();
            return;
        }

        String targetUrl = args[0];
        File databaseFile = new File(args[1]);
        String categoriesFilePath = args[2];

        startApplication(targetUrl, databaseFile, categoriesFilePath);
    }

    private static void startApplication(String url, File dbFile, String path) {
        System.out.println("Starting program...");

        AlcampoFeeder feeder = new AlcampoScraperFeeder(url, path);
        AlcampoStore store = new DatabaseAlcampoStore(dbFile);
        AlcampoController controller = new AlcampoController(feeder, store);

        controller.execute();
    }

    private static void printUsageError() {
        System.err.println("❌ Error: Missing configuration parameters.");
        System.err.println("Usage: <URL> <Database_File> <Categories_File>");
    }
}