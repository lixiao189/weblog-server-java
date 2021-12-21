package com.zjutjh;

import com.zjutjh.controller.Basic;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class WeBlogVerticle extends AbstractVerticle {
    public void initRouter(Router router) {
        router.get("/").handler(Basic::IndexHandler);
        router.post("/json").handler(BodyHandler.create()).handler(Basic::testJsonHandler);
    }

    @Override
    public void start(Promise<Void> startPromise) {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        initRouter(router);

        // Now bind the server:
        server.requestHandler(router).listen(8085, res -> {
            if (res.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(res.cause());
            }
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
