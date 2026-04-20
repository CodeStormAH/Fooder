package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AlcampoScraperFeeder implements AlcampoFeeder {
    private final String baseUrl;
    private final String filePath;
    private static final int LIMIT_PER_CATEGORY = 100;

    public AlcampoScraperFeeder(String baseUrl, String filePath) {
        this.baseUrl = baseUrl;
        this.filePath = filePath;
    }

    private void wait(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public List<Product> fetchProducts() {
        List<String> categories = loadCategoriesFromFile();
        if (categories.isEmpty()) return new ArrayList<>();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        Map<String, Product> extractedProducts = new HashMap<>();

        try {
            for (String currentCategory : categories) {
                if (currentCategory.trim().isEmpty()) continue;

                driver.get(this.baseUrl);
                wait(3000);

                try {
                    WebElement cookieBtn = driver.findElement(By.id("onetrust-accept-btn-handler"));
                    if(cookieBtn.isDisplayed()) cookieBtn.click();
                } catch (Exception e) {}

                System.out.println("🔎 Iniciando búsqueda de: " + currentCategory);

                try {
                    WebElement searchBar = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("input[placeholder*='Buscar'], input[type='search']")
                    ));

                    searchBar.clear();
                    searchBar.sendKeys(currentCategory);
                    searchBar.sendKeys(Keys.ENTER);

                    Thread.sleep(4000);

                    int categoryCount = 0;
                    int sameCountTimes = 0;
                    int lastCount = 0;
                    JavascriptExecutor js = (JavascriptExecutor) driver;

                    while (sameCountTimes < 4 && categoryCount < LIMIT_PER_CATEGORY) {
                        List<WebElement> webElements = driver.findElements(
                                By.cssSelector("[data-retailer-anchor='product-list'] div[data-test^='fop-wrapper']")
                        );

                        for (WebElement element : webElements) {
                            if (categoryCount >= LIMIT_PER_CATEGORY) break;

                            try {
                                String fullName = element.findElement(By.cssSelector("[data-test='fop-title']")).getText();

                                if (!fullName.isEmpty() && !extractedProducts.containsKey(fullName)) {

                                    double priceValue = parseNumericValue(element.findElement(By.cssSelector("[data-test='fop-price']")).getText());
                                    double unitPriceValue = 0.0;
                                    try {
                                        String uPriceText = element.findElement(By.cssSelector("[data-test='fop-price-per-unit']")).getText();
                                        unitPriceValue = parseNumericValue(uPriceText);
                                    } catch (Exception e) {}
                                    String sizeText = getElementText(element, "[data-test='fop-size'] span", "[data-test='fop-size']");

                                    String brandValue = extractCleanBrand(fullName);
                                    String normalizedName = cleanNormalizedName(fullName, brandValue);

                                    extractedProducts.put(fullName, new Product(
                                            UUID.randomUUID().toString(),
                                            fullName,
                                            normalizedName,
                                            brandValue,
                                            currentCategory,
                                            priceValue,
                                            unitPriceValue,
                                            parseUnitLabel(sizeText),
                                            parseQuantityValue(sizeText),
                                            !element.findElements(By.cssSelector(".promotion-container")).isEmpty()
                                    ));
                                    categoryCount++;
                                }
                            } catch (Exception e) {}
                        }

                        js.executeScript("window.scrollBy(0, 1000);");
                        Thread.sleep(2000);

                        if (categoryCount == lastCount) sameCountTimes++;
                        else { sameCountTimes = 0; lastCount = categoryCount; }
                    }
                    System.out.println("   ✅ Finalizado " + currentCategory + " (" + categoryCount + " productos)");

                } catch (Exception e) {
                    System.err.println("⚠️ Error buscando '" + currentCategory + "': " + e.getMessage());
                }
            }
        } finally {
            driver.quit();
        }
        return new ArrayList<>(extractedProducts.values());
    }

    private List<String> loadCategoriesFromFile() {
        try {
            return Files.readAllLines(Paths.get(this.filePath));
        } catch (IOException e) {
            System.err.println("❌ Error crítico leyendo el fichero: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String cleanNormalizedName(String fullName, String brand) {
        String cleaned = fullName.replace(brand, "").trim().toLowerCase();
        cleaned = cleaned.replaceAll("(?i)pack de \\d+|botella de|uds\\.?|\\d+[\\.,]?\\d*\\s?(ml|l|g|kg|cl)|\\d+\\s?x\\s?\\d+[\\.,]?\\d*|[0-9]+|[\\.,\\(\\)\\-x]", "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? fullName.toLowerCase() : cleaned;
    }

    private double parseNumericValue(String text) {
        if (text == null || text.isEmpty()) return 0;
        try { return Double.parseDouble(text.replaceAll("[^0-9,]", "").replace(",", ".")); } catch (Exception e) { return 0; }
    }

    private double parseQuantityValue(String text) {
        if (text == null || text.isEmpty()) return 1.0;
        try {
            double val = Double.parseDouble(text.replaceAll("[^0-9,.]", "").replace(",", "."));
            return text.toLowerCase().contains("ml") ? val / 1000.0 : val;
        } catch (Exception e) { return 1.0; }
    }

    private String parseUnitLabel(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("ml") || lower.contains(" l") || lower.contains("litro")) return "L";
        if (lower.contains("kg") || lower.contains("kilo")) return "kg";
        if (lower.matches(".*\\d\\s?g($|\\s).*") || lower.contains("gramo") || lower.endsWith("g")) return "g";
        return "ud";
    }

    private String extractCleanBrand(String name) {
        String[] words = name.split(" ");
        StringBuilder brand = new StringBuilder();
        for (String word : words) {
            if (word.equals(word.toUpperCase()) && word.length() > 1 && word.matches("[A-ZÁÉÍÓÚÑ0-9]+")) {
                if (brand.length() > 0) brand.append(" ");
                brand.append(word);
            } else break;
        }
        return brand.length() > 0 ? brand.toString() : "GENÉRICO";
    }

    private String getElementText(WebElement parent, String selector, String fallback) {
        try { return parent.findElement(By.cssSelector(selector)).getText(); }
        catch (Exception e) {
            try { return parent.findElement(By.cssSelector(fallback)).getText(); }
            catch (Exception e2) { return ""; }
        }
    }
}