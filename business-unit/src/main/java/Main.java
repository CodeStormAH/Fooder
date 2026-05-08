import org.ulpgc.codestormah.business.api.ApiController;
import org.ulpgc.codestormah.business.broker.ProductConsumer;
import org.ulpgc.codestormah.business.control.EventProcessor;
import org.ulpgc.codestormah.business.datamart.ProductStore;

public class Main {
    public static void main(String[] args) {
        // Validación de seguridad para evitar errores al arrancar
        if (args.length < 4) {
            System.err.println("Error: Parámetros insuficientes.");
            System.err.println("Uso: <BrokerURL> <TopicName> <EventStorePath> <ApiPort>");
            System.exit(1);
        }

        // Configuración (idealmente se pasaría por args[])
        String brokerUrl = args[0];
        String topicName = args[1]; // El topic que decidisteis
        String eventStorePath = args[2];
        int apiPort = Integer.parseInt(args[3]);

        // 1. Inicializar el Datamart y el Procesador
        ProductStore productStore = new ProductStore();
        EventProcessor processor = new EventProcessor(productStore);

        // 2. Cargar histórico de datos (Batch Layer)
        System.out.println("⏳ Cargando histórico desde: " + eventStorePath);
        processor.loadHistoricalData(eventStorePath);

        // 3. Iniciar escucha en tiempo real (Speed Layer)
        ProductConsumer consumer = new ProductConsumer(brokerUrl, topicName, processor);
        consumer.start();

        // 4. Iniciar la interfaz REST (Serving Layer)
        ApiController api = new ApiController(productStore, apiPort);
        api.start();

        System.out.println("✅ Business Unit iniciada con éxito. Arquitectura Lambda activa.");
    }
}
