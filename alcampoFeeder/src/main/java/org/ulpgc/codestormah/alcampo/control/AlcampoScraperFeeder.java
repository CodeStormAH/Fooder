package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlcampoScraperFeeder implements AlcampoFeeder {

    private static final Logger logger = Logger.getLogger(AlcampoScraperFeeder.class.getName());
    private static final int SEARCH_WAIT_MS = 4000;
    private static final int SCROLL_WAIT_MS = 2000;
    private final String baseUrl;
    private final String categoriesPath;

    public AlcampoScraperFeeder(String baseUrl, String categoriesPath) {
        this.baseUrl = baseUrl;
        this.categoriesPath = categoriesPath;
    }

    @Override
    public List<Product> fetchProducts() {
        Map<String, String> categories = loadCategories();
        if (categories.isEmpty()) return new ArrayList<>();
        return executeScrapingSession(categories);
    }

    private Map<String, String> loadCategories() {
        try {
            return readCategoriesFromFile();
        } catch (IOException e) {
            logger.severe("Critical error reading categories file: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private Map<String, String> readCategoriesFromFile() throws IOException {
        Map<String, String> categoryMap = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(this.categoriesPath));
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            parseCategoryLine(line, categoryMap);
        }
        return categoryMap;
    }

    private void parseCategoryLine(String line, Map<String, String> categoryMap) {
        String[] parts = line.split(":");
        if (parts.length == 2) {
            categoryMap.put(parts[0].trim(), parts[1].trim());
        } else {
            categoryMap.put(line.trim(), line.trim());
        }
    }

    private List<Product> executeScrapingSession(Map<String, String> categories) {
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

    private void processAllCategories(WebDriver driver, Map<String, String> categories, Map<String, Product> products) {
        for (Map.Entry<String, String> entry : categories.entrySet()) {
            String categoryName = entry.getKey();
            String searchTerm = entry.getValue();
            if (categoryName.isBlank() || searchTerm.isBlank()) continue;
            logger.info("Searching for: '" + searchTerm + "' (Saving as Category: '" + categoryName + "')");
            scrapeCategory(driver, categoryName, searchTerm, products);
        }
    }

    private void scrapeCategory(WebDriver driver, String categoryName, String searchTerm, Map<String, Product> products) {
        navigateToHome(driver);
        acceptCookies(driver);
        performSearch(driver, searchTerm);
        applyDrinksFilter(driver);
        extractCategoryProducts(driver, categoryName, products);
    }

    private void navigateToHome(WebDriver driver) {
        try {
            driver.get(this.baseUrl);
            pause(3000);
        } catch (Exception ignored) {}
    }

    private void acceptCookies(WebDriver driver) {
        try {
            WebElement acceptButton = driver.findElement(By.id("onetrust-accept-btn-handler"));
            if (acceptButton.isDisplayed()) acceptButton.click();
        } catch (Exception ignored) {}
    }

    private void performSearch(WebDriver driver, String searchTerm) {
        try {
            executeSearchSequence(driver, searchTerm);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Search error for '" + searchTerm + "': " + e.getMessage(), e);
        }
    }

    private void executeSearchSequence(WebDriver driver, String searchTerm) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement searchBar = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[placeholder*='Buscar'], input[type='search']")));
        searchBar.clear();
        searchBar.sendKeys(searchTerm);
        searchBar.sendKeys(Keys.ENTER);
        pause(SEARCH_WAIT_MS);
    }

    private void applyDrinksFilter(WebDriver driver) {
        try {
            executeDrinksFilterLogic(driver);
        } catch (Exception e) {
            logger.warning("Drink filter does not found");
        }
    }

    private void executeDrinksFilterLogic(WebDriver driver) {
        clickBebidasLink(driver);
        waitForCategoryToLoad(driver);
    }

    private void clickBebidasLink(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        WebElement bebidasLink = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@data-test='root-category-link' and contains(normalize-space(), 'Bebidas')]")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", bebidasLink);
        pause(700);
        js.executeScript("arguments[0].click();", bebidasLink);
        logger.info("Filter 'Drinks' applied.");
    }

    private void waitForCategoryToLoad(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        wait.until(ExpectedConditions.urlContains("sublocationId"));
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("[data-test^='fop-wrapper']")));
        pause(1500);
    }

    private void extractCategoryProducts(WebDriver driver, String categoryName, Map<String, Product> products) {
        int totalProducts = 0;
        int retries = 0;
        while (retries < 4) {
            int newTotal = totalProducts + scanPageForProducts(driver, categoryName, products);
            retries = (newTotal == totalProducts) ? retries + 1 : 0;
            totalProducts = newTotal;
            scrollDownAndPause(driver);
        }
        logger.info("Finished category '" + categoryName + "' (" + totalProducts + " products)");
    }

    private void scrollDownAndPause(WebDriver driver) {
        scrollDown(driver);
        pause(SCROLL_WAIT_MS);
    }

    private int scanPageForProducts(WebDriver driver, String categoryName, Map<String, Product> products) {
        int addedProducts = 0;
        try {
            List<WebElement> elements = driver.findElements(By.cssSelector("[data-test^='fop-wrapper']"));
            for (WebElement element : elements) {
                if (processElement(element, categoryName, products)) {
                    addedProducts++;
                }
            }
        } catch (Exception ignored) {}
        return addedProducts;
    }

    private boolean processElement(WebElement element, String categoryName, Map<String, Product> products) {
        try {
            return extractAndSaveProduct(element, categoryName, products);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean extractAndSaveProduct(WebElement element, String categoryName, Map<String, Product> products) {
        String fullName = element.findElement(By.cssSelector("[data-test='fop-title']")).getText();
        if (fullName.isEmpty() || products.containsKey(fullName)) return false;
        Product product = createProductFromElement(element, fullName, categoryName);
        products.put(fullName, product);
        return true;
    }

    private Product createProductFromElement(WebElement element, String name, String categoryName) {
        double unitPrice = fetchPrice(element, "[data-test='fop-price']");
        String sizeText = resolveSizeText(element, name);
        String brand = extractBrand(name);
        String normalizedName = cleanName(name, brand);
        boolean isSale = checkIfOnSale(element);
        String id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
        return new Product(
                id, name, normalizedName, brand, categoryName,
                unitPrice, parseUnit(sizeText), parseQuantity(sizeText), isSale
        );
    }

    private String resolveSizeText(WebElement element, String name) {
        String sizeText = getElementText(element, "[data-test='fop-size'] span", "[data-test='fop-size']");
        if (!sizeText.toLowerCase().matches(".*\\d.*(ml|cl|\\bl\\b|litro|kg|gramo).*")) {
            return extractSizeFromName(name);
        }
        return sizeText;
    }

    private boolean checkIfOnSale(WebElement element) {
        return !element.findElements(By.cssSelector(".promotion-container")).isEmpty();
    }

    private String extractSizeFromName(String name) {
        Pattern pattern = Pattern.compile("(\\d+[,.]?\\d*)\\s*(ml|cl|litros?|\\bl\\b)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(name);
        return matcher.find() ? matcher.group() : "";
    }

    private double fetchPrice(WebElement parentElement, String selector) {
        try {
            String text = parentElement.findElement(By.cssSelector(selector)).getText();
            return parseNumeric(text);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String getElementText(WebElement parentElement, String primarySelector, String fallbackSelector) {
        try {
            return parentElement.findElement(By.cssSelector(primarySelector)).getText();
        } catch (Exception e) {
            return getFallbackText(parentElement, fallbackSelector);
        }
    }

    private String getFallbackText(WebElement parentElement, String fallbackSelector) {
        try {
            return parentElement.findElement(By.cssSelector(fallbackSelector)).getText();
        } catch (Exception fallback) {
            return "";
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
            return calculateQuantity(text);
        } catch (Exception e) {
            return 1.0;
        }
    }

    private double calculateQuantity(String text) {
        double parsedValue = Double.parseDouble(text.replaceAll("[^0-9,.]", "").replace(",", "."));
        String lowerCaseText = text.toLowerCase();
        if (lowerCaseText.contains("ml")) return parsedValue / 1000.0;
        if (lowerCaseText.contains("cl")) return parsedValue / 100.0;
        return parsedValue;
    }

    private String parseUnit(String text) {
        String lowerCaseText = text.toLowerCase();
        if (lowerCaseText.contains("ml") || lowerCaseText.contains("cl") || lowerCaseText.contains(" l") || lowerCaseText.contains("litro")) return "l";
        if (lowerCaseText.contains("kg") || lowerCaseText.contains("kilo")) return "kg";
        if (lowerCaseText.matches(".*\\d\\s?g($|\\s).*") || lowerCaseText.contains("gramo") || lowerCaseText.endsWith("g")) return "g";
        return "ud";
    }

    private String extractBrand(String name) {
        String[] words = name.split(" ");
        StringBuilder brandBuilder = new StringBuilder();
        for (String word : words) {
            if (isUpperCaseWord(word)) {
                if (brandBuilder.length() > 0) brandBuilder.append(" ");
                brandBuilder.append(word);
            } else break;
        }
        return brandBuilder.length() > 0 ? brandBuilder.toString() : "GENÉRICO";
    }

    private boolean isUpperCaseWord(String word) {
        return word.equals(word.toUpperCase()) && word.length() > 1 && word.matches("[A-ZÁÉÍÓÚÑ0-9]+");
    }

    private String cleanName(String fullName, String brand) {
        String cleanedName = fullName.replace(brand, "").trim().toLowerCase();
        cleanedName = cleanedName.replaceAll("(?i)pack de \\d+|botella de|uds\\.?|\\d+[\\.,]?\\d*\\s?(ml|l|g|kg|cl)|\\d+\\s?x\\s?\\d+[\\.,]?\\d*|[0-9]+|[\\.,\\(\\)\\-x]", "");
        cleanedName = cleanedName.replaceAll("\\s+", " ").trim();
        return cleanedName.isEmpty() ? fullName.toLowerCase() : cleanedName;
    }

    private void scrollDown(WebDriver driver) {
        try {
            JavascriptExecutor javascriptExecutor = (JavascriptExecutor) driver;
            javascriptExecutor.executeScript("window.scrollBy(0, 1000);");
        } catch (Exception ignored) {}
    }

    private void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}