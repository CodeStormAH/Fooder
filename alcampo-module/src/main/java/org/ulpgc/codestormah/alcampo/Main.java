package org.ulpgc.codestormah.alcampo;

import org.ulpgc.codestormah.alcampo.control.*;

import java.io.File;

public class Main {
    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("❌ Error: Faltan parámetros de configuración.");
            // Fíjate que el ejemplo ahora NO lleva 'jdbc:sqlite:'
            System.out.println("Uso correcto: java Main <url_alcampo> <nombre_archivo_db>");
            System.out.println("Ejemplo: java Main \"https://www.compraonline.alcampo.es/categories?source=navigation\" \"alcampo.db\"");
            return;
        }

        String urlObjetivo = args[0];

        // Transformamos el texto en un objeto File
        File dbFile = new File(args[1]);

        System.out.println("Iniciando programa con los siguientes parámetros:");
        System.out.println("URL: " + urlObjetivo);
        System.out.println("Archivo BBDD: " + dbFile.getAbsolutePath());

        AlcampoFeeder feeder = new AlcampoScraperFeeder(urlObjetivo);
        // Le pasamos el objeto File, tal y como pide el profesor
        AlcampoSerializer serializer = new DatabaseAlcampoSerializer(dbFile);

        AlcampoController controller = new AlcampoController(feeder, serializer);
        controller.execute();
    }
}