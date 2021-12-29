package com.zjutjh.controller;

import com.zjutjh.App;
import com.zjutjh.Helper;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Message {

    public static void getMessageList(RoutingContext context) {
        Session session = context.session();
        int userID = session.get("id");

        // 查询与本用户相关的评论记录
        String sql = "SELECT * FROM comments INNER JOIN posts ON comments.post_id = posts.id WHERE comments.sender_id <> ? AND (comments.at_id = ? OR posts.sender_id = ?) AND comments.is_read = 0 ORDER BY comments.created_at desc ";
        App.getMySQLClient().preparedQuery(sql).execute(Tuple.of(userID, userID, userID), ar -> {
            if (ar.failed()) {
                System.out.println(ar.cause().getMessage());
                return;
            } else if (ar.result().size() == 0) {
                context.json(new JsonObject(Helper.respData(2, "内容为空", null)));
                return;
            }

            ArrayList<Map<String, Object>> commentList = Helper.getCommentListData(ar.result());
            Map<String, Object> data = new HashMap<>();
            data.put("has_next", false);
            data.put("list", commentList);
            context.json(new JsonObject(Helper.respData(0, "获取成功", data)));
        });


    }

    public static void readMessage(RoutingContext context) {
        Session session = context.session();
        int userID = session.get("id");
        int commentID = Integer.parseInt(context.pathParam("id"));

        String updateCommentsRead = "UPDATE comments SET is_read = 1 WHERE id = ?";
        App.getMySQLClient().preparedQuery(updateCommentsRead).execute(Tuple.of(commentID)).compose(ar -> {

            // 对应用户消息数量+1
            String updateUserMessageNum = "UPDATE users SET message_num = message_num - 1 WHERE id = ?";
            return App.getMySQLClient().preparedQuery(updateUserMessageNum).execute(Tuple.of(userID));

        }).compose(ar -> {

            context.json(new JsonObject(Helper.respData(0, "已读评论", null)));
            return Future.succeededFuture();
        });


    }
}
