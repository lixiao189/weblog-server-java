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
                tagItem.put("id", row.getInteger("id"));
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

    /**
     * 获取标签下帖子列表
     *
     * @param context 框架上下文
     */
    public static void getPostList(RoutingContext context) {
        // 解析路径参数
        int tagID = Integer.parseInt(context.pathParam("tagID"));
        int page = Integer.parseInt(context.pathParam("page"));

        // 获取 tag 下的所有帖子
        final ArrayList<Map<String, Object>> postList = new ArrayList<>();
        String queryPostListStmt = "SELECT posts.* FROM tags " +
                "INNER JOIN post_to_tag INNER JOIN posts " +
                "on tags.id = post_to_tag.tag_id and post_to_tag.post_id = posts.id " +
                "WHERE tags.id = ? " +
                "order by tags.created_at desc " +
                "limit 20 offset ?;";
        App.getMySQLClient().preparedQuery(queryPostListStmt).execute(Tuple.of(tagID, (page - 1) * 20)).compose(ar -> {
            if (ar.size() == 0) {
                context.json(new JsonObject(Helper.respData(2, "内容为空", null)));
                return Future.failedFuture("内容为空");
            } else {
                Helper.getPostListData(ar, postList);
                return App.getMySQLClient().preparedQuery(queryPostListStmt).execute(Tuple.of(tagID, page * 20));
            }
        }).compose(ar -> {
            final Map<String, Object> data = Helper.listRespData(ar, postList);
            context.json(new JsonObject(Helper.respData(0, "获取成功", data)));
            return Future.succeededFuture();
        });
    }

    /**
     * 获取热门前 20 个标签
     *
     * @param context 框架请求上下文
     */
    public static void getHotTag(RoutingContext context) {
        // 获取回帖数最多的 20 个 tag
        final String queryHotTagStmt = "select  * from tags order by post_num desc limit 20";
        App.getMySQLClient().preparedQuery(queryHotTagStmt).execute().compose(ar -> {
            ArrayList<JsonObject> tagList = new ArrayList<>();
            for (Row row : ar)
                tagList.add(row.toJson());
            context.json(new JsonObject(Helper.respData(0, "获取成功", tagList)));
            return Future.succeededFuture();
        });
    }
}
