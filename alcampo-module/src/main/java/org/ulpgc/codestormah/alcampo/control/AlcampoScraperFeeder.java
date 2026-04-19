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
    private static final int TESTING_LIMIT = 200;

    public AlcampoScraperFeeder(String url) {
        this.url = url;
    }

    @Override
    public List<Product> fetchProducts() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");

        WebDriver driver = new ChromeDriver(options);
        Map<String, Product> extractedProducts = new HashMap<>();

        try {
            driver.get(this.url);
            Thread.sleep(4000);

            try {
                driver.findElement(By.id("onetrust-accept-btn-handler")).click();
            } catch (Exception e) {}

            JavascriptExecutor js = (JavascriptExecutor) driver;
            int lastCount = 0;
            int sameCountTimes = 0;

            System.out.println("Extrayendo datos reales de Alcampo...");

            while (sameCountTimes < 5 && extractedProducts.size() < TESTING_LIMIT) {
                // Selector del contenedor principal de cada producto
                List<WebElement> webElements = driver.findElements(
                        By.cssSelector("[data-retailer-anchor='product-list'] div[data-test^='fop-wrapper']")
                );

                for (WebElement element : webElements) {
                    if (extractedProducts.size() >= TESTING_LIMIT) break;

                    try {
                        String name = element.findElement(By.cssSelector("[data-test='fop-title']")).getText();

                        if (!extractedProducts.containsKey(name) && !name.isEmpty()) {

                            // 1. PRECIO TOTAL (Ej: 1,80 €)
                            String priceText = element.findElement(By.cssSelector("[data-test='fop-price']")).getText();
                            double priceValue = parseNumericValue(priceText);

                            // 2. PRECIO POR UNIDAD (Ej: 0,20 € por litro)
                            double unitPriceValue = 0.0;
                            try {
                                String uPriceText = element.findElement(By.cssSelector("[data-test='fop-price-per-unit']")).getText();
                                unitPriceValue = parseNumericValue(uPriceText);
                            } catch (Exception e) {}

                            // 3. CANTIDAD REAL (Evitamos los IDs internos de 8 dígitos)
                            String sizeText = "";
                            try {
                                // Forzamos la lectura del texto dentro del span para evitar atributos ocultos
                                sizeText = element.findElement(By.cssSelector("[data-test='fop-size'] span")).getText();
                            } catch (Exception e) {
                                sizeText = element.findElement(By.cssSelector("[data-test='fop-size']")).getText();
                            }
                            double quantityValue = parseQuantityValue(sizeText);
                            String unitValue = parseUnitLabel(sizeText);

                            // 4. MARCA: Filtramos para que no se cuelen letras de la descripción
                            String brandValue = extractCleanBrand(name);

                            // 5. OFERTA: Detección visual del banner
                            boolean isOnSale = !element.findElements(By.cssSelector(".promotion-container")).isEmpty();

                            extractedProducts.put(name, new Product(
                                    UUID.randomUUID().toString(),
                                    name,
                                    name.toLowerCase(),
                                    brandValue,
                                    "Bebidas", // Mantenemos la categoría fija
                                    priceValue,
                                    unitPriceValue,
                                    unitValue,
                                    quantityValue,
                                    isOnSale
                            ));
                        }
                    } catch (Exception e) {}
                }

                if (extractedProducts.size() >= TESTING_LIMIT) break;

                js.executeScript("window.scrollBy(0, 1000);");
                Thread.sleep(1500);

                if (extractedProducts.size() == lastCount) {
                    sameCountTimes++;
                } else {
                    sameCountTimes = 0;
                    lastCount = extractedProducts.size();
                    System.out.println("Capturados: " + lastCount + "/" + TESTING_LIMIT);
                }
            }
        } catch (Exception e) {
            System.err.println("Error en el scraper: " + e.getMessage());
        } finally {
            driver.quit();
        }
        return new ArrayList<>(extractedProducts.values());
    }

    // --- MÉTODOS DE LIMPIEZA DE DATOS ---

    private double parseNumericValue(String text) {
        if (text == null || text.isEmpty()) return 0;
        // Limpiamos todo lo que no sea número o coma decimal
        String cleaned = text.replaceAll("[^0-9,]", "").replace(",", ".");
        try {
            return Double.parseDouble(cleaned);
        } catch (Exception e) { return 0; }
    }

    private double parseQuantityValue(String text) {
        if (text == null || text.isEmpty()) return 1.0;
        // Extraemos el número del texto "9000ml" o "1,5 L"
        String cleaned = text.replaceAll("[^0-9,.]", "").replace(",", ".");
        try {
            double val = Double.parseDouble(cleaned);
            // Normalización: si son mililitros altos, pasamos a litros
            if (text.toLowerCase().contains("ml") && val >= 100) return val / 1000.0;
            return val;
        } catch (Exception e) { return 1.0; }
    }

    private String parseUnitLabel(String text) {
        if (text == null) return "ud";
        String lower = text.toLowerCase();
        if (lower.contains("ml") || lower.contains(" l") || lower.contains("litro")) return "L";
        if (lower.contains("kg") || lower.contains("kilo") || lower.contains(" g")) return "kg";
        return "ud";
    }

    private String extractCleanBrand(String name) {
        if (name == null || name.isEmpty()) return "GENÉRICO";
        String[] words = name.split(" ");
        StringBuilder brand = new StringBuilder();

        for (String word : words) {
            // Regla: La palabra debe ser TODAS MAYÚSCULAS y tener más de 1 letra
            // Paramos en cuanto aparezca una palabra con minúsculas (la descripción)
            if (word.equals(word.toUpperCase()) && word.length() > 1 && word.matches("[A-ZÁÉÍÓÚÑ0-9]+")) {
                if (brand.length() > 0) brand.append(" ");
                brand.append(word);
            } else {
                break;
            }
        }
        return brand.length() > 0 ? brand.toString() : "GENÉRICO";
    }
}