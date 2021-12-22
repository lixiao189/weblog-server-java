package com.zjutjh.controller;

import com.zjutjh.App;
import com.zjutjh.Helper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;

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

    }

    public static void cancelReport(RoutingContext context) {

    }
}
