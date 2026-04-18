package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AlcampoScraperFeeder implements AlcampoFeeder {
    private final String url;

    public AlcampoScraperFeeder(String url) {
        this.url = url;
    }

    @Override
    public List<Product> fetchProducts() {
        ChromeOptions options = new ChromeOptions();

        // --- OPTIMIZACIONES DE VELOCIDAD ---
        options.addArguments("--headless=new"); // Modo invisible (comenta esta línea si quieres ver qué hace)
        options.addArguments("--start-maximized");
        options.addArguments("--blink-settings=imagesEnabled=false"); // DESACTIVA IMÁGENES
        options.addArguments("--disable-blink-features=AutomationControlled"); // Menos detectable

        WebDriver driver = new ChromeDriver(options);
        Map<String, Product> productosExtraidos = new HashMap<>();

        try {
            driver.get(this.url);
            Thread.sleep(4000);

            // Aceptar cookies rápido
            try {
                driver.findElement(By.id("onetrust-accept-btn-handler")).click();
            } catch (Exception e) {}

            JavascriptExecutor js = (JavascriptExecutor) driver;
            int lastCount = 0;
            int sameCountTimes = 0;

            System.out.println("Iniciando extracción optimizada (sin imágenes)...");

            while (sameCountTimes < 5) {
                List<WebElement> productosWeb = driver.findElements(
                        By.cssSelector("[data-retailer-anchor='product-list'] div[data-test^='fop-wrapper']")
                );

                for (WebElement producto : productosWeb) {
                    try {
                        String nombre = producto.findElement(By.cssSelector("[data-test='fop-title']")).getText();
                        if (!productosExtraidos.containsKey(nombre) && !nombre.isEmpty()) {
                            // ... (aquí va el resto de tu lógica de extracción de precio, peso, etc.)
                            // Mantén el código igual que lo tenías dentro de este IF
                            String precio = producto.findElement(By.cssSelector("[data-test='fop-price']")).getText();
                            String peso = producto.findElement(By.cssSelector("[data-test='fop-size']")).getText();
                            double precioNum = parsearPrecio(precio);
                            double cantidadNum = parsearCantidad(peso);
                            String unidadStr = parsearUnidad(peso);
                            String marca = extraerMarca(nombre);
                            String id = UUID.randomUUID().toString();

                            productosExtraidos.put(nombre, new Product(id, nombre, nombre.toLowerCase(), marca, "categoria_general", precioNum, unidadStr, cantidadNum, false));
                        }
                    } catch (Exception e) {}
                }

                // Scroll un poco más agresivo y espera más corta
                js.executeScript("window.scrollBy(0, 1000);");
                Thread.sleep(800); // Bajamos de 1500ms a 800ms

                if (productosExtraidos.size() == lastCount) {
                    sameCountTimes++;
                } else {
                    sameCountTimes = 0;
                    lastCount = productosExtraidos.size();
                    if(lastCount % 100 == 0) System.out.println("Acumulado: " + lastCount + " productos...");
                }
            }
        } catch (Exception e) {
            System.out.println("Aviso: El scroll se detuvo, pero se conservan los datos extraídos.");
        } finally {
            driver.quit();
        }
        return new ArrayList<>(productosExtraidos.values());
    }

    private double parsearPrecio(String t) {
        if (t == null || t.isEmpty()) return 0;
        return Double.parseDouble(t.replace("€", "").replace(",", ".").replaceAll("[^0-9.]", ""));
    }

    private double parsearCantidad(String t) {
        if (t == null) return 1;
        String n = t.replaceAll("[^0-9]", "");
        return n.isEmpty() ? 1 : Double.parseDouble(n);
    }

    private String parsearUnidad(String t) {
        return t == null ? "" : t.replaceAll("[0-9 ]", "");
    }

    private String extraerMarca(String nombre) {
        return nombre.isEmpty() ? "Desconocida" : nombre.split(" ")[0];
    }
}
