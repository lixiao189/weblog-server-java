package com.zjutjh.controller;

import com.zjutjh.App;
import com.zjutjh.Helper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class Comment {
    public static void createComment(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        int postID = body.getInteger("post_id");
        String content = body.getString("content");
        Integer atID = body.getInteger("at_id");
        String atName = body.getString("at_name");

        String queryMatchingStmt = "select * from users where id = ?";
        App.getMySQLClient().preparedQuery(queryMatchingStmt).execute(Tuple.of(atID), ar -> {
            String trueName = "";

            if (ar.result().size() == 0) {
                context.json(new JsonObject(Helper.respData(1, "At 用户不存在", null)));
                return;
            }

            for (Row row : ar.result())
                trueName = row.getString("username");
            if (!trueName.equals(atName)) {
                context.json(new JsonObject(Helper.respData(2, "At 用户和 ID 无法对应", null)));
                return;
            }

            // 获取帖子标题
            String queryTitleStmt = "select * from posts where id = ?";
            App.getMySQLClient().preparedQuery(queryTitleStmt).execute(Tuple.of(atID), queryTitleAr -> {
                if (ar.failed()) {
                    System.out.println(ar.cause().getMessage());
                    return;
                }

                if (ar.result().size() == 0) {
                    context.json(new JsonObject(Helper.respData(3, "帖子不存在", null)));
                    return;
                }

                String title = "";
                for (Row row : queryTitleAr.result()) {
                    title = row.getString("title");
                }

                // 解析用户 session
                Session session = context.session();
                int senderID = session.get("id");
                String senderName = session.get("username");

                String insertCommentStmt = "insert into comments (post_id, post_title, content, sender_id, sender_name, at_id, at_name, is_reported) values (?, ?, ?, ?, ?, ?, ?, 0)";
                App.getMySQLClient().preparedQuery(insertCommentStmt).execute(Tuple.of(
                        postID, title, content, atID, atName, senderID, senderName
                ), insertAr -> {
                    if (insertAr.succeeded()) {
                        context.json(new JsonObject(Helper.respData(0, "创建成功", null)));
                    } else {
                        System.out.println(insertAr.cause().getMessage());
                    }
                });
            });
        });
    }

    public static void deleteComment(RoutingContext context) {
        // 解析路径参数
        int id = Integer.parseInt(context.pathParam("id"));

        // 解析用户 session
        Session session = context.session();
        int senderID = session.get("id");
        boolean isAdmin = (int) session.get("administrator") == 1;

        String queryCommentStmt = "select * from comments where id = ?";
        App.getMySQLClient().preparedQuery(queryCommentStmt).execute(Tuple.of(id), ar -> {
            RowSet<Row> result = ar.result();
            if (result.size() == 0) {
                context.json(new JsonObject(Helper.respData(1, "数据不存在", null)));
                return;
            }

            int realSenderID = -1;
            for (Row row : ar.result()) {
                realSenderID = row.getInteger("sender_id");
            }

            if (realSenderID != senderID && !isAdmin) {
                context.json(new JsonObject(Helper.respData(2, "只能删除自己的评论", null)));
                return;
            }

            String deleteCommentStmt = "delete from comments where id = ?";
            App.getMySQLClient().preparedQuery(deleteCommentStmt).execute(Tuple.of(id), deleteAr -> {
                if (deleteAr.succeeded()) {
                    context.json(new JsonObject(Helper.respData(0, "删除成功", null)));
                } else {
                    System.out.println(deleteAr.cause().getMessage());
                }
            });
        });
    }

    public static void getCommentList(RoutingContext context) {

    }
}
