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
import java.util.HashMap;
import java.util.Map;

public class Follow {
    public static void getFollowList(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        String type = body.getString("type");
        String userId = body.getString("user_id");
        int page = body.getInteger("page");

        if (type.equals("fans")) {
            int offset1 = (page - 1) * 20;
            App.getMySQLClient()
                    .preparedQuery("SELECT * FROM follow INNER JOIN users ON users.id = follow.follower_id WHERE user_id = ? LIMIT 20 OFFSET ?")
                    .execute(Tuple.of(userId, offset1), res -> {
                        if (res.succeeded()) {
                            System.out.println("233");
                            RowSet<Row> rowSet = res.result();
                            ArrayList<Map<String, Object>> list = new ArrayList<>();
                            for (Row row : rowSet) {
                                Map<String, Object> _data = new HashMap<>();
                                _data.put("id", row.getInteger("id"));
                                _data.put("name", row.getString("username"));
                                list.add(_data);
                            }

                            String msg;

                            if (list.size() == 0) {
                                msg = "没有结果";
                                Object data = null;
                                context.json(Helper.respData(2, msg, data));
                            } else {
                                msg = "获取成功";
                                Map<String, Object> data = new HashMap<>();
                                data.put("list", list);

                                // 多查询一次
                                int offset2 = page * 20;
                                String finalMsg = msg;
                                App.getMySQLClient()
                                        .preparedQuery("SELECT * FROM follow INNER JOIN users ON users.id = follow.follower_id WHERE user_id = ? LIMIT 20 OFFSET ?")
                                        .execute(Tuple.of(userId, offset2), res2 -> {
                                            if (res2.succeeded()) {
                                                RowSet<Row> rows = res2.result();
                                                boolean has_next = rows.size() != 0;
                                                data.put("has_next", has_next);
                                                context.json(Helper.respData(0, finalMsg, data));
                                            } else {
                                                System.out.println(res2.cause().getMessage());
                                                context.end();
                                            }
                                        });
                            }
                        } else {
                            System.out.println(res.cause().getMessage());
                            context.end();
                        }
                    });
        } else if (type.equals("follow")) {
            int offset1 = (page - 1) * 20;
            App.getMySQLClient()
                    .preparedQuery("SELECT * FROM follow INNER JOIN users ON users.id = follow.user_id WHERE follower_id = ? LIMIT 20 OFFSET ?")
                    .execute(Tuple.of(userId, offset1), res -> {
                        if (res.succeeded()) {
                            RowSet<Row> rowSet = res.result();
                            ArrayList<Map<String, Object>> list = new ArrayList<>();
                            for (Row row : rowSet) {
                                Map<String, Object> _data = new HashMap<>();
                                _data.put("id", row.getInteger("id"));
                                _data.put("name", row.getString("username"));
                                list.add(_data);
                            }

                            String msg;

                            if (list.size() == 0) {
                                msg = "没有结果";
                                Object data = null;
                                context.json(Helper.respData(2, msg, data));
                            } else {
                                msg = "获取成功";
                                Map<String, Object> data = new HashMap<>();
                                data.put("list", list);

                                // 多查询一次
                                int offset2 = page * 20;
                                String finalMsg = msg;
                                App.getMySQLClient()
                                        .preparedQuery("SELECT * FROM follow INNER JOIN users ON users.id = follow.user_id WHERE follower_id = ? LIMIT 20 OFFSET ?")
                                        .execute(Tuple.of(userId, offset2), res2 -> {
                                            if (res2.succeeded()) {
                                                RowSet<Row> rows = res2.result();
                                                boolean has_next = rows.size() != 0;
                                                data.put("has_next", has_next);
                                                context.json(Helper.respData(0, finalMsg, data));
                                            } else {
                                                System.out.println(res2.cause().getMessage());
                                                context.end();
                                            }
                                        });
                            }
                        } else {
                            System.out.println(res.cause().getMessage());
                            context.end();
                        }
                    });
        } else {
            context.json(Helper.respData(1, "参数错误", null));
        }
    }

    public static void cancelFollow(RoutingContext context) {
        Session session = context.session();
        int followerId = session.get("id");
        int id = Integer.parseInt(context.pathParam("id"));

        App.getMySQLClient()
                .preparedQuery("DELETE FROM follow WHERE user_id = ? AND follower_id = ?")
                .execute(Tuple.of(id, followerId), res -> {
                    if (res.succeeded()) {

                        // 先减少自己的关注的大佬人数
                        App.getMySQLClient()
                                .preparedQuery("UPDATE users SET followed_num = followed_num - 1 WHERE id = ?")
                                .execute(Tuple.of(followerId), res2 -> {
                                    if (res2.succeeded()) {

                                        // 减少被关注的大佬的跟随者人数
                                        App.getMySQLClient()
                                                .preparedQuery("UPDATE users SET followers_num = followers_num - 1 WHERE id = ?")
                                                .execute(Tuple.of(id), res3 -> {
                                                    if (res3.succeeded()) {
                                                        JsonObject respData = new JsonObject(Helper.respData(0, "取消关注成功", null));
                                                        context.json(respData);
                                                    } else {
                                                        System.out.println(res3.cause().getMessage());
                                                        context.end();
                                                    }
                                                });

                                    } else {
                                        System.out.println(res2.cause().getMessage());
                                        context.end();
                                    }
                                });
                    } else {
                        System.out.println(res.cause().getMessage());
                        context.end();
                    }
                });
    }

    public static void followUser(RoutingContext context) {
        int id = Integer.parseInt(context.pathParam("id"));
        Session session = context.session();
        int followerId = session.get("id");

        App.getMySQLClient()
                .preparedQuery("INSERT INTO follow (user_id, follower_id) VALUES (?, ?)")
                .execute(Tuple.of(id, followerId), res -> {
                    if (res.succeeded()) {

                        // 先添加自己的关注的大佬人数
                        App.getMySQLClient()
                                .preparedQuery("UPDATE users SET followed_num = followed_num + 1 WHERE id = ?")
                                .execute(Tuple.of(followerId), res2 -> {
                                    if (res2.succeeded()) {

                                        // 添加被关注的大佬的跟随者人数
                                        App.getMySQLClient()
                                                .preparedQuery("UPDATE users SET followers_num = followers_num + 1 WHERE id = ?")
                                                .execute(Tuple.of(id), res3 -> {
                                                    if (res3.succeeded()) {
                                                        JsonObject respData = new JsonObject(Helper.respData(0, "关注成功", null));
                                                        context.json(respData);
                                                    } else {
                                                        System.out.println(res3.cause().getMessage());
                                                        context.end();
                                                    }
                                                });
                                    } else {
                                        System.out.println(res2.cause().getMessage());
                                        context.end();
                                    }
                                });
                    } else {
                        System.out.println(res.cause().getMessage());
                        context.end();
                    }
                });
    }
}
