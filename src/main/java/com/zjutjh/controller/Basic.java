package com.zjutjh.controller;

import com.zjutjh.Helper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Basic {
    public static void IndexHandler(RoutingContext context) {
        ArrayList<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> tmpMap = new HashMap<>();
        tmpMap.put("title", "reoreoreo");
        tmpMap.put("content", "reoreoreoreoreo");
        data.add(tmpMap);
        data.add(tmpMap);

        JsonObject respData = new JsonObject(Helper.respData(0, "成功", data));
        context.json(respData);
    }

    public static void testJsonHandler(RoutingContext context) {
        // 解析 json 数据
        JsonObject data;
        try {
            data = context.getBodyAsJson();

        } catch (IllegalArgumentException e) {
            context.end("参数错误");
            return;
        }

        context.end("发送成功");
    }
}
