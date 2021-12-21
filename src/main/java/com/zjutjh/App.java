package com.zjutjh;

import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;

import java.io.IOException;
import java.util.Properties;

public class App {
    private static Properties config;
    private static MySQLPool mySQLClient;

    public static MySQLPool getMySQLClient() {
        return mySQLClient;
    }

    public static Properties getConfig() {
        return config;
    }

    public static void main(String[] args) {
        // 生成框架
        Vertx vertx = Vertx.vertx();

        // 读取配置文件
        try {
            config = new Properties();
            config.load(App.class.getResourceAsStream("/properties/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 初始化数据库
        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                .setPort(Integer.parseInt(config.getProperty("db_port")))
                .setHost(config.getProperty("db_host"))
                .setDatabase(config.getProperty("db_name"))
                .setUser(config.getProperty("db_user"))
                .setPassword(config.getProperty("db_pass"));
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(100);

        mySQLClient = MySQLPool.pool(vertx, connectOptions, poolOptions);

        vertx.deployVerticle(new WeBlogVerticle(), res -> {
            if (res.succeeded()) {
                System.out.println("Deployment id is: " + res.result());
            } else {
                System.out.println("Deployment failed! " + res.cause());
            }
        });
    }
}
