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
* **Ejemplo de consulta (buscador de explorador web):**
  `http://localhost:7000/api/health`
* **Respuesta:** `Business Unit running`

### 2. Listar Categorías Disponibles
Devuelve un conjunto único con todas las categorías de productos que el sistema ha procesado hasta el momento (tanto del histórico como de la capa de tiempo real).
* **Endpoint:** `GET /api/categories`
* **Ejemplo de consulta (buscador de explorador web):**
  `http://localhost:7000/api/categories`
* **Respuesta:** `["licores","refresco de naranja","cerveza","vino tinto","cava","refresco de té y sin gas","bitter","energético","refresco de limón","sidra","isotónico","vino blanco","refresco de cola","tónica","agua"]`

### 3. Obtener Productos por Categoría
Devuelve la lista completa de todos los productos pertenecientes a una categoría específica. La respuesta contiene únicamente la última actualización conocida de cada artículo y el listado se encuentra ordenado ascendentemente por precio unitario.
* **Endpoint:** `GET /api/products/{category}`
* **Ejemplo de consulta (buscador de explorador web):**
  `http://localhost:7000/api/products/licores`
* **Respuesta:** `[{"ts":"2026-05-17T13:22:26.626732Z","ss":"alcampo","id":"310fa12a-1a2a-39c5-8724-01241d69dc1e","name":"RUAVIEJA Cremosa light Licor de crema de orujo con un 60% menos de azÃºcar botella 5 cl.","normalizedName":"cremosa light licor de crema de orujo con un % menos de azÃºcar botella","brand":"RUAVIEJA","category":"licores","unitPrice":1.0,"unit":"l","quantity":0.05,"onSale":true},{"ts":"2026-05-17T13:22:29.220273Z","ss":"alcampo","id":"41000454-2ac3-32bb-8fed-145bbaf831e5","name":"PANIZO Licor de crema de orujo botella 5 cl.","normalizedName":"licor de crema de orujo botella","brand":"PANIZO","category":"licores","unitPrice":1.0,"unit":"l","quantity":0.05,"onSale":true}...`

### 4. Obtener el Producto Más Barato de una Categoría
Filtra la categoría seleccionada y extrae directamente el artículo con el menor precio del mercado actual.
* **Endpoint:** `GET /api/products/{category}/cheapest`
* **Ejemplo de consulta (buscador de explorador web):**
  `http://localhost:8080/api/products/licores/cheapest`
* **Respuesta:** `{"ts":"2026-05-17T13:22:26.626732Z","ss":"alcampo","id":"310fa12a-1a2a-39c5-8724-01241d69dc1e","name":"RUAVIEJA Cremosa light Licor de crema de orujo con un 60% menos de azÃºcar botella 5 cl.","normalizedName":"cremosa light licor de crema de orujo con un % menos de azÃºcar botella","brand":"RUAVIEJA","category":"licores","unitPrice":1.0,"unit":"l","quantity":0.05,"onSale":true}`

### 5. Obtener el Producto Más Caro de una Categoría
Extrae el artículo con el mayor precio unitario dentro de la categoría proporcionada.
* **Endpoint:** `GET /api/products/{category}/expensive`
* **Ejemplo de consulta (buscador de explorador web):**
  `http://localhost:8080/api/products/licores/expensive`
* **Respuesta:** `{"ts":"2026-05-17T13:25:34.3476878","ss":"mercadona","id":"28630","name":"Whisky escocÃ©s Black Label Johnnie Walker","normalizedName":"whisky escocÃ©s black label johnnie walker","brand":"Other","category":"licores","unitPrice":28.9,"unit":"l","quantity":0.7,"onSale":false}`

### 6. Recomendación de Compra Inteligente por Categoría
Consulta la vista materializada precalculada de recomendaciones. Este endpoint devuelve cuál es el supermercado idóneo para comprar globalmente esa categoría (basándose en la media aritmética de todos sus artículos), indica el producto más barato absoluto del momento y adjunta una comparativa de medias.
* **Endpoint:** `GET /api/recommendation/{category}`
* **Ejemplo de consulta (buscador de explorador web):**
  `http://localhost:7000/api/recommendation/licores`
* **Respuesta:** `{"category":"licores","recommendedSource":"mercadona","cheapestProductName":"RUAVIEJA Cremosa light Licor de crema de orujo con un 60% menos de azÃºcar botella 5 cl.","cheapestUnitPrice":1.0,"cheapestSource":"alcampo","comparison":{"mercadona":"9.21 € de media","alcampo":"10.31 € de media"}}`

