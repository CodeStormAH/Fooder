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


El Datamart en memoria se ha diseñado bajo los principios de baja latencia de lectura y alta concurrencia hilos, estructurado principalmente en dos almacenes independientes pero coordinados: `ProductStore` y `RecommendationStore`.

1. **Tratamiento de la Concurrencia (Thread-Safety)**

Dado que el bróker de mensajería inyecta datos constantemente a través de un hilo asíncrono (`ProductConsumer`), mientras que múltiples usuarios pueden realizar peticiones HTTP simultáneas a la API REST, el Datamart mitiga condiciones de carrera mediante estructuras no bloqueantes:

- `ConcurrentHashMap`: Utilizado en ambos almacenes para indexar los productos y las recomendaciones por clave única sin necesidad de bloquear toda la tabla en operaciones de lectura.

- `CopyOnWriteArrayList`: Utilizado en el historial de cada producto. Permite añadir nuevas actualizaciones de precios de forma segura mientras otros hilos leen la lista de manera consistente, evitando lanzar excepciones de tipo ConcurrentModificationException.

2. **Compactación Dinámica del Historial (Filtrado de Ruido)**

Los scrapers tienden a reenviar la información de un producto de manera periódica, incluso si su precio no ha cambiado, lo que saturaría la memoria con información redundante.
Para solucionarlo, el método `getProductHistory` implementa un algoritmo de compactación en tiempo de lectura que recorre la línea temporal del artículo y elimina registros consecutivos con precios idénticos. De este modo, la API solo devuelve los puntos exactos en el tiempo donde ocurrió una fluctuación real de valor o un cambio en el estado de oferta (on sale), ahorrando memoria y optimizando el payload JSON de transferencia.

3. **Precalculación Eficiente de Recomendaciones (Vistas Materializadas)**

Calcular las estadísticas del mercado (medias de precios por supermercado, producto más barato, etc.) en cada petición REST sería computacionalmente inviable ante miles de usuarios simultáneos.
Por ello, el sistema aplica el patrón de vistas materializadas a través de `RecommendationStore`:

  - Las recomendaciones no se calculan al consultar la API.

  - En su lugar, se mantienen precalculadas en un mapa estático. Cada vez que llega un evento en tiempo real, el `EventProcessor` invoca un recálculo asíncrono puntual de la categoría afectada (`recommendationStore.update`).

  - Como resultado, el endpoint de recomendaciones opera con una complejidad temporal de `O(1)`, ofreciendo respuestas instantáneas en milisegundos.

## Instrucciones de Compilación y Ejecución

Esta sección detalla los pasos necesarios para configurar el entorno, compilar el proyecto modular mediante Maven y ejecutar secuencialmente cada uno de los componentes del sistema.

**Requisitos Previos**

Antes de proceder, asegúrate de tener instaladas y configuradas las siguientes herramientas en tu sistema operativo:

**Java Development Kit (JDK)**: Versión 21 o superior.

**Apache Maven**: Versión 3.8 o superior para la gestión de dependencias y compilación.

**Apache ActiveMQ**: Servidor de mensajería (Classic) versión 5.x o superior.

**Google Chrome & ChromeDriver**: Necesarios para el correcto funcionamiento del scraper de Alcampo. Asegúrate de que la versión de chromedriver coincida exactamente con la versión de tu navegador Chrome instalado.

**Variables de Entorno**: Configura correctamente JAVA_HOME y M2_HOME en tu sistema.

**Configuración e Inicialización del Entorno**

1. **Iniciar el Bróker de Mensajería (Apache ActiveMQ)**

El sistema requiere que el servidor de mensajería esté activo antes de lanzar los módulos para permitir la creación del Topic y gestionar las suscripciones duraderas.

En Linux/macOS:

Bash
`cd /ruta/hacia/apache-activemq/bin
./activemq start`

En Windows:

DOS
`cd C:\ruta\hacia\apache-activemq\bin
activemq start`

Nota: Puedes verificar que está corriendo accediendo a la consola web en `http://localhost:8161` (credenciales por defecto: admin / admin).

2. **Preparar los Directorios de Datos**

Crea una carpeta local en tu máquina que servirá como el almacenamiento histórico de eventos (event-store). El sistema leerá los archivos con extensión .events de esta ruta al iniciar.

Bash
`mkdir event-store`


3. **Ejecución de los módulos**

Para ejecutar los respectivos módulos, hace falta introducir una serie de variables las cuales especificamos a continuación:

- Módulo `alcampoFeeder`:
  - URL de Alcampo: `"https://www.compraonline.alcampo.es/categories?source=navigation"`
  - Direcotorio del fichero de categorías: `"/Users/macbookpro/IdeaProjects/Fooder/alcampoFeeder/src/main/resources/categories.txt"` (ejemplo)
  - URL para el broker: `"tcp://localhost:61616"`
  - Nombre del topic: `"comparison.Product"`
  - Nombre de la fuente: `"alcampo"`
 
- Módulo `mercadonaFeeder`:
  - URL de Mercadona: `"https://tienda.mercadona.es/"`
  - Dirección de la API interna: `"/api/categories/"`
  - Directorio del fichero de categorías: `"C:\Users\abelc\Documents\Universidad 25-26\Segundo Cuatrimestre\DACD\Intellij\Fooder\mercadonaFeeder\src\main\resources\categories.json"`(ejemplo)
  - URL para el broker: `"tcp://localhost:61616"`
  - Nombre del topic: `"comparison.Product"`
  - Nombre de la fuente: `"mercadona"`

- Módulo `eventStoreBuilder`:
  - URL para el broker: `"tcp://localhost:61616"`
  - Nombre del topic: `"comparison.Product"`
  - Nombre de la fuente (según el feeder que quieras mandar): `"alcampo"` o `"mercadona"`
  - Nombre del paquete para guardar los eventos: `"eventstore"`
 
- Módulo `bussines-unit`:
  - URL para el broker: `"tcp://localhost:61616"`
  - Nombre del topic: `"comparison.Product"`
  - Directorio de la carpeta de eventos: `"/Users/macbookpro/IdeaProjects/Fooder/eventstore"`(ejemplo)
  - Puerto de la API: `7000`

## Ejemplos de Uso de la API REST

La Unidad de Negocio expone una API HTTP pública a través de **Javalin** para permitir la consulta del Datamart en tiempo real. Asumiendo que el servicio se ha levantado localmente en el puerto `7000`, los endpoints disponibles y sus contratos de uso son los siguientes:

### 1. Verificación del Estado del Sistema (Health Check)
Comprueba de manera rápida si el servidor HTTP está levantado y respondiendo peticiones.
* **Endpoint:** `GET /api/health`
* **Ejemplo de consulta:**
  ``` http://localhost:8080/api/health
