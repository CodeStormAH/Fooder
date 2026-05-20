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
        List<Product> products = new ArrayList<>();
        try {
            executeCategoriesLoop(driver, categories, products);
        } finally {
            driver.quit();
        }
        return products;
    }

    private void executeCategoriesLoop(WebDriver driver, Map<String, String> categories, List<Product> products) {
        for (Map.Entry<String, String> entry : categories.entrySet()) {
            if (entry.getKey().isBlank() || entry.getValue().isBlank()) continue;
            ScrapingContext context = new ScrapingContext(entry.getKey(), entry.getValue(), products);
            logger.info("Searching for: '" + context.getSearchTerm() + "'");
            scrapeCategory(driver, context);
        }
    }

    private WebDriver setupDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        return new ChromeDriver(options);
    }

    private void scrapeCategory(WebDriver driver, ScrapingContext context) {
        navigateToHome(driver);
        acceptCookies(driver);
        performSearch(driver, context.getSearchTerm());
        applyDrinksFilter(driver);
        extractCategoryProducts(driver, context);
    }

    private void navigateToHome(WebDriver driver) {
        try {
            executeNavigation(driver);
        } catch (Exception ignored) {}
    }

    private void executeNavigation(WebDriver driver) {
        driver.get(this.baseUrl);
        pause(3000);
    }

    private void acceptCookies(WebDriver driver) {
        try {
            executeCookieAcceptance(driver);
        } catch (Exception ignored) {}
    }

    private void executeCookieAcceptance(WebDriver driver) {
        WebElement acceptButton = driver.findElement(By.id("onetrust-accept-btn-handler"));
        if (acceptButton.isDisplayed()) acceptButton.click();
    }

    private void performSearch(WebDriver driver, String searchTerm) {
        try {
            executeSearchSequence(driver, searchTerm);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Search error for '" + searchTerm + "': " + e.getMessage());
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
            logger.warning("Drink filter not found");
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

    private void extractCategoryProducts(WebDriver driver, ScrapingContext context) {
        int totalProducts = 0;
        int retries = 0;
        while (retries < 4) {
            int newTotal = totalProducts + scanPageForProducts(driver, context);
            retries = (newTotal == totalProducts) ? retries + 1 : 0;
            totalProducts = newTotal;
            scrollDownAndPause(driver);
        }
        logger.info("Finished category '" + context.getCategoryName() + "' (" + totalProducts + " products)");
    }

    private void scrollDownAndPause(WebDriver driver) {
        try {
            executeScrollDown(driver);
        } catch (Exception ignored) {}
        pause(SCROLL_WAIT_MS);
    }

    private void executeScrollDown(WebDriver driver) {
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) driver;
        javascriptExecutor.executeScript("window.scrollBy(0, 1000);");
    }

    private int scanPageForProducts(WebDriver driver, ScrapingContext context) {
        try {
            return executeElementsScanning(driver, context);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error scanning page: " + e.getMessage());
            return 0;
        }
    }

    private int executeElementsScanning(WebDriver driver, ScrapingContext context) {
        List<WebElement> elements = driver.findElements(By.cssSelector("[data-test^='fop-wrapper']"));
        int addedProducts = 0;
        for (WebElement element : elements) {
            if (processElement(element, context)) {
                addedProducts++;
            }
        }
        return addedProducts;
    }

    private boolean processElement(WebElement element, ScrapingContext context) {
        try {
            return executeElementProcessing(element, context);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean executeElementProcessing(WebElement element, ScrapingContext context) {
        String fullName = element.findElement(By.cssSelector("[data-test='fop-title']")).getText();
        if (fullName.isEmpty() || containsProduct(context.getProducts(), fullName)) return false;
        Product product = createProductFromElement(element, context.getCategoryName());
        context.getProducts().add(product);
        return true;
    }

    private boolean containsProduct(List<Product> products, String name) {
        for (Product p : products) {
            if (p.getName().equals(name)) return true;
        }
        return false;
    }

    private Product createProductFromElement(WebElement element, String categoryName) {
        String name = element.findElement(By.cssSelector("[data-test='fop-title']")).getText();
        String sizeText = resolveSizeText(element, name);
        String brand = extractBrand(name);

        return new Product(
                generateProductId(name),
                name,
                cleanName(name, brand),
                brand,
                categoryName,
                fetchPrice(element),
                parseUnit(sizeText),
                parseQuantity(sizeText),
                checkIfOnSale(element)
        );
    }

    private String generateProductId(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String resolveSizeText(WebElement element, String name) {
        String sizeText = getElementText(element);
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

    private double fetchPrice(WebElement element) {
        try {
            return executeFetchPrice(element);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double executeFetchPrice(WebElement element) {
        String text = element.findElement(By.cssSelector("[data-test='fop-price']")).getText();
        return parseNumeric(text);
    }

    private String getElementText(WebElement element) {
        try {
            return element.findElement(By.cssSelector("[data-test='fop-size'] span")).getText();
        } catch (Exception e) {
            return getFallbackText(element);
        }
    }

    private String getFallbackText(WebElement element) {
        try {
            return element.findElement(By.cssSelector("[data-test='fop-size']")).getText();
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

    private void pause(int milliseconds) {
        try {
            executeSleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void executeSleep(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }

    private static class ScrapingContext {
        private final String categoryName;
        private final String searchTerm;
        private final List<Product> products;

        public ScrapingContext(String categoryName, String searchTerm, List<Product> products) {
            this.categoryName = categoryName;
            this.searchTerm = searchTerm;
            this.products = products;
        }

        public String getCategoryName() { return categoryName; }
        public String getSearchTerm() { return searchTerm; }
        public List<Product> getProducts() { return products; }
    }
}