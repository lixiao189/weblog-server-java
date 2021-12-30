package com.zjutjh.controller;

import com.zjutjh.App;
import com.zjutjh.Helper;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.lang.reflect.Array;
import java.util.*;

public class Post {
    public static void createPost(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        Session session = context.session();

        int senderID = session.get("id");
        String name = session.get("username");
        String title = body.getString("title");
        String content = body.getString("content");
        JsonArray tags = body.getJsonArray("tags");

        // 先创建帖子
        final String insertPostStmt = "insert into posts (sender_id, sender_name, title, content, is_reported) values (?, ?, ?, ?, 0)";
        final List<Tuple> insertNewTagsBatch = new ArrayList<>();
        final List<Tuple> insertRelationBatch = new ArrayList<>();
        final Map<String, Object> futureContext = new HashMap<>();
        App.getMySQLClient().preparedQuery(insertPostStmt).execute(Tuple.of(senderID, name, title, content)).compose(ar -> {
            // 获取创建的帖子的 ID
            long postID = ar.property(MySQLClient.LAST_INSERTED_ID);
            futureContext.put("postID", postID);

            final List<Tuple> queryExitTagBatch = new ArrayList<>();
            for (Object tag : tags)
                queryExitTagBatch.add(Tuple.of(tag));
            String queryExistTagStmt = "select * from tags where name = ?";

            return App.getMySQLClient().preparedQuery(queryExistTagStmt).executeBatch(queryExitTagBatch);
        }).compose(ar -> {
            // 挑出新 tag 和旧 tag
            RowSet<Row> result = ar;
            futureContext.put("hasTag", tags.size() > 0); // 记录这个帖子是否存在 tag
            List<Row> oldTagList = new ArrayList<>();

            // 取出老 tag
            do {
                for (Row row : result)
                    oldTagList.add(row);
            } while ((result = result.next()) != null);

            for (Object tag : tags) {
                // 检查这个 tag 是否是 old tag
                boolean isOldTag = false;
                for (Row tagItem : oldTagList) {
                    if (tagItem.getString("name").equals(tag)) {
                        isOldTag = true;
                        insertRelationBatch.add(Tuple.of(futureContext.get("postID"), tagItem.getString("id")));
                        break;
                    }
                }

                // 检测到是新 tag
                if (!isOldTag) {
                    String newTagID = UUID.randomUUID().toString();
                    insertNewTagsBatch.add(Tuple.of(newTagID, tag));
                    insertRelationBatch.add(Tuple.of(futureContext.get("postID"), newTagID));
                }
            }

            // 插入 tag 和 post 的关系
            String insertRelationStmt = "insert into post_to_tag (post_id, tag_id) values (?, ?)";
            return App.getMySQLClient().preparedQuery(insertRelationStmt).executeBatch(insertRelationBatch);
        }).compose(ar -> {
            if (insertNewTagsBatch.size() == 0) {
                return Future.succeededFuture();
            } else {
                // 插入新的 tag
                String insertNewTagsStmt = "insert into tags (id, name) values (?, ?)";
                return App.getMySQLClient().preparedQuery(insertNewTagsStmt).executeBatch(insertNewTagsBatch);
            }
        }).compose(ar -> {
            context.json(new JsonObject(Helper.respData(0, "发送成功", null)));
            return Future.succeededFuture();
        }).onFailure(cause -> {
            cause.printStackTrace();
            context.end();
        });
    }

    public static void getPost(RoutingContext context) {
        int id = Integer.parseInt(context.pathParam("id"));

        String queryPostStmt = "select posts.*, tags.id as tag_id, tags.name as tag_name from posts " +
                "inner join post_to_tag " +
                "inner join tags " +
                "on post_to_tag.post_id = posts.id and post_to_tag.tag_id = tags.id " +
                "where posts.id = ?";
        App.getMySQLClient().preparedQuery(queryPostStmt).execute(Tuple.of(id), ar -> {
            RowSet<Row> rows = ar.result();
            if (rows.size() == 0) {
                context.json(new JsonObject(Helper.respData(2, "帖子不存在", null)));
                return;
            }

            List<Map<String, Object>> tagList = new ArrayList<>();
            for (Row row : rows) {
                Map<String, Object> tag = new HashMap<>();
                tag.put("id", row.getString("tag_id"));
                tag.put("name", row.getString("tag_name"));
                tagList.add(tag);
            }

            Map<String, Object> result = new HashMap<>();
            for (Row row : rows) {
                result.put("sender_id", row.getInteger("sender_id"));
                result.put("sender_name", row.getString("sender_name"));
                result.put("title", row.getString("title"));
                result.put("content", row.getString("content"));
                result.put("created_at", Helper.getTime(row.getLocalDate("created_at"), row.getLocalTime("created_at")));
                result.put("tags", tagList);
            }

            context.json(new JsonObject(Helper.respData(0, "获取成功", result)));
        });
    }

