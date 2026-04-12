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
        return loadProducts(limit);
    }

    private List<Product> loadProducts(int limit) throws IOException {
        ProductContext ctx = new ProductContext(limit);

        for (JsonElement cat : getCategories()) {
            processCategory(cat, ctx);
        }

        return limit == -1
                ? ctx.getProducts()
                : ctx.getProducts().subList(0, Math.min(limit, ctx.getProducts().size()));
    }

    private void processCategory(JsonElement category, ProductContext ctx) throws IOException {
        JsonArray subcategories = extractSubcategoriesArray(category);
        if (subcategories == null) return;

        for (JsonElement sub : subcategories) {
            processSubcategory(sub, ctx);
        }
    }

    private void processSubcategory(JsonElement subcategory, ProductContext ctx) throws IOException {
        JsonObject obj = subcategory.getAsJsonObject();
        JsonArray inner = fetchInnerCategories(obj);

        if (inner == null) return;

        String categoryName = obj.get("name").getAsString();

        for (JsonElement i : inner) {
            processProductList(i, categoryName, ctx);
        }
    }

    private void processProductList(JsonElement inner, String category, ProductContext ctx) {
        JsonArray productsArray = extractProductsArray(inner);
        if (productsArray == null) return;

        for (JsonElement p : productsArray) {
            processProduct(p, category, ctx);
        }
    }

    private void processProduct(JsonElement elem, String category, ProductContext ctx) {

        JsonObject product = elem.getAsJsonObject();
        String id = getId(product);

        if (!isValidProduct(product) || ctx.isDuplicateProduct(id)) return;

        ctx.addProduct(mapToProduct(product, category));
    }

    private Product mapToProduct(JsonObject p, String category) {
        JsonObject price = getPrice(p);

        return new Product(
                getId(p),
                getName(p),
                normalizeName(p),
                extractBrand(p),
                category,
                getUnitPrice(price),
                getUnit(price),
                getAmount(price),
                isOnOffer(price)
        );
    }

    private String extractBrand(JsonObject p) {
        return ProductTextNormalizer.extractBrand(getName(p));
    }

    private String normalizeName(JsonObject p) {
        return ProductTextNormalizer.normalizeName(getName(p));
    }

    private JsonArray getCategories() throws IOException {
        return extractResultsArray(executeHttpRequest("/api/categories/"));
    }

    private boolean isValidProduct(JsonObject p) {
        return p.has("id") &&
                p.has("display_name") &&
                p.has("price_instructions") &&
                p.getAsJsonObject("price_instructions").has("unit_price");
    }

    private JsonArray extractResultsArray(JsonObject json) {
        return json.getAsJsonArray("results");
    }

    private String getName(JsonObject p) {
        return p.get("display_name").getAsString();
    }

    private String getId(JsonObject p) {
        return p.get("id").getAsString();
    }

    private JsonObject getPrice(JsonObject p) {
        return p.getAsJsonObject("price_instructions");
    }

    private double getUnitPrice(JsonObject price) {
        return price.get("unit_price").getAsDouble();
    }

    private double getAmount(JsonObject price) {
        return price.has("unit_size") && !price.get("unit_size").isJsonNull()
                ? price.get("unit_size").getAsDouble()
                : 1;
    }

    private String getUnit(JsonObject price) {
        return price.has("size_format") && !price.get("size_format").isJsonNull()
                ? price.get("size_format").getAsString()
                : "";
    }

    private boolean isOnOffer(JsonObject price) {
        return price.has("price_decreased")
                && !price.get("price_decreased").isJsonNull()
                && price.get("price_decreased").getAsBoolean();
    }

    private JsonArray fetchInnerCategories(JsonObject obj) throws IOException {
        return executeHttpRequest("/api/categories/" + obj.get("id").getAsInt())
                .getAsJsonArray("categories");
    }

    private JsonArray extractSubcategoriesArray(JsonElement category) {
        return category.getAsJsonObject().getAsJsonArray("categories");
    }

    private JsonArray extractProductsArray(JsonElement inner) {
        return inner.getAsJsonObject().getAsJsonArray("products");
    }

    private JsonObject fetchJson(String path) throws IOException {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return parseResponse(sendRequest(path));
            } catch (java.net.SocketTimeoutException ignored) {}
        }
        throw new IOException("Failed to fetch JSON from " + path);
    }

    private JsonObject executeHttpRequest(String path) throws IOException {
        return fetchJson(path);
    }

    private JsonObject parseResponse(Connection.Response res) {
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private Connection.Response sendRequest(String path) throws IOException {
        return Jsoup.connect(BASE_URL + path)
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .method(Connection.Method.GET)
                .execute();
    }

    private static class ProductContext {
        private final List<Product> products = new ArrayList<>();
        private final Set<String> ids = new HashSet<>();
        private final int limit;

        ProductContext(int limit) {
            this.limit = limit;
        }

        boolean isDuplicateProduct(String id) {
            return !ids.add(id);
        }

        void addProduct(Product product) {
            products.add(product);
        }

        List<Product> getProducts() {
            return products;
        }
    }
}
