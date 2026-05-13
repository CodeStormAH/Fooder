package org.ulpgc.codestormah.mercadona.controller;

import com.google.gson.*;
import org.jsoup.Jsoup;
import org.ulpgc.codestormah.mercadona.model.Product;
import org.ulpgc.codestormah.mercadona.model.ProductTextProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MercadonaFeeder implements ProductFeeder {

    private static final int TIMEOUT_MS = 15000;

    private final String apiUrl;
    private final String categoriesPath;
    private final Set<String> allowedCategories;

    public MercadonaFeeder(
            String apiUrl,
            String categoriesPath,
            Set<String> allowedCategories
    ) {
        this.apiUrl = apiUrl;
        this.categoriesPath = categoriesPath;
        this.allowedCategories = allowedCategories;
    }

    @Override
    public List<Product> run(int maxProducts) throws IOException {
        List<Product> products = new ArrayList<>();

        for (JsonElement rootCategory : fetchRootCategories()) {
            processRootCategory(rootCategory, products);
        }

        return products;
    }

    private void processRootCategory(JsonElement root, List<Product> products) throws IOException {
        JsonArray subcategories = extractCategories(root);
        if (subcategories == null) return;

        for (JsonElement sub : subcategories) {
            processSubcategory(sub, products);
        }
    }

    private void processSubcategory(JsonElement subcategoryElement, List<Product> products) throws IOException {
        JsonObject subcategory = subcategoryElement.getAsJsonObject();
        String categoryName = extractName(subcategory);

        if (!isAllowed(categoryName)) return;

        JsonArray nestedCategories = fetchNestedCategories(subcategory);
        if (nestedCategories == null) return;

        for (JsonElement nested : nestedCategories) {
            processProductList(nested, categoryName, products);
        }
    }

    private void processProductList(JsonElement categoryElement, String categoryName, List<Product> products) {
        JsonArray items = extractProducts(categoryElement);
        if (items == null) return;

        for (JsonElement productElement : items) {
            processProduct(productElement, categoryName, products);
        }
    }

    private void processProduct(JsonElement productElement, String categoryName, List<Product> products) {

        JsonObject json = productElement.getAsJsonObject();

        if (!isValidProduct(json)) return;

        String productName = json.get("display_name")
                .getAsString()
                .toLowerCase();

        String finalCategory = resolveCategory(categoryName, productName);

        if (finalCategory != null) {
            products.add(toProduct(json, finalCategory));
        }
    }

    private String resolveCategory(String categoryName, String productName) {

        return switch (categoryName) {

            case "sidra y cava" -> {
                if (productName.contains("sidra")) yield "sidra";
                if (productName.contains("cava")) yield "cava";
                yield null;
            }

            case "tónica y bitter" -> {
                if (productName.contains("tónica")) yield "tónica";
                if (productName.contains("bitter")) yield "bitter";
                yield null;
            }

            case "isotónico y energético" -> {
                if (productName.contains("isotónica")) yield "isotónico";
                if (productName.contains("energética")) yield "energético";
                yield null;
            }

            case "refresco de naranja y limón" -> {
                if (productName.contains("Refresco de naranja")) yield "refresco de naranja";
                if (productName.contains("Refresco de limón")) yield "refresco de limón";
                yield null;
            }

            default -> categoryName;
        };
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