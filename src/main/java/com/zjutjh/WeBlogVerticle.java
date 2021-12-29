package com.zjutjh;

import com.zjutjh.controller.*;
import com.zjutjh.middleware.Validator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class WeBlogVerticle extends AbstractVerticle {
    public void initRouter(Router router) {
        Router apiRouter = Router.router(vertx);
        Router userRouter = Router.router(vertx);
        Router followRouter = Router.router(vertx);
        Router postRouter = Router.router(vertx);
        Router reportRouter = Router.router(vertx);
        Router tagRouter = Router.router(vertx);
        Router commentRouter = Router.router(vertx);
        Router messageRouter = Router.router(vertx);

        router.mountSubRouter("/api", apiRouter);
        apiRouter.route().handler(BodyHandler.create()); // 添加 body 处理中间件
        apiRouter.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        // 用户路由
        apiRouter.mountSubRouter("/user", userRouter);
        {
            userRouter.post("/login").handler(User::login);
            userRouter.post("/register").handler(User::register);
            userRouter.post("/info").handler(User::getInfo);
            userRouter.post("/update").handler(Validator::checkAuth).handler(User::update);
            userRouter.get("/logout").handler(Validator::checkAuth).handler(User::logout);

            userRouter.mountSubRouter("/follow", followRouter);
            {
                followRouter.post("/list").handler(Follow::getFollowList);
                followRouter.get("/cancel/:id").handler(Validator::checkAuth).handler(Follow::cancelFollow);
                followRouter.get("/:id").handler(Validator::checkAuth).handler(Follow::followUser);
            }

            // 消息路由
            userRouter.mountSubRouter("/message", messageRouter);
            {
                messageRouter.get("/list").handler(Validator::checkAuth).handler(Message::getMessageList);
                messageRouter.get("/:id").handler(Validator::checkAuth).handler(Message::readMessage);
            }
        }

        // 帖子路由
        apiRouter.mountSubRouter("/post", postRouter);
        {
            postRouter.post("/create").handler(Validator::checkAuth).handler(Post::createPost);
            postRouter.get("/:id").handler(Post::getPost);
            postRouter.get("/delete/:id").handler(Validator::checkAuth).handler(Post::deletePost);
            postRouter.post("/list/:page").handler(Post::getPostList);
            postRouter.post("/modify").handler(Validator::checkAuth).handler(Post::modifyPost);

            // 标签相关路由
            postRouter.mountSubRouter("/tag", tagRouter);
            {
                tagRouter.post("/autocomplete").handler(Validator::checkAuth).handler(Tag::tagAutoComplete); // 获取标签自动补全列表
            }

            // 举报路由
            postRouter.mountSubRouter("/report", reportRouter);
            {
                reportRouter.post("/create").handler(Validator::checkAuth).handler(Report::createReport);
                reportRouter.post("/list").handler(Validator::checkPrivilege).handler(Report::ListReport);
                reportRouter.post("/cancel").handler(Validator::checkPrivilege).handler(Report::cancelReport);
            }

            // 评论路由
            postRouter.mountSubRouter("/comment", commentRouter);
            {
                commentRouter.post("/create").handler(Validator::checkAuth).handler(Comment::createComment);
                commentRouter.get("/delete/:id").handler(Validator::checkAuth).handler(Comment::deleteComment);
                commentRouter.post("/list").handler(Comment::getCommentList);
            }
        }
    }

    @Override
    public void start(Promise<Void> startPromise) {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        initRouter(router);

        // Now bind the server:
        server.requestHandler(router).listen(8085, res -> {
            if (res.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(res.cause());
            }
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
