package com.zjutjh.controller;

import com.zjutjh.App;
import com.zjutjh.Helper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.Map;

public class Comment {
    public static void createComment(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        int postID = body.getInteger("post_id");
        String content = body.getString("content");
        Integer atID = body.getInteger("at_id");
        String atName = body.getString("at_name");

        // 检查是否为对用户评论或者对帖子
        if (atID == null || atName == null) {
            Helper.executeCreateCommentQuery(context, postID, content, atID, atName);
        } else {

            // 查询对应用户
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

                Helper.executeCreateCommentQuery(context, postID, content, atID, atName);

            });
        }

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
        JsonObject body = context.getBodyAsJson();
        String type = body.getString("type");
        int id = body.getInteger("id");
        int page = body.getInteger("page");

        if (type.equals("post")) {
            String queryCommentListStmt = "select comments.*, posts.title from comments inner join posts on posts.id = comments.post_id where comments.post_id = ? limit 20 offset ?";

            App.getMySQLClient().preparedQuery(queryCommentListStmt).execute(Tuple.of(id, (page - 1) * 20), ar -> {
                if (ar.failed()) {
                    System.out.println(ar.cause().getMessage());
                    return;
                } else if (ar.result().size() == 0) {
                    context.json(new JsonObject(Helper.respData(2, "内容为空", null)));
                    return;
                }

                ArrayList<Map<String, Object>> commentList = Helper.getCommentListData(ar.result());
                // 检查是否有下一个列表
                App.getMySQLClient().preparedQuery(queryCommentListStmt).execute(Tuple.of(id, page * 20), nextAr -> {
                    Map<String, Object> data = Helper.listRespData(nextAr.result(), commentList);
                    context.json(new JsonObject(Helper.respData(0, "获取成功", data)));
                });
            });
        } else if (type.equals("user")) {
            String queryUserListStmt = "select comments.*, posts.title from comments inner join posts on posts.id = comments.post_id where comments.sender_id = ? limit 20 offset ?";

            App.getMySQLClient().preparedQuery(queryUserListStmt).execute(Tuple.of(id, (page - 1) * 20), ar -> {
                if (ar.failed()) {
                    System.out.println(ar.cause().getMessage());
                    return;
                } else if (ar.result().size() == 0) {
                    context.json(new JsonObject(Helper.respData(2, "内容为空", null)));
                    return;
                }

                ArrayList<Map<String, Object>> commentList = Helper.getCommentListData(ar.result());
                // 检查是否有下一个列表
                App.getMySQLClient().preparedQuery(queryUserListStmt).execute(Tuple.of(id, page * 20), nextAr -> {
                    Map<String, Object> data = Helper.listRespData(nextAr.result(), commentList);
                    context.json(new JsonObject(Helper.respData(0, "获取成功", data)));
                });
            });
        } else {
            context.json(new JsonObject(Helper.respData(1, "参数错误", null)));
        }
    }
}
