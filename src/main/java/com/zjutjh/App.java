package com.zjutjh;

import io.vertx.core.Vertx;

import java.io.IOException;
import java.util.Properties;

public class App {
    private static Properties config;

    public static Properties getConfig() {
        return config;
    }

    public static void main(String[] args) {
        // 读取配置文件
        try {
            config = new Properties();
            config.load(App.class.getResourceAsStream("/properties/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new WeBlogVerticle(), res -> {
            if (res.succeeded()) {
                System.out.println("Deployment id is: " + res.result());
            } else {
                System.out.println("Deployment failed! " + res.cause());
            }
        });
    }
}
