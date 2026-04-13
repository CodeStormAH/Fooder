package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;

import java.util.List;

public class AlcampoController {

    private final AlcampoFeeder feeder;
    private final AlcampoSerializer serializer;

    // Inyección de dependencias a través del constructor
    public AlcampoController(AlcampoFeeder feeder, AlcampoSerializer serializer) {
        this.feeder = feeder;
        this.serializer = serializer;
    }

    // Coordina el flujo general
    public void execute() {
        System.out.println("Iniciando proceso de extracción de datos de Alcampo...");

        // 1. Obtener datos (Extracción y Transformación)
        List<Product> productos = feeder.fetchProducts();

        if (productos != null && !productos.isEmpty()) {
            System.out.println("Extracción finalizada. " + productos.size() + " productos obtenidos. Procediendo a guardar...");

            // 2. Persistir datos (Guardado)
            serializer.serialize(productos);

            System.out.println("Proceso completado con éxito.");
        } else {
            System.out.println("No se encontraron productos o hubo un error en la extracción.");
        }
    }
}
