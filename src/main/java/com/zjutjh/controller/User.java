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

public class User {
    public static void login(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        String username = body.getString("username");
        String password = body.getString("password");

        // A simple query
        App.getMySQLClient().preparedQuery("SELECT * FROM users WHERE username = ?").execute(
                Tuple.of(username), ar -> {
                    if (ar.succeeded()) {
                        RowSet<Row> rows = ar.result();
                        if (rows.size() == 0) {
                            context.json(new JsonObject(Helper.respData(2, "用户不存在", null)));
                        } else {
                            int id = 0;
                            String trueUser = "";
                            int administrator = 0;
                            String truePass = "";

                            for (Row row : rows) {
                                id = row.getInteger("id");
                                trueUser = row.getString("username");
                                truePass = row.getString("password");
                                administrator = row.getInteger("administrator");
                            }
                            if (truePass.equals(password)) {
                                // 设置 session
                                Session session = context.session();
                                session.put("id", id);
                                session.put("username", trueUser);
                                session.put("administrator", administrator);

                                context.json(new JsonObject(Helper.respData(0, "登录成功", null)));
                            } else {
                                context.json(new JsonObject(Helper.respData(1, "密码错误", null)));
                            }
                        }
                    } else {
                        System.out.println("Failure: " + ar.cause().getMessage());
                    }
                });
    }

    public static void register(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        String username = body.getString("username");
        String password = body.getString("password");

        if (username.length() < 4 || username.length() > 16 || password.length() < 7 || password.length() > 20) {
            context.json(new JsonObject(Helper.respData(2, "参数错误", null)));
            return;
        }

        String sqlStmt = "insert into users (username, password, administrator, followers_num, followed_num) VALUES (?, ?, 0, 0, 0)";
        App.getMySQLClient().preparedQuery(sqlStmt)
                .execute(Tuple.of(username, password), ar -> {
                    if (ar.succeeded()) {
                        context.json(new JsonObject(Helper.respData(0, "注册成功", null)));
                        return;
                    } else {
                        try {
                            throw ar.cause();
                        } catch (Throwable e) {
                            if (e.getMessage().equals("Duplicate entry 'node' for key 'users.username'")) {
                                context.json(new JsonObject(Helper.respData(1, "该用户名注册过了", null)));
                                return;
                            }
                        }
                    }
                    context.end();
                });
    }

    public static void getInfo(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        Session session = context.session();

        if (body.getString("type").equals("user")) {
            if (session.get("id") == null) { // 没有登录
                context.json(new JsonObject(Helper.notLoginResponse()));
                return;
            }

            // 查询用户信息
            String sqlStmt = "select * from users where id = ?";
            App.getMySQLClient().preparedQuery(sqlStmt).execute(Tuple.of(session.get("id")), ar -> {
                if (ar.succeeded()) {
                    RowSet<Row> rowSet = ar.result();
                    Map<String, Object> data = new HashMap<>();

                    for (Row row : rowSet) {
                        data.put("id", row.getInteger("id"));
                        data.put("username", row.getString("username"));
                        data.put("administrator", row.getInteger("administrator") == 1);
                        data.put("followed_num", row.getInteger("followed_num"));
                        data.put("followers_num", row.getInteger("followers_num"));
                    }

                    context.json(new JsonObject(Helper.respData(0, "查询成功", data)));
                } else {
                    context.json(new JsonObject(Helper.respData(0, "参数错误", null)));
                }
            });
        } else if (body.getString("type").equals("other")) {
            String queryOtherInfoStmt = "select * from users where id = ?";
            App.getMySQLClient().preparedQuery(queryOtherInfoStmt).execute(Tuple.of(body.getInteger("id")), ar -> {
                if (ar.result().size() == 0) {
                    context.json(new JsonObject(Helper.respData(1, "参数错误", null)));
                } else {
                    Map<String, Object> data = new HashMap<>();
                    for (Row row : ar.result()) {
                        data.put("id", row.getInteger("id"));
                        data.put("username", row.getString("username"));
                        data.put("followed_num", row.getInteger("followed_num"));
                        data.put("followers_num", row.getInteger("followers_num"));
                    }

                    // 根据是否关注来返回数据
                    String queryFollowRelationStmt = "select * from follow where user_id = ? and follower_id = ?";
                    if (session.get("id") == null) {
                        data.put("is_followed", false);

                        context.json(new JsonObject(Helper.respData(0, "查询成功", data)));
                    } else {
                        App.getMySQLClient().preparedQuery(queryFollowRelationStmt).execute(Tuple.of(body.getInteger("id"), (int) session.get("id")), checkFollowRelationResult -> {
                            boolean isFollowed = checkFollowRelationResult.result().size() != 0;
                            data.put("is_followed", isFollowed);

                            context.json(new JsonObject(Helper.respData(0, "查询成功", data)));
                        });
                    }
                }
            });
        } else {
            context.json(new JsonObject(Helper.respData(1, "参数错误", null)));
        }
    }

    public static void update(RoutingContext context) {
        // 解析 HTTP body 数据
        JsonObject body = context.getBodyAsJson();
        String pass = body.getString("password");

        // 解析 session 数据
        Session session = context.session();
        int id = session.get("id");

        if (pass.length() < 7 || pass.length() > 20) {
            context.json(new JsonObject(Helper.respData(1, "参数错误", null)));
            return;
        }

        String updateUserStmt = "update users set password = ? where id = ?";
        App.getMySQLClient().preparedQuery(updateUserStmt).execute(Tuple.of(pass, id), ar -> {
            if (ar.succeeded())
                context.json(new JsonObject(Helper.respData(0, "修改成功", null)));
            else {
                try {
                    throw ar.cause();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void logout(RoutingContext context) {
        Session session = context.session();
        session.destroy();

        context.json(new JsonObject(Helper.respData(0, "删除成功", null)));
    }
}
