package org.ulpgc.codestormah.mercadona.controller;

import org.ulpgc.codestormah.mercadona.model.Product;
import com.google.gson.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.ulpgc.codestormah.mercadona.model.ProductTextNormalizer;

import java.io.IOException;
import java.util.*;

public class MercadonaFeeder implements ProductFeeder {
    private static final String BASE_URL = "https://tienda.mercadona.es";
    private static final int MAX_ATTEMPTS = 3;
    private static final int TIMEOUT_MS = 15000;

    @Override
    public List<Product> getProducts(int limit) throws IOException {
        List<Product> products = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonElement cat : getCategories()) processCategory(cat, products, ids, limit);
        return products;
    }

    private JsonArray getCategories() throws IOException {
        return getJson("/api/categories/").getAsJsonArray("results");
    }

    private void processCategory(JsonElement category, List<Product> products, Set<String> ids, int limit) throws IOException {
        JsonArray subcategories = category.getAsJsonObject().getAsJsonArray("categories");
        if (subcategories == null) return;
        for (JsonElement subcategory : subcategories) processSubcategory(subcategory, products, ids, limit);
    }

    private void processSubcategory(JsonElement subcategory, List<Product> products, Set<String> ids, int limit) throws IOException {
        JsonObject obj = subcategory.getAsJsonObject();
        JsonArray inner = getJson("/api/categories/" + obj.get("id").getAsInt()).getAsJsonArray("categories");
        if (inner == null) return;
        for (JsonElement i : inner) processInnerCategory(i, obj.get("name").getAsString(), products, ids, limit);
    }

    private void processInnerCategory(JsonElement inner, String category, List<Product> products, Set<String> ids, int limit) {
        JsonArray arr = inner.getAsJsonObject().getAsJsonArray("products");
        if (arr == null) return;
        for (JsonElement p : arr) processProduct(p, category, products, ids, limit);
    }

    private void processProduct(JsonElement elem, String category, List<Product> products, Set<String> ids, int limit) {
        if (limit != -1 && products.size() >= limit) return;
        JsonObject p = elem.getAsJsonObject();
        if (!isValid(p) || !ids.add(getId(p))) return;
        products.add(createProduct(p, category));
    }

    private boolean isValid(JsonObject p) {
        return p.has("id") &&
                p.has("display_name") &&
                p.has("price_instructions") &&
                p.getAsJsonObject("price_instructions").has("unit_price");
    }

    private Product createProduct(JsonObject p, String category) {
        JsonObject price = getPrice(p);

        return new Product(
                getId(p),
                getName(p),
                getNormalizedName(p),
                getBrand(p),
                category,
                getUnitPrice(price),
                getUnit(price),
                getAmount(price),
                isOnOffer(price)
        );
    }

    private String getBrand(JsonObject p) {
        return ProductTextNormalizer.extractBrand(getName(p));
    }

    private String getNormalizedName(JsonObject p) {
        return ProductTextNormalizer.normalizeName(getName(p));
    }

    private String getName(JsonObject p) {
        return p.get("display_name").getAsString();
    }

    private String getId(JsonObject p) {
        return p.get("id").getAsString();
    }

    private double getUnitPrice(JsonObject price) {
        return price.get("unit_price").getAsDouble();
    }

    private double getAmount(JsonObject price) {
        return price.has("unit_size") && !price.get("unit_size").isJsonNull()
                ? price.get("unit_size").getAsDouble()
                : 1;
    }

    private JsonObject getPrice(JsonObject p) {
        return p.getAsJsonObject("price_instructions");
    }

    private String getUnit(JsonObject price) {
        return price.has("size_format") && !price.get("size_format").isJsonNull()
                ? price.get("size_format").getAsString()
                : "";
    }

    private boolean isOnOffer(JsonObject price) {
        return price.has("price_decreased") &&
                !price.get("price_decreased").isJsonNull() &&
                price.get("price_decreased").getAsBoolean();
    }

    private JsonObject getJson(String path) throws IOException {
        int attempts = 0;

        while(attempts < MAX_ATTEMPTS) {
            try {
                Connection.Response res = Jsoup.connect(BASE_URL + path)
                        .ignoreContentType(true)
                        .timeout(TIMEOUT_MS)
                        .header("Accept", "application/json")
                        .header("User-Agent", "Mozilla/5.0")
                        .method(Connection.Method.GET)
                        .execute();
                return JsonParser.parseString(res.body()).getAsJsonObject();
            } catch (java.net.SocketTimeoutException e) { attempts++; }
        }
        throw new IOException("Failed to fetch JSON from " + path);
    }
}
