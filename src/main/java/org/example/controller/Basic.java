package org.example.controller;

import io.vertx.ext.web.RoutingContext;
import org.example.App;

public class Basic {
    public static void HelloHandler(RoutingContext context) {
        context.response().end(App.getConfig().getProperty("db_user"));
    }
}
