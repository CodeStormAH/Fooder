# Fooder APP 

## Descripción del proyecto y propuesta de valor 

En el escenario económico actual, marcado por la inflación y la constante fluctuación de costes en la cesta de la compra, los consumidores sufren una falta de transparencia al comparar los precios de los bienes de primera necesidad. Cada gran superficie gestiona sus catálogos, ofertas y variaciones de precio de forma aislada, lo que imposibilita al usuario realizar una compra óptima sin invertir demasiado tiempo.

Este proyecto nace para solucionar esta problemática mediante una aplicación centralizada de **monitorización y análisis de precios de supermercados**. La plataforma se encarga de recopilar de manera continua la información de los productos y sus tarifas (tanto registros pasados como actualizaciones en el momento en que ocurren), unificando los catálogos bajo un mismo formato para ofrecer un servicio de consulta inteligente, limpio y accesible.


La **propuesta de valor** de la aplicación se sostiene sobre tres pilares orientados a transformar datos dispersos en decisiones de ahorro eficientes:

- **Centralización y Transparencia Multicanal**: Elimina la opacidad del mercado al unificar los productos de diferentes cadenas competidoras (como Alcampo y otras grandes superficies) en una única base de conocimiento común, facilitando la comparación directa.

- **Recomendación Automatizada del Mayor Ahorro**: El sistema no se limita a listar datos; procesa estadísticamente el coste de los artículos para recomendar de forma activa qué supermercado ofrece las mejores tarifas medias para una categoría concreta (por ejemplo, "Aceites" o "Lácteos"), identificando además el producto más barato de forma absoluta.

- **Historial de Evolución Depurado**: Rastrea la línea de tiempo de cada artículo eliminando de forma inteligente las lecturas repetidas. El usuario puede visualizar con total claridad los momentos exactos en los que se produjo una fluctuación real de valor o una oferta especial, permitiendo analizar tendencias de precios a lo largo del tiempo.



## Justificación de la elección de APIs y estructura del datamart

El diseño del sistema ha requerido tomar decisiones tecnológicas tanto en la fase de captura de datos (Feeders) como en el desarrollo del núcleo de negocio (Business Unit).

1. **APIs de Captura y Extracción de Datos (Módulo Feeders)**

Cada plataforma de supermercado presenta restricciones técnicas distintas, lo que obligó a diversificar las estrategias de obtención de datos:

- **API REST de Mercadona (Enfoque de Consumo Directo)**: Para este origen, se optó por el consumo directo de su API de datos interna (descubierta mediante ingeniería inversa del tráfico de red). Al devolver payloads en formato JSON perfectamente estructurados, permite una captura limpia, masiva, de extrema ligereza y con una latencia mínima, sin necesidad de renderizar interfaz gráfica.

- **Web Scraping en Alcampo via Selenium WebDriver (Enfoque de Automatización)**: A diferencia de Mercadona, Alcampo no expone un acceso público amigable o estático a sus datos de inventario. Por ello, se justificó la elección de la API de Selenium WebDriver para simular la navegación de un usuario real. Esto permite:

  - Ejecutar el JavaScript dinámico de la página para cargar los productos.

  - Interactuar y saltar barreras físicas como los banners de aceptación de cookies.

  - Realizar desplazamientos controlados (scrolling) para forzar la carga perezosa (lazy loading) de los productos en pantalla antes de capturar el DOM.

2. **APIs de Infraestructura y Desarrollo (Módulo Business Unit)**

Para el procesamiento y la exposición del Datamart, se seleccionaron componentes con nula sobrecarga de memoria, priorizando el rendimiento bruto sobre frameworks pesados tradicionales:

- **Javalin 5**: Elegido por ser un framework micro-web extremadamente rápido que corre sobre Jetty embebido. Permite levantar endpoints REST de forma explícita e imperativa. Además, incluye la configuración nativa de CORS (`config.plugins.enableCors`), crucial para que cualquier aplicación Front-End externa pueda consumir los datos sin bloqueos de seguridad del navegador.

- **Apache ActiveMQ** (JMS API): Utilizado para garantizar el desacoplamiento de la Capa Speed. Se configura mediante una Suscripción Duradera (`createDurableSubscriber`), asegurando la tolerancia a fallos: si la Unidad de Negocio se apaga, ActiveMQ retiene los eventos y los entrega al reiniciar, evitando pérdidas de datos.

- **Gson** (Google): Utilizado como motor de parseo por su alta velocidad para transformar instantáneamente los JSON crudos recibidos en objetos de tipo Product.

