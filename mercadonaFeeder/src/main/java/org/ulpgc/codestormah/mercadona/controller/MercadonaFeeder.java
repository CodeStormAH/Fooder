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

    public MercadonaFeeder(String apiUrl, String categoriesPath, Set<String> allowedCategories) {
        this.apiUrl = apiUrl;
        this.categoriesPath = categoriesPath;
        this.allowedCategories = allowedCategories;
    }

    @Override
    public List<Product> run(int maxProducts) throws IOException {
        List<Product> products = new ArrayList<>();
        for (JsonElement root : fetchRootCategories()) processRootCategory(root, products);
        return products;
    }

    private JsonArray fetchRootCategories() throws IOException {
        return parse(send(categoriesPath)).getAsJsonArray("results");
    }

    private void processRootCategory(JsonElement root, List<Product> products) throws IOException {
        JsonArray subs = extractCategories(root);
        if (subs != null) for (JsonElement sub : subs) processSubcategory(sub, products);
    }

    private void processSubcategory(JsonElement subElement, List<Product> out) throws IOException {
        JsonObject sub = subElement.getAsJsonObject();
        String name = extractName(sub);
        if (isAllowed(name)) processNestedSafely(fetchNestedCategories(sub), name, out);
    }

    private boolean isAllowed(String categoryName) {
        return allowedCategories.contains(categoryName);
    }

    private JsonArray fetchNestedCategories(JsonObject category) throws IOException {
        String path = categoriesPath + category.get("id").getAsInt();
        return parse(send(path)).getAsJsonArray("categories");
    }

    private void processNestedSafely(JsonArray nested, String name, List<Product> out) {
        if (nested != null) for (JsonElement e : nested) processProductList(e, name, out);
    }

    private void processProductList(JsonElement element, String name, List<Product> out) {
        JsonArray items = extractProducts(element);
        if (items != null) for (JsonElement item : items) processProduct(item, name, out);
    }

    private void processProduct(JsonElement element, String name, List<Product> out) {
        JsonObject json = element.getAsJsonObject();
        if (isValidProduct(json)) addProductIfCategoryMatches(json, name, out);
    }

    private boolean isValidProduct(JsonObject json) {
        return json.has("id") && json.has("display_name") && json.has("price_instructions");
    }

    private void addProductIfCategoryMatches(JsonObject json, String rawName, List<Product> out) {
        String prodName = extractProductName(json);
        String finalCategory = resolveCategory(rawName, prodName);
        if (finalCategory != null) out.add(toProduct(json, finalCategory));
    }

    private String extractProductName(JsonObject json) {
        return json.get("display_name").getAsString().toLowerCase();
    }

    private String resolveCategory(String cat, String p) {
        if ("sidra y cava".equals(cat)) return findMatch(p, "sidra", "cava");
        if ("tónica y bitter".equals(cat)) return findMatch(p, "tónica", "bitter");
        if ("isotónico y energético".equals(cat)) return mapEnergy(p);
        if ("refresco de naranja y de limón".equals(cat)) return mapSoda(p);
        return cat;
    }

    private String findMatch(String p, String k1, String k2) {
        if (p.contains(k1)) return k1;
        return p.contains(k2) ? k2 : null;
    }

    private String mapEnergy(String p) {
        if (p.contains("isotónica")) return "isotónico";
        return p.contains("energética") ? "energético" : null;
    }

    private String mapSoda(String p) {
        if (p.contains("naranja")) return "refresco de naranja";
        return p.contains("limón") ? "refresco de limón" : null;
    }

    private Product toProduct(JsonObject json, String category) {
        JsonObject price = json.getAsJsonObject("price_instructions");
        return createProductEntity(json, category, price);
    }

    private Product createProductEntity(JsonObject json, String cat, JsonObject price) {
        String name = json.get("display_name").getAsString();
        String n = ProductTextProcessor.normalizeName(name);
        String b = ProductTextProcessor.extractBrand(name);
        return new Product(json.get("id").getAsString(), name, n, b, cat,
                extractUnitPrice(price), extractUnitFormat(price), extractUnitSize(price), isDiscount(price));
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

    private String send(String path) throws IOException {
        return Jsoup.connect(apiUrl + path).ignoreContentType(true).timeout(TIMEOUT_MS).execute().body();
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