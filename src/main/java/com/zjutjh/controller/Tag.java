package com.zjutjh.controller;

import com.zjutjh.App;
import com.zjutjh.Helper;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Tag {
    /**
     * 获取标签相关的自动补全
     *
     * @param context 框架上下文
     */
    public static void tagAutoComplete(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        String name = body.getString("name"); // 获取补全名称

        // 搜索可能的 tag 列表
        String queryTagListStmt = "select * from tags where match(`name`) against (?)";
        App.getMySQLClient().preparedQuery(queryTagListStmt).execute(Tuple.of(name)).compose(ar -> {
            if (ar.size() == 0) {
                context.json(new JsonObject(Helper.respData(1, "数据为空", null)));
                return Future.succeededFuture();
            }

            final ArrayList<Map<String, Object>> tagList = new ArrayList<>();

            for (Row row : ar) {
                final Map<String, Object> tagItem = new HashMap<>();
                tagItem.put("name", row.getString("name"));
                tagList.add(tagItem);
            }

            context.json(new JsonObject(Helper.respData(0, "获取成功", tagList)));
            return Future.succeededFuture();
        }).onFailure(cause -> {
            System.out.println(cause.getMessage());
            context.end();
        });
    }
}
