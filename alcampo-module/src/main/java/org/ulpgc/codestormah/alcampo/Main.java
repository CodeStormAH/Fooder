package org.ulpgc.codestormah.alcampo;

import org.ulpgc.codestormah.alcampo.control.*;

import java.io.File;

public class Main {
    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("❌ Error: Faltan parámetros de configuración.");
            return;
        }

        String urlObjetivo = args[0];

        File dbFile = new File(args[1]);

        System.out.println("Iniciando programa con los siguientes parámetros:");
        System.out.println("URL: " + urlObjetivo);
        System.out.println("Archivo BBDD: " + dbFile.getAbsolutePath());

        AlcampoFeeder feeder = new AlcampoScraperFeeder(urlObjetivo);
        AlcampoStore store = new DatabaseAlcampoStore(dbFile);

        AlcampoController controller = new AlcampoController(feeder, store);
        controller.execute();
    }
}