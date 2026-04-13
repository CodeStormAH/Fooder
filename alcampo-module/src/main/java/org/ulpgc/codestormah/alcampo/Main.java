package org.ulpgc.codestormah.alcampo;

import org.ulpgc.codestormah.alcampo.control.*;

public class Main {
    public static void main(String[] args) {

        AlcampoFeeder feeder = new AlcampoScraperFeeder();
        AlcampoSerializer serializer = new DatabaseAlcampoSerializer();

        AlcampoController controller = new AlcampoController(feeder, serializer);

        controller.execute();
    }
}
