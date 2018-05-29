package com.overtureone.formalapi;

import com.overtureone.entity.Product;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.ArrayList;
import java.util.List;

public class FormalAPIVerticle extends AbstractVerticle {

    private static final List<Product> listProducts;
    private static final Logger LOGGER;

    static {
        listProducts = new ArrayList<>();
        Product p1 = new Product("1", "Mac Mini");
        Product p2 = new Product("2", "4K TV");
        listProducts.add(p1);
        listProducts.add(p2);

        LOGGER = LoggerFactory.getLogger(FormalAPIVerticle.class);
    }

    public static void main(String... args) {

        Vertx vertx = Vertx.vertx();

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
        configRetriever.getConfig(config -> {

            if (config.succeeded()) {
                JsonObject configJson = config.result();
                DeploymentOptions options = new DeploymentOptions().setConfig(configJson);
                vertx.deployVerticle(new FormalAPIVerticle(), options); //Deploy the Verticle
            }
        });
    }

    @Override
    public void start() {
        LOGGER.info("Verticle Started");

        //API Routing
        Router router = Router.router(vertx);
        router.route("/api*").handler(this::defaultProcessorForAllAPI);
        router.route("/api/v1/products*").handler(BodyHandler.create()); //Ensures JSON Bodies in POST and PUTs work

        router.get("/api/v1/products").handler(this::getAllProducts);
        router.get("/api/v1/products/:id").handler(this::getProductByID);
        router.post("/api/v1/products").handler(this::addProduct);
        router.put("/api/v1/products/:id").handler(this::updateProductsByID);
        router.delete("/api/v1/products/:id").handler(this::deleteProductByID);

        //Setup the Server
        router.route().handler(StaticHandler.create().setCachingEnabled(false));
        vertx.createHttpServer().requestHandler(router::accept).listen(config().getInteger("http.port"), asyncResult -> {
            if (asyncResult.succeeded()) {
                LOGGER.info("HTTP server running on port " + config().getInteger("http.port"));
            }
            else {
                LOGGER.error("Could not start a HTTP server", asyncResult.cause());
            }
        });
    }

    @Override
    public void stop() {
        LOGGER.info("Verticle Stopped");

    }

    //Called for ALL API Operations
    private void defaultProcessorForAllAPI(RoutingContext routingContext) {

        String authToken = routingContext.request().headers().get("AuthToken");

        if (authToken == null || !authToken.equals("123")) {
            LOGGER.info("Failed basic auth check");

            routingContext.response()
                    .setStatusCode(401)
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .end(Json.encodePrettily(new JsonObject().put("error", "Not Authorized to use APIs")));
        }
        else {
            LOGGER.info("Passed basic auth check");

            routingContext.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            routingContext.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE");

            routingContext.next(); //Call the next matching route
        }
    }

    //Methods to support all REST calls--------------------------------------------------------------------------
    private void getAllProducts(RoutingContext routingContext) {

        JsonObject responseJson = new JsonObject();
        responseJson.put("products", listProducts);

        routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(Json.encodePrettily(responseJson));
    }


    private void getProductByID(RoutingContext routingContext) {

        //Get the parameter
        final String productID = routingContext.request().getParam("id");

        Product product = null;
        for (Product p : listProducts) {
            if (p.getNumber().equals(productID)) {
                product = p;
            }
        }

        //Did we find the product
        if (product != null) {
            routingContext.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(Json.encodePrettily(product));
        }
        else {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json")
                    .end(Json.encodePrettily(""));
        }
    }

    private void addProduct(RoutingContext routingContext) {

        //Get the JSON body
        JsonObject jsonBody = routingContext.getBodyAsJson();
        LOGGER.info("METHOD addProduct: " + jsonBody);

        String desc = jsonBody.getString("description");
        String id = jsonBody.getString("number");

        Product newItem = new Product(id, desc);
        listProducts.add(newItem);

        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json")
                .end(Json.encodePrettily(newItem));
    }

    private void updateProductsByID(RoutingContext routingContext) {

        String id = routingContext.request().getParam("id");

        //Get the JSON body
        JsonObject jsonBody = routingContext.getBodyAsJson();
        String desc = jsonBody.getString("description");

        //Find the item
        Product prod = null;
        for (int i = 0; i < listProducts.size(); i++) {
            prod = listProducts.get(i);
            if (prod.getNumber().equals(id)) {
                prod.setDescription(desc);
            }
        }

        if (prod != null) {
            routingContext.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(Json.encodePrettily(prod));
        }
        else {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json")
                    .end();
        }

    }

    private void deleteProductByID(RoutingContext routingContext) {

        //Get the parameter
        final String productID = routingContext.request().getParam("id");

        listProducts.removeIf(p -> p.getNumber().equals(productID));

        routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end();

    }
}
