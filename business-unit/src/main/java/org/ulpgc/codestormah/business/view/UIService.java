package org.ulpgc.codestormah.business.view;

import io.javalin.Javalin;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.codestormah.business.control.ProductStore;
import org.ulpgc.codestormah.business.control.RecommendationStore;
import org.ulpgc.codestormah.business.model.Recommendation;

public class UIService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIService.class);
    private final ProductStore productStore;
    private final RecommendationStore recommendationStore;
    private final int port;

    public UIService(ProductStore productStore, RecommendationStore recommendationStore, int port) {
        this.productStore = productStore;
        this.recommendationStore = recommendationStore;
        this.port = port;
    }

    public void start() {
        Javalin app = Javalin.create(c -> c.plugins.enableCors(cors -> cors.add(CorsPluginConfig::anyHost))).start(port);
        setupRoutes(app);
        LOGGER.info("🚀 API REST started on port {}", port);
    }

    private void setupRoutes(Javalin app) {
        setupBasicRoutes(app);
        setupProductRoutes(app);
        setupAdvancedRoutes(app);
    }

    private void setupBasicRoutes(Javalin app) {
        app.get("/api/health", ctx -> ctx.result("Business Unit running"));
        app.get("/api/categories", ctx -> ctx.json(productStore.getCategories()));
    }

    private void setupProductRoutes(Javalin app) {
        app.get("/api/products/{cat}", ctx -> ctx.json(productStore.getProductsByCategory(ctx.pathParam("cat"))));
        app.get("/api/products/{cat}/cheapest", ctx -> ctx.json(productStore.getCheapestProduct(ctx.pathParam("cat"))));
        app.get("/api/products/{cat}/expensive", ctx -> ctx.json(productStore.getMostExpensiveProduct(ctx.pathParam("cat"))));
    }

    private void setupAdvancedRoutes(Javalin app) {
        app.get("/api/recommendation/{cat}", this::handleRecommendation);
        app.get("/api/history/{source}/{id}", this::handleHistory);
    }

    private void handleRecommendation(io.javalin.http.Context ctx) {
        Recommendation rec = recommendationStore.get(ctx.pathParam("cat"));
        if (rec == null) ctx.status(404).result("Sin recomendación");
        else ctx.json(rec);
    }

    private void handleHistory(io.javalin.http.Context ctx) {
        ctx.json(productStore.getProductHistory(ctx.pathParam("id"), ctx.pathParam("source")));
    }
}