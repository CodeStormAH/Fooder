package org.ulpgc.codestormah.business.view;

import org.ulpgc.codestormah.business.control.ProductStore;
import io.javalin.Javalin;
import org.ulpgc.codestormah.business.control.RecommendationStore;
import org.ulpgc.codestormah.business.model.Recommendation;

public class ApiController {
    private final ProductStore productStore;
    private final int port;
    private final RecommendationStore recommendationStore;

    public ApiController(ProductStore productStore, RecommendationStore recommendationStore, int port) {
        this.productStore = productStore;
        this.recommendationStore = recommendationStore;
        this.port = port;
    }

    public void start() {
        Javalin app = Javalin.create(config -> {
            // Sintaxis para Javalin 5
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.anyHost();
                });
            });
        }).start(port);

        app.get("/api/health", ctx -> ctx.result("Business Unit running"));
        app.get("/api/categories", ctx -> ctx.json(productStore.getCategories()));
        app.get("/api/products/{category}", ctx -> ctx.json(productStore.getProductsByCategory(ctx.pathParam("category"))));
        app.get("/api/products/{category}/cheapest", ctx -> ctx.json(productStore.getCheapestProduct(ctx.pathParam("category"))));
        app.get("/api/products/{category}/expensive", ctx -> ctx.json(productStore.getMostExpensiveProduct(ctx.pathParam("category"))));
        app.get("/api/recommendation/{category}", ctx -> {
            Recommendation rec = recommendationStore.get(ctx.pathParam("category"));
            if (rec == null) ctx.status(404).result("Sin recomendación para esa categoría");
            else ctx.json(rec);
        });
        app.get("/api/products", ctx ->
                ctx.json(productStore.getAllProducts()));
        app.get("/api/history/{source}/{id}", ctx -> ctx.json(productStore.getProductHistory(ctx.pathParam("id"), ctx.pathParam("source"))));

        System.out.println("🚀 API REST iniciada en http://localhost:" + port);
    }
}
