package com.zjutjh.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class Basic {
    public static void IndexHandler(RoutingContext context) {
        JsonObject data = new JsonObject();
        data.put("hello", "world");
        context.json(data);
    }

    public static void testJsonHandler(RoutingContext context) {
        JsonObject data = context.getBodyAsJson();
        System.out.println(data.getString("hello"));
        context.end("发送成功");
    }
}
