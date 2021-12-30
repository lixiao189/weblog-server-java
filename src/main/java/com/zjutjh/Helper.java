package com.zjutjh;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
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
        for (Row row : rows) {
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

    public static void getPostListData(RowSet<Row> rows, ArrayList<Map<String, Object>> list) {
        Map<Integer, Map<String, Object>> postList = new HashMap<>();
        Map<Integer, ArrayList<Map<String, Object>>> tagList = new HashMap<>();
        for (Row row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.getInteger("id"));
            item.put("sender_id", row.getInteger("sender_id"));
            item.put("sender_name", row.getString("sender_name"));
            item.put("title", row.getString("title"));
            item.put("content", row.getString("content").length() > 100 ? row.getString("content").substring(0, 100) + "..." : row.getString("content"));
            item.put("created_at", Helper.getTime(row.getLocalDate("created_at"), row.getLocalTime("created_at")));

            Map<String, Object> tag = new HashMap<>();
            tag.put("id", row.getString("tag_id"));
            tag.put("name", row.getString("name"));

            int id = row.getInteger("id");
            postList.put(id, item);
            if (!tagList.containsKey(row.getInteger("id")))
                tagList.put(row.getInteger("id"), new ArrayList<>());
            tagList.get(row.getInteger("id")).add(tag);
        }

        for (Integer key : postList.keySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", postList.get(key).get("id"));
            item.put("sender_id", postList.get(key).get("sender_id"));
            item.put("sender_name", postList.get(key).get("sender_name"));
            item.put("title", postList.get(key).get("title"));
            item.put("created_at", postList.get(key).get("created_at"));
            item.put("tags", tagList.get(key));

            list.add(item);
        }
    }

    public static ArrayList<Map<String, Object>> getCommentListData(RowSet<Row> rows) {
        ArrayList<Map<String, Object>> list = new ArrayList<>();


        for (Row row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.getInteger("id"));
            item.put("post_id", row.getInteger("post_id"));
            item.put("post_title", row.getString("title"));
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

    public static Map<String, Object> listRespData(RowSet<Row> result, ArrayList<Map<String, Object>> list) {
        Map<String, Object> data = new HashMap<>();
        boolean hasNext = result.size() > 0;

        data.put("has_next", hasNext);
        data.put("list", list);

        return data;
    }

    public static void executeCreateCommentQuery(RoutingContext context, int postID, String content, Integer atID, String atName) {

        // 获取帖子标题
        String queryTitleStmt = "select * from posts where id = ?";
        App.getMySQLClient().preparedQuery(queryTitleStmt).execute(Tuple.of(postID), ar -> {
            if (ar.failed()) {
                System.out.println(ar.cause().getMessage());
                return;
            }

            if (ar.result().size() == 0) {
                context.json(new JsonObject(Helper.respData(3, "帖子不存在", null)));
                return;
            }

            int userID = -1;
            for (Row row : ar.result()) {
                userID = row.getInteger("sender_id");
            }

            // 解析用户 session
            Session session = context.session();
            int senderID = session.get("id");
            String senderName = session.get("username");


            String insertCommentStmt = "insert into comments (post_id, content, sender_id, sender_name, at_id, at_name, is_reported) values (?, ?, ?, ?, ?, ?, 0)";
            int finalUserID = userID;
            App.getMySQLClient().preparedQuery(insertCommentStmt).execute(Tuple.of(
                    postID, content, senderID, senderName, atID, atName
            ), insertAr -> {
                if (insertAr.succeeded()) {
                    // 对应用户消息数量+1 (自己对自己发的不加)
                    if (senderID == finalUserID || (atID != null && atID.equals(senderID))) {
                        context.json(new JsonObject(Helper.respData(0, "创建成功", null)));
                    } else {
                        int finalID;
                        if (atID != null) {
                            finalID = atID;
                        } else {
                            finalID = finalUserID;
                        }
                        String sql = "UPDATE users SET message_num = message_num + 1 WHERE id = ?";
                        App.getMySQLClient().preparedQuery(sql).execute(Tuple.of(finalID), res -> {
                            if (res.failed()) {
                                System.out.println(res.cause().getMessage());
                                context.end();
                            } else {
                                context.json(new JsonObject(Helper.respData(0, "创建成功", null)));
                            }
                        });
                    }


                } else {
                    System.out.println(insertAr.cause().getMessage());
                    context.end();
                }
            });
        });
    }
}

