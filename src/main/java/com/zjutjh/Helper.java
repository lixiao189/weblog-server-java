package com.zjutjh;

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
}
