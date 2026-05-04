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
import java.nio.charset.StandardCharsets;
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
    private final String categoriesPath;
    private static final int SEARCH_WAIT_MS = 4000;
    private static final int SCROLL_WAIT_MS = 2000;

    public AlcampoScraperFeeder(String baseUrl, String categoriesPath) {
        this.baseUrl = baseUrl;
        this.categoriesPath = categoriesPath;
    }

    @Override
    public List<Product> fetchProducts() {
        List<String> categories = loadCategories();
        if (categories.isEmpty()) return new ArrayList<>();

        return executeScrapingSession(categories);
    }

    private List<Product> executeScrapingSession(List<String> categories) {
        WebDriver driver = setupDriver();
        Map<String, Product> products = new HashMap<>();
        try {
            processAllCategories(driver, categories, products);
        } finally {
            driver.quit();
        }
        return new ArrayList<>(products.values());
    }

    private WebDriver setupDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        return new ChromeDriver(options);
    }

    private void processAllCategories(WebDriver driver, List<String> categories, Map<String, Product> products) {
        for (String category : categories) {
            if (category.isBlank()) continue;
            System.out.println("Searching for: " + category);
            scrapeCategory(driver, category, products);
        }
    }

    private void scrapeCategory(WebDriver driver, String category, Map<String, Product> products) {
        navigateToHome(driver);
        acceptCookies(driver);
        performSearch(driver, category);
        extractCategoryProducts(driver, category, products);
    }

    private void performSearch(WebDriver driver, String category) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement searchBar = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[placeholder*='Buscar'], input[type='search']")
            ));

            searchBar.clear();
            searchBar.sendKeys(category);
            searchBar.sendKeys(Keys.ENTER);
            pause(SEARCH_WAIT_MS);
        } catch (Exception e) {
            System.err.println("Search error for '" + category + "': " + e.getMessage());
        }
    }

    private void extractCategoryProducts(WebDriver driver, String category, Map<String, Product> products) {
        int currentCategoryCount = 0;
        int attemptsWithoutNewData = 0;
        int lastCount = 0;

        while (attemptsWithoutNewData < 4) {
            int addedInThisCycle = scanPageForProducts(driver, category, products, currentCategoryCount);
            currentCategoryCount += addedInThisCycle;

            scrollDown(driver);
            pause(SCROLL_WAIT_MS);

            if (currentCategoryCount == lastCount) {
                attemptsWithoutNewData++;
            } else {
                attemptsWithoutNewData = 0;
                lastCount = currentCategoryCount;
            }
        }
        System.out.println("   Finished " + category + " (" + currentCategoryCount + " products)");
    }

    private int scanPageForProducts(WebDriver driver, String category, Map<String, Product> products, int currentCount) {
        int added = 0;
        try {
            List<WebElement> elements = driver.findElements(By.cssSelector("[data-test^='fop-wrapper']"));
            for (WebElement element : elements) {
                if (processElement(element, category, products)) {
                    added++;
                }
            }
        } catch (Exception ignored) {}
        return added;
    }

    private boolean processElement(WebElement element, String category, Map<String, Product> products) {
        try {
            String fullName = element.findElement(By.cssSelector("[data-test='fop-title']")).getText();
            if (fullName.isEmpty() || products.containsKey(fullName)) return false;

            products.put(fullName, createProductFromElement(element, fullName, category));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Product createProductFromElement(WebElement el, String name, String category) {
        double price = fetchPrice(el, "[data-test='fop-price']");
        double unitPrice = fetchPrice(el, "[data-test='fop-price-per-unit']");
        String sizeText = getElementText(el, "[data-test='fop-size'] span", "[data-test='fop-size']");

        String brand = extractBrand(name);
        String normalizedName = cleanName(name, brand);
        boolean isSale = !el.findElements(By.cssSelector(".promotion-container")).isEmpty();
        String deterministicId = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();

        return new Product(
                deterministicId, name, normalizedName,
                brand, category, price, unitPrice, parseUnit(sizeText),
                parseQuantity(sizeText), isSale
        );
    }

    private double fetchPrice(WebElement parent, String selector) {
        try {
            String text = parent.findElement(By.cssSelector(selector)).getText();
            return parseNumeric(text);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String getElementText(WebElement parent, String primarySelector, String fallbackSelector) {
        try {
            return parent.findElement(By.cssSelector(primarySelector)).getText();
        } catch (Exception e) {
            try {
                return parent.findElement(By.cssSelector(fallbackSelector)).getText();
            } catch (Exception fallback) {
                return "";
            }
        }
    }

    private double parseNumeric(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(text.replaceAll("[^0-9,]", "").replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double parseQuantity(String text) {
        if (text == null || text.isEmpty()) return 1.0;
        try {
            double val = Double.parseDouble(text.replaceAll("[^0-9,.]", "").replace(",", "."));
            return text.toLowerCase().contains("ml") ? val / 1000.0 : val;
        } catch (Exception e) {
            return 1.0;
        }
    }

    private String parseUnit(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("ml") || lower.contains(" l") || lower.contains("litro")) return "L";
        if (lower.contains("kg") || lower.contains("kilo")) return "kg";
        if (lower.matches(".*\\d\\s?g($|\\s).*") || lower.contains("gramo") || lower.endsWith("g")) return "g";
        return "ud";
    }

    private String extractBrand(String name) {
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

    private String cleanName(String fullName, String brand) {
        String cleaned = fullName.replace(brand, "").trim().toLowerCase();
        cleaned = cleaned.replaceAll("(?i)pack de \\d+|botella de|uds\\.?|\\d+[\\.,]?\\d*\\s?(ml|l|g|kg|cl)|\\d+\\s?x\\s?\\d+[\\.,]?\\d*|[0-9]+|[\\.,\\(\\)\\-x]", "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? fullName.toLowerCase() : cleaned;
    }

    private List<String> loadCategories() {
        try {
            return Files.readAllLines(Paths.get(this.categoriesPath));
        } catch (IOException e) {
            System.err.println("Critical error reading categories file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void navigateToHome(WebDriver driver) {
        try {
            driver.get(this.baseUrl);
            pause(3000);
        } catch (Exception ignored) {}
    }

    private void acceptCookies(WebDriver driver) {
        try {
            WebElement btn = driver.findElement(By.id("onetrust-accept-btn-handler"));
            if (btn.isDisplayed()) btn.click();
        } catch (Exception ignored) {}
    }

    private void scrollDown(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollBy(0, 1000);");
        } catch (Exception ignored) {}
    }

    private void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}