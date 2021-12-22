package com.zjutjh;

import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDate;
import java.time.LocalTime;
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

    public static void executeReportQuery(RoutingContext context, int id, String sql) {
        App.getMySQLClient()
                .preparedQuery(sql)
                .execute(Tuple.of(id), res -> {
                    if (res.succeeded()) {
                        System.out.println(res.result());
                        context.json(Helper.respData(0, "举报成功", null));
                    } else {
                        System.out.println(res.cause().getMessage());
                        context.end();
                    }
                });
    }
}
