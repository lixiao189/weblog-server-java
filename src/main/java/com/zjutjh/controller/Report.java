package com.zjutjh.controller;

import com.zjutjh.App;
import com.zjutjh.Helper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Report {

    public static void createReport(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        String type = body.getString("type");
        int id = body.getInteger("id");

        if (type.equals("post")) {
            Helper.executeReportQuery(context, id, "UPDATE posts SET is_reported = 1 WHERE id = ?");
        } else if (type.equals("comment")) {
            Helper.executeReportQuery(context, id, "UPDATE comments SET is_reported = 1 WHERE id = ?");
        } else {
            context.json(Helper.respData(1, "参数错误", null));
        }
    }

    public static void ListReport(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        String type = body.getString("type");

        if (type.equals("post")) {
            App.getMySQLClient()
                    .query("SELECT * FROM posts WHERE is_reported = 1")
                    .execute(res -> {
                        if (res.succeeded()) {
                            RowSet<Row> rows = res.result();
                            ArrayList<Map<String, Object>> list = Helper.getPostListData(rows);

                            String msg;

                            if (list.size() == 0) {
                                msg = "没有结果";
                                context.json(Helper.respData(2, msg, null));
                            } else {
                                msg = "获取成功";
                                Map<String, Object> data = new HashMap<>();
                                data.put("list", list);
                                context.json(Helper.respData(0, msg, data));
                            }

                        } else {
                            System.out.println(res.cause().getMessage());
                            context.end();
                        }
                    });
        } else if (type.equals("comment")) {
            App.getMySQLClient()
                    .query("SELECT * FROM comments WHERE is_reported = 1")
                    .execute(res -> {
                        if (res.succeeded()) {
                            RowSet<Row> rows = res.result();
                            ArrayList<Map<String, Object>> list = Helper.getCommentListData(rows);

                            String msg;

                            if (list.size() == 0) {
                                msg = "没有结果";
                                context.json(Helper.respData(2, msg, null));
                            } else {
                                msg = "获取成功";
                                Map<String, Object> data = new HashMap<>();
                                data.put("list", list);
                                context.json(Helper.respData(0, msg, data));
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

    public static void cancelReport(RoutingContext context) {

    }
}