### 7. Historial de Fluctuación de un Producto (Compactado)
Rastrea la evolución temporal de tarifas para un artículo concreto. Gracias al algoritmo de filtrado de ruido implementado en la lógica del negocio, este endpoint omite lecturas consecutivas idénticas, retornando únicamente los eventos cronológicos donde hubo un cambio real de precio o de estado de oferta.
* **Endpoint:** `GET /api/history/{source}/{id}`
* **Ejemplo de consulta (buscador de explorador web):**
  `http://localhost:7000/api/history/mercadona/21694`
* **Respuesta:** `[{"ts":"2026-05-17T13:25:34.3949769","ss":"mercadona","id":"21694","name":"Bebida preparada de vodka sabor maracuyÃ¡ Knebep Passion fruit","normalizedName":"bebida preparada de vodka sabor maracuyÃ¡ knebep passion fruit","brand":"Other","category":"licores","unitPrice":1.3,"unit":"l","quantity":0.275,"onSale":false}]` (solo hay un precio porque no se ha registrado ninguno distinto para ese producto)


### HTML implementado por nosotros

Además del HTML básico que nos proporciona Javalin hemos decicido implementar uno con el objetivo de facicilitar la búsqueda de la información útil dentro de nuestra APP. Para ello hemos creado un fichero index.HTML que genera la siguiente URL: `http://localhost:63342/Fooder/business-unit/index.html?_ijt=8ait8audveoq283uoig34nh1f0&_ij_reload=RELOAD_ON_SAVE`

A continuación mostramos capturas de las posibilidades que tiene nuestra APP desde esta ubicación:

#### 1. Consulta por categorías
Al entrar a la página, tendremos que selccionar el desplegable de categorías. Este nos mostrará todas las categorías que maneja nuestra APP. Usted podrá seleccionar la que quiera consultar y le tendrá que dar a buscar datos. Hecho esto, se mostrarán los productos y la recomendación de dicha categoría.

<img width="1680" height="1050" alt="Captura de Pantalla 2026-05-17 a las 18 00 45" src="https://github.com/user-attachments/assets/96fea511-284d-41b5-bc5b-09907b733bb1" />

#### 2. Información contenida en la categoría
Se muestran los productos ordenados por precios de menor a mayor. Al principio de la página encontrarás la recomendación de esa categoría, indicándote el mejor supermercado según la media calculada con los precios de los productos de ambas fuentes. Además, se muestra también el producto más barato y el cálculo de la media para que usted pueda comprobarlo. El producto más caro, en el caso de que lo quiera consultar, lo encontrará al final de la página. También puede hacer uso de la barra de búsqueda si está interesado en buscar un producto o marca en concreto.

<img width="1680" height="1050" alt="Captura de Pantalla 2026-05-17 a las 18 04 26" src="https://github.com/user-attachments/assets/b223ef03-98fd-4a90-b059-02808b657c74" />


#### 3. Historial de precios
Una vez visto toda la información disponible, queda por ver una función muy útil. Como pudo comprobar, cada producto tiene un botón de historial al final de su fila. Si aprieta este botón se le abrirá una pestaña al principio de la página donde podrá ver los cambios en el precio de ese producto, indicándole día y hora del cambio registrado.

<img width="1680" height="1050" alt="Captura de Pantalla 2026-05-17 a las 18 10 21" src="https://github.com/user-attachments/assets/0469d8e8-7004-4787-822b-06f2fb025c57" />


Una vez visto todo esto, ya tendría perfecto conocimiento sobre nuestra APP.


## Arquitectura del Sistema y de la Aplicación

El sistema se ha diseñado siguiendo un enfoque modular, desacoplado y guiado por eventos, estructurado bajo el paradigma de una **Arquitectura Lambda** híbrida para combinar el procesamiento de datos históricos con la ingesta en tiempo real.

### 1. Arquitectura del Sistema

La topología del sistema se divide en tres capas fundamentales que garantizan la consistencia y la baja latencia en la entrega de la información:

