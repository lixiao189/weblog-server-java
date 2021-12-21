package com.zjutjh.controller;

import com.zjutjh.mapper.basic.IndexJsonData;
import com.zjutjh.mapper.basic.TestJsonData;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class Basic {
    public static void IndexHandler(RoutingContext context) {
        JsonObject data = JsonObject.mapFrom(new IndexJsonData());
        context.json(data);
    }

    public static void testJsonHandler(RoutingContext context) {
        // 解析 json 数据
        TestJsonData data;
        try {
            data = context.getBodyAsJson().mapTo(TestJsonData.class);
            System.out.println(data.getMsg());
        } catch (IllegalArgumentException e) {
            context.end("参数错误");
            return;
        }

        context.end("发送成功");
    }
}
