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

        // --- SPEED OPTIMIZATIONS ---
        options.addArguments("--headless=new");
        options.addArguments("--start-maximized");
        options.addArguments("--blink-settings=imagesEnabled=false"); // DISABLE IMAGES
        options.addArguments("--disable-blink-features=AutomationControlled"); // Less detectable

        WebDriver driver = new ChromeDriver(options);
        Map<String, Product> extractedProducts = new HashMap<>();

        try {
            driver.get(this.url);
            Thread.sleep(4000);

            // Accept cookies quickly
            try {
                driver.findElement(By.id("onetrust-accept-btn-handler")).click();
            } catch (Exception e) {}

            JavascriptExecutor js = (JavascriptExecutor) driver;
            int lastCount = 0;
            int sameCountTimes = 0;

            System.out.println("Starting optimized extraction (no images)...");

            while (sameCountTimes < 5) {
                List<WebElement> webElements = driver.findElements(
                        By.cssSelector("[data-retailer-anchor='product-list'] div[data-test^='fop-wrapper']")
                );

                for (WebElement element : webElements) {
                    try {
                        String name = element.findElement(By.cssSelector("[data-test='fop-title']")).getText();
                        if (!extractedProducts.containsKey(name) && !name.isEmpty()) {

                            String priceText = element.findElement(By.cssSelector("[data-test='fop-price']")).getText();
                            String sizeText = element.findElement(By.cssSelector("[data-test='fop-size']")).getText();

                            double priceValue = parsePrice(priceText);
                            double quantityValue = parseQuantity(sizeText);
                            String unitValue = parseUnit(sizeText);
                            String brandValue = extractBrand(name);
                            String id = UUID.randomUUID().toString();

                            extractedProducts.put(name, new Product(
                                    id,
                                    name,
                                    name.toLowerCase(),
                                    brandValue,
                                    "general_category",
                                    priceValue,
                                    unitValue,
                                    quantityValue,
                                    false
                            ));
                        }
                    } catch (Exception e) {}
                }

                // Smooth scroll and wait
                js.executeScript("window.scrollBy(0, 1000);");
                Thread.sleep(800);

                if (extractedProducts.size() == lastCount) {
                    sameCountTimes++;
                } else {
                    sameCountTimes = 0;
                    lastCount = extractedProducts.size();
                    if(lastCount % 100 == 0) System.out.println("Accumulated: " + lastCount + " products...");
                }
            }
        } catch (Exception e) {
            System.out.println("Notice: Scrolling stopped, but extracted data is preserved.");
        } finally {
            driver.quit();
        }
        return new ArrayList<>(extractedProducts.values());
    }

    private double parsePrice(String text) {
        if (text == null || text.isEmpty()) return 0;
        return Double.parseDouble(text.replace("€", "").replace(",", ".").replaceAll("[^0-9.]", ""));
    }

    private double parseQuantity(String text) {
        if (text == null) return 1;
        String n = text.replaceAll("[^0-9]", "");
        return n.isEmpty() ? 1 : Double.parseDouble(n);
    }

    private String parseUnit(String text) {
        return text == null ? "" : text.replaceAll("[0-9 ]", "");
    }

    private String extractBrand(String name) {
        return name.isEmpty() ? "Unknown" : name.split(" ")[0];
    }
}
