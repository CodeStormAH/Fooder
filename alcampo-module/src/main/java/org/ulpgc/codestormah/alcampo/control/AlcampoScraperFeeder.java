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

    // Constructor que recibe la URL desde el Main
    public AlcampoScraperFeeder(String url) {
        this.url = url;
    }

    @Override
    public List<Product> fetchProducts() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        WebDriver driver = new ChromeDriver(options);

        Map<String, Product> productosExtraidos = new HashMap<>();

        try {
            driver.get(this.url);
            Thread.sleep(5000);

            try {
                WebElement btnCookies = driver.findElement(By.id("onetrust-accept-btn-handler"));
                btnCookies.click();
                Thread.sleep(1500);
            } catch (Exception ignored) {
            }

            JavascriptExecutor js = (JavascriptExecutor) driver;
            int lastCount = 0;
            int sameCountTimes = 0;

            System.out.println("Iniciando modo 'caza en movimiento' (Virtual Scrolling)...");

            while (sameCountTimes < 5) {

                List<WebElement> productosWeb = driver.findElements(
                        By.cssSelector("[data-retailer-anchor='product-list'] div[data-test^='fop-wrapper']")
                );

                for (WebElement producto : productosWeb) {
                    try {
                        String nombre = producto.findElement(By.cssSelector("[data-test='fop-title']")).getText();

                        if (!productosExtraidos.containsKey(nombre) && !nombre.isEmpty()) {
                            String precio = producto.findElement(By.cssSelector("[data-test='fop-price']")).getText();
                            String peso = producto.findElement(By.cssSelector("[data-test='fop-size']")).getText();

                            double precioNum = parsearPrecio(precio);
                            double cantidadNum = parsearCantidad(peso);
                            String unidadStr = parsearUnidad(peso);
                            String marca = extraerMarca(nombre);
                            String id = UUID.randomUUID().toString();

                            Product p = new Product(
                                    id, nombre, nombre.toLowerCase(), marca, "categoria_general",
                                    precioNum, unidadStr, cantidadNum, false
                            );

                            productosExtraidos.put(nombre, p);
                        }
                    } catch (Exception ignored) {
                    }
                }

                js.executeScript("window.scrollBy(0, 800);");
                Thread.sleep(1500);

                if (productosExtraidos.size() == lastCount) {
                    sameCountTimes++;
                } else {
                    sameCountTimes = 0;
                    lastCount = productosExtraidos.size();
                    System.out.println("Extrayendo... Total acumulado único: " + lastCount);
                }
            }

            System.out.println("Scroll finalizado. No hay productos nuevos a la vista.");

        } catch (Exception e) {
            e.printStackTrace();
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