* **Capa Batch (Histórica):** Representada por el almacenamiento persistente inmutable (`event-store`). Al arrancar la Unidad de Negocio, el componente `EventProcessor` realiza un volcado secuencial leyendo todos los archivos con extensión `.events` del directorio local. Esto reconstruye el estado maestro del Datamart a partir de la secuencia histórica de hechos.
* **Capa Speed (Tiempo Real):** Diseñada para capturar los deltas y las actualizaciones instantáneas del mercado. Los módulos **Feeders** (Mercadona y Alcampo) extraen los datos de forma independiente y los publican inmediatamente en un Topic centralizado de **Apache ActiveMQ**. La Unidad de Negocio actúa como un consumidor permanente gracias a una **Suscripción Duradera**, procesando e integrando cada evento JSON en el Datamart en el mismo milisegundo en que se genera.
* **Capa Serving (Servicio):** Encargada de exponer las vistas materiales resultantes de la unión de la capa batch y la capa speed. Se implementa mediante un servidor HTTP embebido con **Javalin**, permitiendo a clientes externos (como interfaces Front-End o herramientas de analítica) consumir consultas complejas y precalculadas con un coste temporal mínimo.

<img width="1828" height="407" alt="image 2" src="https://github.com/user-attachments/assets/c89609b5-dc31-4dd4-bc29-540f75fd5b6e" />


### Arquitectura de la Aplicación (Estructura de Módulos)

El sistema está desarrollado como un proyecto multimódulo gestionado por Maven. Esta separación física garantiza un desacoplamiento absoluto de responsabilidades, facilitando la mantenibilidad y permitiendo que cada componente escale de forma independiente. A continuación, se detalla la organización interna de las clases y paquetes de cada módulo del repositorio:

#### 1. Módulo: alcampoFeeder

Este componente se encarga exclusivamente de la captura y extracción automatizada de datos desde la plataforma web de Alcampo utilizando técnicas de Web Scraping dinámico.

* **org.ulpgc.codestormah.alcampo**
* `Main`: Punto de entrada que inicializa las configuraciones de ruta y arranca el ciclo de vida de la extracción.


* **org.ulpgc.codestormah.alcampo.control**
* `AlcampoFeeder`: Interfaz que define los métodos abstractos y el contrato de comportamiento para cualquier alimentador de datos de Alcampo.
* `AlcampoScraperFeeder`: Implementación concreta encargada de interactuar con la API de Selenium WebDriver. Gestiona la inicialización del navegador Chrome, la aceptación del banner de cookies, el scroll automático para la carga perezosa y la lectura del DOM.
* `AlcampoController`: Componente de orquestación que controla la lógica de negocio del feeder, secuenciando el recorrido de las categorías.
* `AlcampoStore` / `DatabaseAlcampoStore`: Interfaces y abstracciones diseñadas para gestionar la persistencia y la salida del flujo de datos de los artículos procesados localmente.
* `ActiveMQAlcampoStore`: Componente encargado de abrir la conexión con Apache ActiveMQ para transformar los objetos a texto JSON y publicarlos directamente en el Topic correspondiente.


* **org.ulpgc.codestormah.alcampo.model**
* `Product`: Modelo de datos interno que representa la estructura base del producto capturado en el contexto de Alcampo.

<img width="1471" height="744" alt="alcamposi" src="https://github.com/user-attachments/assets/bd643d66-345f-410a-8e82-52300a179dce" />


#### 2. Módulo: mercadonaFeeder

Componente optimizado para la ingesta de datos a gran velocidad mediante el consumo directo de la API estructurada interna de Mercadona.

* **org.ulpgc.codestormah.mercadona**
* `Main`: Inicializa el controlador y lanza las peticiones concurrentes de ingesta.


* **org.ulpgc.codestormah.mercadona.config**
* `CategoryLoader`: Clase de utilidad encargada de leer y parsear el archivo de recursos `categories.json` para inyectar la lista de identificadores oficiales de las categorías a consultar en la API.


* **org.ulpgc.codestormah.mercadona.controller**
* `ProductFeeder` / `MercadonaFeeder`: Componentes encargados de realizar las llamadas HTTP asíncronas hacia los endpoints de Mercadona y capturar los payloads JSON nativos.
* `Controller`: Coordina el flujo principal del módulo, tomando las categorías cargadas, procesando las respuestas de la API y enviándolas al bróker.
* `ProductStore` / `DatabaseProductStore`: Gestionan el almacenamiento intermedio o la canalización de los objetos de catálogo capturados.
* `ActiveMQFactory`: Factoría encargada de centralizar, configurar y proveer conexiones eficientes hacia las colas de mensajería de ActiveMQ.


* **org.ulpgc.codestormah.mercadona.model**
* `Product`: Modelo orientado al dominio que mapea las propiedades nativas de la API de Mercadona.
* `ProductTextProcessor`: Utilidad complementaria que limpia, formatea y normaliza las cadenas de caracteres de los nombres y marcas del supermercado.


<img width="1467" height="738" alt="mercadona" src="https://github.com/user-attachments/assets/d6bc7ff7-ac9d-4fae-8135-8abc5fccc541" />


