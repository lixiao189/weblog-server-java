package com.zjutjh.middleware;

import com.zjutjh.Helper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public class Validator {
    public static void checkAuth(RoutingContext context) {
        Session session = context.session();
        if (session.get("username") != null) {
            context.next();
        }

        context.json(new JsonObject(Helper.notLoginResponse()));
    }

    public static void checkPrivilege(RoutingContext context) {
        Session session = context.session();
        if (session.get("username") != null) {
            if (Integer.parseInt(session.get("administrator")) == 1) {
                context.next();
            } else {
                context.json(new JsonObject(Helper.notAdministratorResponse()));
            }
        }

        context.json(new JsonObject(Helper.notLoginResponse()));
    }
}
