package org.ulpgc.codestormah.mercadona.controller;

import com.google.gson.*;
import org.jsoup.Jsoup;
import org.ulpgc.codestormah.mercadona.model.Product;
import org.ulpgc.codestormah.mercadona.model.ProductTextProcessor;

import java.io.IOException;
import java.util.Set;

public class MercadonaFeeder implements ProductFeeder {

    private static final int TIMEOUT_MS = 15000;

    private final String apiUrl;
    private final String categoriesPath;
    private final Set<String> allowedCategories;
    private final EventPublisher publisher;
    private final String eventTopic;

    public MercadonaFeeder(
            String apiUrl,
            String categoriesPath,
            Set<String> allowedCategories,
            EventPublisher publisher,
            String eventTopic
    ) {
        this.apiUrl = apiUrl;
        this.categoriesPath = categoriesPath;
        this.allowedCategories = allowedCategories;
        this.publisher = publisher;
        this.eventTopic = eventTopic;
    }

    @Override
    public void run(int maxProducts) throws IOException {
        for (JsonElement rootCategory : fetchRootCategories()) {
            processRootCategory(rootCategory);
        }
    }

    private void processRootCategory(JsonElement root) throws IOException {
        JsonArray subcategories = extractCategories(root);
        if (subcategories == null) return;

        for (JsonElement sub : subcategories) {
            processSubcategory(sub);
        }
    }

    private void processSubcategory(JsonElement subcategoryElement) throws IOException {
        JsonObject subcategory = subcategoryElement.getAsJsonObject();
        String categoryName = extractName(subcategory);

        if (!isAllowed(categoryName)) return;

        JsonArray nestedCategories = fetchNestedCategories(subcategory);
        if (nestedCategories == null) return;

        for (JsonElement nested : nestedCategories) {
            processProductList(nested, categoryName);
        }
    }

    private void processProductList(JsonElement categoryElement, String categoryName) {
        JsonArray products = extractProducts(categoryElement);
        if (products == null) return;

        for (JsonElement productElement : products) {
            processProduct(productElement, categoryName);
        }
    }

    private void processProduct(JsonElement productElement, String categoryName) {
        JsonObject json = productElement.getAsJsonObject();

        if (!isValidProduct(json)) return;

        Product product = toProduct(json, categoryName);
        System.out.println("PUBLISHING: " + product.name());
        publish(product);
    }

    private void publish(Product product) {
        publisher.publish(eventTopic, product);
    }

    private Product toProduct(JsonObject json, String category) {
        JsonObject price = json.getAsJsonObject("price_instructions");
        String name = json.get("display_name").getAsString();

        return new Product(
                json.get("id").getAsString(),
                name,
                ProductTextProcessor.normalizeName(name),
                ProductTextProcessor.extractBrand(name),
                category,
                extractUnitPrice(price),
                extractUnitFormat(price),
                extractUnitSize(price),
                isDiscount(price)
        );
    }

    private double extractUnitPrice(JsonObject price) {
        return price.get("unit_price").getAsDouble();
    }

    private String extractUnitFormat(JsonObject price) {
        return price.has("size_format") ? price.get("size_format").getAsString() : "";
    }

    private double extractUnitSize(JsonObject price) {
        return price.has("unit_size") ? price.get("unit_size").getAsDouble() : 1;
    }

    private boolean isDiscount(JsonObject price) {
        return price.has("price_decreased") && price.get("price_decreased").getAsBoolean();
    }

    private boolean isValidProduct(JsonObject json) {
        return json.has("id")
                && json.has("display_name")
                && json.has("price_instructions");
    }

    private boolean isAllowed(String categoryName) {
        return allowedCategories.contains(categoryName);
    }

    private JsonArray fetchRootCategories() throws IOException {
        return parse(send(categoriesPath)).getAsJsonArray("results");
    }

    private JsonArray fetchNestedCategories(JsonObject category) throws IOException {
        return parse(send(categoriesPath + category.get("id").getAsInt()))
                .getAsJsonArray("categories");
    }

    private String send(String path) throws IOException {
        return Jsoup.connect(apiUrl + path)
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .execute()
                .body();
    }

    private JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private JsonArray extractCategories(JsonElement element) {
        return element.getAsJsonObject().getAsJsonArray("categories");
    }

    private JsonArray extractProducts(JsonElement element) {
        return element.getAsJsonObject().getAsJsonArray("products");
    }

    private String extractName(JsonObject json) {
        return json.get("name").getAsString().toLowerCase();
    }
}