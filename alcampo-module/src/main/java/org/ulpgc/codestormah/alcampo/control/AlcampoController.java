package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;

import java.util.List;

public class AlcampoController {

    private final AlcampoFeeder feeder;
    private final AlcampoSerializer serializer;

    public AlcampoController(AlcampoFeeder feeder, AlcampoSerializer serializer) {
        this.feeder = feeder;
        this.serializer = serializer;
    }

    public void execute() {
        System.out.println("Iniciando proceso de extracción de datos de Alcampo...");

        List<Product> productos = feeder.fetchProducts();

        if (productos != null && !productos.isEmpty()) {
            System.out.println("Extracción finalizada. " + productos.size() + " productos obtenidos. Procediendo a guardar...");

            serializer.serialize(productos);

            System.out.println("Proceso completado con éxito.");
        } else {
            System.out.println("No se encontraron productos o hubo un error en la extracción.");
        }
    }
}
