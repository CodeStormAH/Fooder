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

            // Aceptar cookies si aparecen
            try {
                driver.findElement(By.id("onetrust-accept-btn-handler")).click();
            } catch (Exception e) {}

            JavascriptExecutor js = (JavascriptExecutor) driver;
            int lastCount = 0;
            int sameCountTimes = 0;

            System.out.println("Iniciando extracción en Alcampo (Límite: " + TESTING_LIMIT + ")...");

            while (sameCountTimes < 5 && extractedProducts.size() < TESTING_LIMIT) {
                // Seleccionamos el contenedor de cada producto
                List<WebElement> webElements = driver.findElements(
                        By.cssSelector("[data-retailer-anchor='product-list'] div[data-test^='fop-wrapper']")
                );

                for (WebElement element : webElements) {
                    if (extractedProducts.size() >= TESTING_LIMIT) break;

                    try {
                        String fullName = element.findElement(By.cssSelector("[data-test='fop-title']")).getText();

                        if (!extractedProducts.containsKey(fullName) && !fullName.isEmpty()) {

                            // 1. PRECIO TOTAL Y UNITARIO
                            String priceText = element.findElement(By.cssSelector("[data-test='fop-price']")).getText();
                            double priceValue = parseNumericValue(priceText);

                            double unitPriceValue = 0.0;
                            try {
                                String uPriceText = element.findElement(By.cssSelector("[data-test='fop-price-per-unit']")).getText();
                                unitPriceValue = parseNumericValue(uPriceText);
                            } catch (Exception e) {}

                            // 2. CANTIDAD Y UNIDAD (Corregido para detectar gramos 'g')
                            String sizeText = "";
                            try {
                                // Leemos el texto visible para evitar IDs internos
                                sizeText = element.findElement(By.cssSelector("[data-test='fop-size'] span")).getText();
                            } catch (Exception e) {
                                sizeText = element.findElement(By.cssSelector("[data-test='fop-size']")).getText();
                            }

                            double quantityValue = parseQuantityValue(sizeText);
                            String unitValue = parseUnitLabel(sizeText);

                            // 3. MARCA (Solo palabras en MAYÚSCULAS al inicio)
                            String brandValue = extractCleanBrand(fullName);

                            // 4. OFERTA
                            boolean isOnSale = !element.findElements(By.cssSelector(".promotion-container")).isEmpty();

                            String normalizedName = cleanNormalizedName(fullName, brandValue);

                            extractedProducts.put(fullName, new Product(
                                    UUID.randomUUID().toString(),
                                    fullName,
                                    normalizedName,
                                    brandValue,
                                    "general_category", // Revertido a categoría fija como solicitaste
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
                    System.out.println("Progreso: " + lastCount + "/" + TESTING_LIMIT);
                }
            }
        } catch (Exception e) {
            System.err.println("Error durante la ejecución: " + e.getMessage());
        } finally {
            driver.quit();
        }
        return new ArrayList<>(extractedProducts.values());
    }

    // --- MÉTODOS DE SOPORTE PARA LIMPIEZA DE DATOS ---

    private double parseNumericValue(String text) {
        if (text == null || text.isEmpty()) return 0;
        // Convierte "0,20 €" en "0.20" para poder parsearlo a Double
        String cleaned = text.replaceAll("[^0-9,]", "").replace(",", ".");
        try {
            return Double.parseDouble(cleaned);
        } catch (Exception e) { return 0; }
    }

    private double parseQuantityValue(String text) {
        if (text == null || text.isEmpty()) return 1.0;
        String cleaned = text.replaceAll("[^0-9,.]", "").replace(",", ".");
        try {
            double val = Double.parseDouble(cleaned);
            // Si detectamos mililitros, dividimos por 1000 para que coincida con la unidad "L"
            if (text.toLowerCase().contains("ml")) return val / 1000.0;
            return val;
        } catch (Exception e) { return 1.0; }
    }

    private String parseUnitLabel(String text) {
        if (text == null) return "ud";
        String lower = text.toLowerCase();

        // Detección de Líquidos
        if (lower.contains("ml") || lower.contains("litro") || lower.contains(" l")) return "L";

        // Detección de Peso (Gramos y Kilos)
        if (lower.contains("kg") || lower.contains("kilo")) return "kg";
        if (lower.matches(".*\\d\\s?g($|\\s).*") || lower.contains("gramo") || lower.endsWith("g")) return "g";

        return "ud";
    }

    private String extractCleanBrand(String name) {
        if (name == null || name.isEmpty()) return "GENÉRICO";
        String[] words = name.split(" ");
        StringBuilder brand = new StringBuilder();

        for (String word : words) {
            // Se queda con las palabras en mayúsculas (ej: FONT VELLA) y para al llegar a la descripción
            if (word.equals(word.toUpperCase()) && word.length() > 1 && word.matches("[A-ZÁÉÍÓÚÑ0-9]+")) {
                if (brand.length() > 0) brand.append(" ");
                brand.append(word);
            } else {
                break;
            }
        }
        return brand.length() > 0 ? brand.toString() : "GENÉRICO";
    }

    private String cleanNormalizedName(String fullName, String brand) {
        // A. Quitamos la marca si existe al principio
        String cleaned = fullName.replace(brand, "").trim();

        // B. Pasamos a minúsculas
        cleaned = cleaned.toLowerCase();

        // C. Quitamos patrones de cantidades (pack de X, botella de, x 1,5l, etc.)
        cleaned = cleaned.replaceAll("(?i)pack de \\d+", "");
        cleaned = cleaned.replaceAll("(?i)botella de", "");
        cleaned = cleaned.replaceAll("(?i)uds\\.?", "");

        // D. Quitamos números acompañados de unidades (1,5l, 500g, 9000ml)
        cleaned = cleaned.replaceAll("\\d+[\\.,]?\\d*\\s?(ml|l|g|kg|cl)", "");

        // E. Quitamos multiplicadores tipo "6 x 1,5" o "3 x 33"
        cleaned = cleaned.replaceAll("\\d+\\s?x\\s?\\d+[\\.,]?\\d*", "");

        // F. Quitamos números sueltos y caracteres especiales
        cleaned = cleaned.replaceAll("[0-9]+", "");
        cleaned = cleaned.replaceAll("[\\.,\\(\\)\\-x]", "");

        // G. Quitamos palabras que hayan quedado en mayúsculas (por si eran parte de la marca no detectada)
        // En este punto todo es minúscula por el step B, así que limpiamos espacios extra
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return cleaned.isEmpty() ? fullName.toLowerCase() : cleaned;
    }
}