#### 3. Módulo: eventStoreBuilder

Este módulo funciona de manera autónoma y aislada de la lógica analítica. Su única responsabilidad es actuar como el sumidero inmutable de la arquitectura, garantizando que ningún evento se pierda.

* **org.ulpgc.codestormah.eventstore**
* `Main`: Levanta el demonio de escucha persistente para el almacenamiento de eventos.


* **org.ulpgc.codestormah.eventstore.control**
* `ActiveMQSubscriber`: Actúa como un oyente permanente conectado a los Topics de ActiveMQ. Recibe de forma asíncrona cada mensaje de texto JSON emitido por cualquiera de los feeders activos.
* `FileEventStore`: Componente encargado de la persistencia física en disco. Toma las cadenas JSON crudas y realiza una operación inmutable de adición de líneas (append) sobre archivos locales con extensión `.events`, conformando el registro maestro histórico del sistema.

<img width="1150" height="481" alt="EventStore" src="https://github.com/user-attachments/assets/68649138-69ea-4a40-b3d5-7301d02df80f" />

#### 4. Módulo: business-unit

Es el núcleo inteligente del sistema (Unidad de Negocio). Se encarga de procesar los datos históricos (Batch) y en tiempo real (Speed), administrar el Datamart en memoria de alta concurrencia y servir las consultas mediante una interfaz HTTP pública.

* **org.ulpgc.codestormah.business**
* `Main`: Punto de inicio del sistema central. Valida los 4 argumentos de la línea de comandos, ordena la reconstrucción del histórico a través del procesador, arranca el consumidor JMS de tiempo real y levanta la API de Javalin.


* **org.ulpgc.codestormah.business.control**
* `EventProcessor`: Componente centralizador del flujo analítico. Utiliza una instancia de `Gson` para deserializar las cadenas JSON en objetos de tipo `Product`. Contiene el método `loadHistoricalData` para escanear recursivamente carpetas mediante flujos de archivos (`Files.walk` y `Files.lines`) e inyectar el histórico al arrancar, además de `processJson` para procesar el tiempo real y forzar el recálculo analítico inmediato de la categoría afectada.
* `ProductConsumer`: Configura un cliente JMS clásico sobre ActiveMQ con un identificador de cliente fijo (`"BusinessUnit_API_Client"`). Utiliza el método `createDurableSubscriber` bajo el nombre `"BusinessUnit_Sub"`, garantizando la tolerancia a fallos: si la unidad de negocio se detiene, el bróker almacena los eventos acumulados de la capa speed hasta que el consumidor se reactive.
* `ProductStore`: Gestiona el Datamart maestro indexado por un `ConcurrentHashMap` seguro para hilos, cuyos valores son listas de tipo `CopyOnWriteArrayList` para evitar condiciones de carrera. Expone métodos de consulta optimizados como `getProductsByCategory` (que extrae la última actualización de cada producto y la devuelve ordenada ascendentemente por precio a través de un comparador encadenado), `getCheapestProduct`, `getMostExpensiveProduct` y el filtrado del historial compactado sin ruido en `getProductHistory`.
* `RecommendationStore`: Mantiene la vista materializada precalculada de las recomendaciones de compra en un `ConcurrentHashMap`. Al invocar su método `update`, agrupa dinámicamente los productos de la categoría por su origen (`Product::getSs`), calcula las estadísticas de precios con `DoubleSummaryStatistics` para obtener las medias aritméticas y determina de forma asíncrona qué supermercado ofrece el mayor ahorro global.


* **org.ulpgc.codestormah.business.model**
* `Product`: Objeto de dominio unificado que cohesiona los catálogos unificando propiedades críticas (marca, categoría, precio unitario, timestamps y estados de oferta).
* `Recommendation`: Modelo que estructura la respuesta analítica precalculada final (supermercado recomendado, producto más barato, precio unitario y la comparativa de medias del mercado).


* **org.ulpgc.codestormah.business.view**
* `ApiController`: Define e inicializa el servidor HTTP embebido utilizando **Javalin 5**. Configura el plugin de CORS nativo mediante `config.plugins.enableCors` otorgando permisos de acceso a cualquier cliente web externo (`anyHost()`). Mapea de forma limpia los verbos HTTP `GET` hacia las funciones atómicas del `ProductStore` y del `RecommendationStore`, gestionando los códigos de estado HTTP (como el `404 Not Found`).

<img width="1303" height="878" alt="Bussines-unit" src="https://github.com/user-attachments/assets/7f002b17-e0b8-4153-a99b-89aa3808763b" />

