package com.zjutjh.controller;

import com.zjutjh.App;
import com.zjutjh.Helper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.HashMap;
import java.util.Map;

public class Post {
    public static void createPost(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        Session session = context.session();

        int senderID = session.get("id");
        String name = session.get("username");
        String title = body.getString("title");
        String content = body.getString("content");

        String insertPostStmt = "insert into posts (sender_id, sender_name, title, content, is_reported) values (?, ?, ?, ?, 0)";
        App.getMySQLClient().preparedQuery(insertPostStmt).execute(Tuple.of(senderID, name, title, content),
                ar -> context.json(new JsonObject(Helper.respData(0, "发送成功", null))));
    }

    public static void getPost(RoutingContext context) {
        int id = Integer.parseInt(context.pathParam("id"));

        String queryPostStmt = "select * from posts where id = ?";
        App.getMySQLClient().preparedQuery(queryPostStmt).execute(Tuple.of(id), ar -> {
            RowSet<Row> rows = ar.result();
            if (rows.size() == 0) {
                context.json(new JsonObject(Helper.respData(2, "帖子不存在", null)));
                return;
            }

            Map<String, Object> result = new HashMap<>();
            for (Row row : rows) {
                result.put("sender_id", row.getInteger("sender_id"));
                result.put("sender_name", row.getString("sender_name"));
                result.put("title", row.getString("title"));
                result.put("content", row.getString("content"));
                result.put("created_at", Helper.getTime(row.getLocalDate("created_at"), row.getLocalTime("created_at")));
            }

            context.json(new JsonObject(Helper.respData(0, "获取成功", result)));
        });
    }

    public static void deletePost(RoutingContext context) {

    }

    public static void getPostList(RoutingContext context) {

    }

    public static void modifyPost(RoutingContext context) {

    }
}
