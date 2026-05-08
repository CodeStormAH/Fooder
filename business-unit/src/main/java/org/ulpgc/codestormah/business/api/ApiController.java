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

        app.get("/health", ctx -> ctx.result("Business Unit running"));
        app.get("/categories", ctx -> ctx.json(productStore.getCategories()));
        app.get("/products/category/{category}", ctx -> ctx.json(productStore.getProductsByCategory(ctx.pathParam("category"))));
        app.get("/products/category/{category}/cheapest", ctx -> ctx.json(productStore.getCheapestProduct(ctx.pathParam("category"))));
        app.get("/products/category/{category}/expensive", ctx -> ctx.json(productStore.getMostExpensiveProduct(ctx.pathParam("category"))));

        System.out.println("🚀 API REST iniciada en http://localhost:" + port);
    }
}
