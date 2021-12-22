package com.zjutjh.controller;

import com.zjutjh.App;
import com.zjutjh.Helper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

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
                        return;
                    } else {
                        System.out.println("Failure: " + ar.cause().getMessage());
                    }
                    App.getMySQLClient().close();
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

        App.getMySQLClient().preparedQuery("insert into users (username, password, administrator, followers_num, followed_num) VALUES (?, ?, 0, 0, 0)")
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

    }

    public static void update(RoutingContext context) {

    }

    public static void logout(RoutingContext context) {
        Session session = context.session();
        session.destroy();

        context.json(new JsonObject(Helper.respData(0, "删除成功", null)));
    }
}