    public static void deletePost(RoutingContext context) {
        Session session = context.session();
        int postID = Integer.parseInt(context.pathParam("id"));
        int senderID = session.get("id");
        boolean isAdministrator = (int) session.get("administrator") == 1;

        String queryPostStmt = "select  * from posts where id = ?";
        App.getMySQLClient().preparedQuery(queryPostStmt).execute(Tuple.of(postID)).compose(ar -> {
            if (ar.size() == 0) {
                context.json(new JsonObject(Helper.respData(1, "数据不存在", null)));
                return Future.failedFuture("数据不存在");
            }

            int trueSenderID = -1;
            for (Row row : ar) {
                trueSenderID = row.getInteger("sender_id");
            }

            if (trueSenderID != senderID && !isAdministrator) {
                context.json(new JsonObject(Helper.respData(2, "只能删除自己的帖子", null)));
                return Future.failedFuture("只能删除自己的帖子");
            }

            String deletePostStmt = "delete from posts where id = ?";
            return App.getMySQLClient().preparedQuery(deletePostStmt).execute(Tuple.of(postID));
        }).compose(ar -> {
            context.json(new JsonObject(Helper.respData(0, "删除成功", null)));
            return Future.succeededFuture();
        });
    }

    public static void getPostList(RoutingContext context) {
        // 解析路径参数
        int page = Integer.parseInt(context.pathParam("page"));

        // 解析 body 参数
        JsonObject body = context.getBodyAsJson();
        String type = body.getString("type");

        // 查询数据结果
        final String queryPostListStmt;
        final Tuple queryPageData;
        final Tuple queryNextPageData;

        if (type.equals("all")) {
            queryPostListStmt = "select posts.*, tags.id as tag_id, tags.name from posts " +
                    "inner join post_to_tag inner join tags " +
                    "on posts.id = post_to_tag.post_id and post_to_tag.tag_id = tags.id " +
                    "order by posts.created_at desc limit 20 offset ?";
            queryPageData = Tuple.of((page - 1) * 20);
            queryNextPageData = Tuple.of(page * 20);
        } else if (type.equals("user")) {
            queryPostListStmt = "select posts.*, tags.id as tag_id, tags.name from posts " +
                    "inner join post_to_tag inner join tags " +
                    "on posts.id = post_to_tag.post_id and post_to_tag.tag_id = tags.id " +
                    "where sender_id = ? order by posts.created_at desc limit 20 offset ?";
            queryPageData = Tuple.of(body.getInteger("id"), (page - 1) * 20);
            queryNextPageData = Tuple.of(body.getInteger("id"), page * 20);
        } else {
            context.json(new JsonObject(Helper.respData(1, "参数错误", null)));
            return;
        }

        final ArrayList<Map<String, Object>> postList = new ArrayList<>();
        App.getMySQLClient().preparedQuery(queryPostListStmt).execute(queryPageData).compose(ar -> {
            if (ar.size() != 0) {
                Helper.getPostListData(ar, postList);
                return App.getMySQLClient().preparedQuery(queryPostListStmt).execute(queryNextPageData);
            } else {
                context.json(new JsonObject(Helper.respData(2, "内容为空", null)));
                return Future.failedFuture("内容为空");
            }
        }).compose(ar -> {
            final Map<String, Object> data = Helper.listRespData(ar, postList);
            context.json(new JsonObject(Helper.respData(0, "获取成功", data)));
            return Future.succeededFuture();
        });
    }

    public static void modifyPost(RoutingContext context) {
        Session session = context.session();
        JsonObject body = context.getBodyAsJson();

        int postID = body.getInteger("id");
        String title = body.getString("title");
        String content = body.getString("content");

        int senderID = session.get("id");
        boolean isAdministrator = (int) session.get("administrator") == 1;

        String queryPostStmt = "select * from posts where id = ?";
        App.getMySQLClient().preparedQuery(queryPostStmt).execute(Tuple.of(postID), ar -> {
            if (ar.result().size() <= 0) {
                context.json(new JsonObject(Helper.respData(1, "数据不存在", null)));
                return;
            }

            int trueSenderID = -1;
            for (Row row : ar.result()) {
                trueSenderID = row.getInteger("sender_id");
            }

            if (trueSenderID != senderID && !isAdministrator) {
                context.json(new JsonObject(Helper.respData(2, "只能修改自己的帖子", null)));
                return;
            }

            String deletePostStmt = "update posts set title = ?, content = ? where id = ?";
            App.getMySQLClient().preparedQuery(deletePostStmt).execute(Tuple.of(title, content, postID), updateAr -> {
                if (updateAr.succeeded()) {
                    context.json(new JsonObject(Helper.respData(0, "修改成功", null)));
                } else {
                    System.out.println(updateAr.cause().getMessage()); // 记录失败的原因
                }
            });
        });
    }
}
