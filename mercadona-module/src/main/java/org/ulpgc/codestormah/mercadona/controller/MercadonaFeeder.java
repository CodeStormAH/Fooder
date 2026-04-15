package org.ulpgc.codestormah.mercadona.controller;

import org.ulpgc.codestormah.mercadona.model.Product;
import com.google.gson.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.ulpgc.codestormah.mercadona.model.ProductTextProcessor;

import java.io.IOException;
import java.util.*;

public class MercadonaFeeder implements ProductFeeder {
    private static final int MAX_ATTEMPTS = 3;
    private static final int TIMEOUT_MS = 15000;
    private final String categoriesPath;
    private final String apiUrl;

    public MercadonaFeeder(String apiUrl, String categoriesPath) {
        this.apiUrl = apiUrl;
        this.categoriesPath = categoriesPath;
    }

    @Override
    public List<Product> getProducts(int maxProducts) throws IOException {
        return loadProducts(maxProducts);
    }

    private List<Product> loadProducts(int maxProducts) throws IOException {
        ProductAccumulator accumulator = new ProductAccumulator(maxProducts);

        for (JsonElement categoryElement : fetchCategories()) {
            processCategory(categoryElement, accumulator);
        }

        List<Product> products = accumulator.getAllProducts();

        return maxProducts == -1
                ? products
                : products.subList(0, Math.min(maxProducts, products.size()));
    }

    private void processCategory(JsonElement categoryElement, ProductAccumulator accumulator) throws IOException {
        JsonArray subcategories = extractSubcategories(categoryElement);
        if (subcategories == null) return;

        for (JsonElement subcategoryElement : subcategories) {
            processSubcategory(subcategoryElement, accumulator);
        }
    }

    private void processSubcategory(JsonElement subcategoryElement, ProductAccumulator accumulator) throws IOException {
        JsonObject subcategoryObject = subcategoryElement.getAsJsonObject();

        JsonArray nestedCategories  = fetchNestedCategories(subcategoryObject);
        if (nestedCategories  == null) return;

        String categoryName = subcategoryObject.get("name").getAsString();

        for (JsonElement nestedCategory : nestedCategories) {
            processProductList(nestedCategory, categoryName, accumulator);
        }
    }

    private void processProductList(JsonElement categoryElement, String categoryName, ProductAccumulator accumulator) {
        JsonArray productsArray = extractProducts(categoryElement);
        if (productsArray == null) return;

        for (JsonElement productElement : productsArray) {
            processProduct(productElement, categoryName, accumulator);
        }
    }

    private void processProduct(JsonElement productElement, String categoryName, ProductAccumulator accumulator) {
        JsonObject productJson = productElement.getAsJsonObject();
        String productId = extractProductId(productJson);

        if (!isValidProduct(productJson)) return;

        boolean added = accumulator.add(mapToProduct(productJson, categoryName));
        if (!added) return;
    }

    private Product mapToProduct(JsonObject productJson, String categoryName) {
        JsonObject priceJson = extractPriceInfo(productJson);

        return new Product(
                extractProductId(productJson),
                extractProductName(productJson),
                normalizeProductName(productJson),
                extractBrand(productJson),
                categoryName,
                extractUnitPrice(priceJson),
                extractUnit(priceJson),
                extractAmount(priceJson),
                isOnOffer(priceJson)
        );
    }

    private String extractBrand(JsonObject productJson) {
        return ProductTextProcessor.extractBrand(extractProductName(productJson));
    }

    private String normalizeProductName(JsonObject productJson) {
        return ProductTextProcessor.normalizeName(extractProductName(productJson));
    }

    private boolean isValidProduct(JsonObject productJson) {
        return productJson.has("id") &&
                productJson.has("display_name") &&
                productJson.has("price_instructions") &&
                productJson.getAsJsonObject("price_instructions").has("unit_price");
    }

    private JsonArray fetchCategories() throws IOException {
        return extractResultsArray(fetchJson(categoriesPath));
    }

    private JsonArray fetchNestedCategories(JsonObject categoryJson) throws IOException {
        return fetchJson(categoriesPath + categoryJson.get("id").getAsInt())
                .getAsJsonArray("categories");
    }

    private JsonObject fetchJson(String path) throws IOException {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return parseResponse(sendRequest(path));
            } catch (java.net.SocketTimeoutException ignored) {}
        }
        throw new IOException("Failed to fetch JSON from " + path);
    }

    private Connection.Response sendRequest(String path) throws IOException {
        return Jsoup.connect(apiUrl + path)
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .method(Connection.Method.GET)
                .execute();
    }

    private JsonObject parseResponse(Connection.Response res) {
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private JsonArray extractResultsArray(JsonObject responseJson) {
        return responseJson.getAsJsonArray("results");
    }

    private String extractProductName(JsonObject productJson) {
        return productJson.get("display_name").getAsString();
    }

    private String extractProductId(JsonObject productJson) {
        return productJson.get("id").getAsString();
    }

    private JsonObject extractPriceInfo(JsonObject productJson) {
        return productJson.getAsJsonObject("price_instructions");
    }

    private double extractUnitPrice(JsonObject priceJson) {
        return priceJson.get("unit_price").getAsDouble();
    }

    private double extractAmount(JsonObject priceJson) {
        return priceJson.has("unit_size") && !priceJson.get("unit_size").isJsonNull()
                ? priceJson.get("unit_size").getAsDouble()
                : 1;
    }

    private String extractUnit(JsonObject priceJson) {
        return priceJson.has("size_format") && !priceJson.get("size_format").isJsonNull()
                ? priceJson.get("size_format").getAsString()
                : "";
    }

    private boolean isOnOffer(JsonObject priceJson) {
        return priceJson.has("price_decreased")
                && !priceJson.get("price_decreased").isJsonNull()
                && priceJson.get("price_decreased").getAsBoolean();
    }

    private JsonArray extractSubcategories(JsonElement categoryElement) {
        return categoryElement.getAsJsonObject().getAsJsonArray("categories");
    }

    private JsonArray extractProducts(JsonElement categoryElement) {
        return categoryElement.getAsJsonObject().getAsJsonArray("products");
    }

    private static class ProductAccumulator {
        private final List<Product> products = new ArrayList<>();
        private final Set<String> ids = new HashSet<>();
        private final int limit;

        ProductAccumulator(int limit) {
            this.limit = limit;
        }

        boolean add(Product product) {
            if (!ids.add(product.id())) return false;
            if (limit != -1 && products.size() >= limit) return false;

            products.add(product);
            return true;
        }

        List<Product> getAllProducts() {
            return products;
        }
    }
}
