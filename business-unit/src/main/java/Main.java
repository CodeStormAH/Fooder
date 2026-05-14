import org.ulpgc.codestormah.business.view.ApiController;
import org.ulpgc.codestormah.business.control.ProductConsumer;
import org.ulpgc.codestormah.business.control.EventProcessor;
import org.ulpgc.codestormah.business.control.ProductStore;

public class Main {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Error: Parámetros insuficientes.");
            System.err.println("Uso: <BrokerURL> <TopicName> <EventStorePath> <ApiPort>");
            System.exit(1);
        }

        String brokerUrl = args[0];
        String topicName = args[1];
        String eventStorePath = args[2];
        int apiPort = Integer.parseInt(args[3]);

        ProductStore productStore = new ProductStore();
        EventProcessor processor = new EventProcessor(productStore);

        System.out.println("⏳ Cargando histórico desde: " + eventStorePath);
        processor.loadHistoricalData(eventStorePath);

        ProductConsumer consumer = new ProductConsumer(brokerUrl, topicName, processor);
        consumer.start();

        ApiController api = new ApiController(productStore, apiPort);
        api.start();

        System.out.println("✅ Business Unit iniciada con éxito. Arquitectura Lambda activa.");
    }
}
