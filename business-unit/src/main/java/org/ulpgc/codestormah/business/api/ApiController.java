package org.ulpgc.codestormah.business.api;

import org.ulpgc.codestormah.business.datamart.ProductStore;
import io.javalin.Javalin;

public class ApiController {
    private final ProductStore productStore;
    private final int port;

    public ApiController(ProductStore productStore, int port) {
        this.productStore = productStore;
        this.port = port;
    }

    public void start() {
        Javalin app = Javalin.create().start(port);

        // CLEAN CODE: Todas las rutas bajo el estándar /api/...
        app.get("/api/health", ctx -> ctx.result("Business Unit running"));
        app.get("/api/categories", ctx -> ctx.json(productStore.getCategories()));
        app.get("/api/products/{category}", ctx -> ctx.json(productStore.getProductsByCategory(ctx.pathParam("category"))));
        app.get("/api/products/{category}/cheapest", ctx -> ctx.json(productStore.getCheapestProduct(ctx.pathParam("category"))));
        app.get("/api/products/{category}/expensive", ctx -> ctx.json(productStore.getMostExpensiveProduct(ctx.pathParam("category"))));
        app.get("/api/recommendation/{category}", ctx -> ctx.json(productStore.getRecommendation(ctx.pathParam("category"))));
        app.get("/api/history/{source}/{id}", ctx -> ctx.json(productStore.getProductHistory(ctx.pathParam("id"), ctx.pathParam("source"))));

        System.out.println("🚀 API REST iniciada en http://localhost:" + port);
    }
}
