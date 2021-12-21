package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.example.controller.Basic;

import java.io.IOException;
import java.util.Properties;

class MyVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.get("/hello").handler(Basic::HelloHandler);

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

public class App {
    public static Properties config;

    public static void main(String[] args) {
        // 读取配置文件
        try {
            config = new Properties();
            config.load(App.class.getResourceAsStream("/properties/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MyVerticle(), res -> {
            if (res.succeeded()) {
                System.out.println("Deployment id is: " + res.result());
            } else {
                System.out.println("Deployment failed! " + res.cause());
            }
        });
    }
}
