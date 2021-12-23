package com.zjutjh;

import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Helper {
    public static Map<String, Object> respData(int code, String msg, Object data) {
        Map<String, Object> respData = new HashMap<>();
        respData.put("code", code);
        respData.put("msg", msg);
        respData.put("data", data);
        return respData;
    }

    public static Map<String, Object> notLoginResponse() {
        return respData(255, "尚未登录", null);
    }

    public static Map<String, Object> notAdministratorResponse() {
        return respData(254, "不是管理员", null);
    }

    public static String getTime(LocalDate localDate, LocalTime localTime) {
        return localDate + " " + localTime;
    }

    public static void executeReportQuery(RoutingContext context, int id, String sql, String msg) {
        App.getMySQLClient()
                .preparedQuery(sql)
                .execute(Tuple.of(id), res -> {
                    if (res.succeeded()) {
                        System.out.println(res.result());
                        context.json(Helper.respData(0, msg, null));
                    } else {
                        System.out.println(res.cause().getMessage());
                        context.end();
                    }
                });
    }

    public static ArrayList<Map<String, Object>> getPostListData(RowSet<Row> rows) {
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (Row row: rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.getInteger("id"));
            item.put("sender_id", row.getInteger("sender_id"));
            item.put("sender_name", row.getString("sender_name"));
            item.put("title", row.getString("title"));
            item.put("content", row.getString("content").length() > 100 ? row.getString("content").substring(0, 100) + "..." : row.getString("content"));
            item.put("created_at", Helper.getTime(row.getLocalDate("created_at"), row.getLocalTime("created_at")));
            list.add(item);
        }
        return list;
    }

    public static ArrayList<Map<String, Object>> getCommentListData(RowSet<Row> rows) {
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (Row row: rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.getInteger("id"));
            item.put("post_id", row.getInteger("post_id"));
            item.put("post_title", row.getString("post_title"));
            item.put("content", row.getString("content"));
            item.put("sender_id", row.getInteger("sender_id"));
            item.put("sender_name", row.getString("sender_name"));
            item.put("at_id", row.getInteger("at_id"));
            item.put("at_name", row.getString("at_name"));
            item.put("created_at", Helper.getTime(row.getLocalDate("created_at"), row.getLocalTime("created_at")));
            list.add(item);
        }
        return list;
    }
}
