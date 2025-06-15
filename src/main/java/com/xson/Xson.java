package com.xson;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 向外提供通用的序列化与反序列化接口。
 */
public class Xson {

    /**
     * 将任意 Java 对象序列化为 JSON 字符串。
     */
    public static String toJson(Object obj) {
        try {
            Map<String, Object> map = toMap(obj);
            return Native.encode(map);
        } catch (Throwable e) {
            return fallbackEncode(obj);
        }
    }

    /**
     * 从 JSON 字符串反序列化为指定类型的对象。
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            Map<String, Object> map = Native.decode(json);
            return fromMap(map, clazz);
        } catch (Throwable e) {
            return fallbackDecode(json, clazz);
        }
    }

    private static Map<String, Object> toMap(Object obj) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();
        for (Field f : obj.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            map.put(f.getName(), f.get(obj));
        }
        return map;
    }

    private static <T> T fromMap(Map<String, Object> map, Class<T> clazz) throws ReflectiveOperationException {
        T instance = clazz.getDeclaredConstructor().newInstance();
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            if (map.containsKey(f.getName())) {
                f.set(instance, map.get(f.getName()));
            }
        }
        return instance;
    }

    // 简易回退实现：直接使用字符串拼接
    private static String fallbackEncode(Object obj) {
        try {
            Map<String, Object> map = toMap(obj);
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(e.getKey()).append('"').append(':');
                Object v = e.getValue();
                if (v instanceof Number || v instanceof Boolean) {
                    sb.append(String.valueOf(v));
                } else {
                    sb.append('"').append(String.valueOf(v)).append('"');
                }
            }
            sb.append('}');
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static <T> T fallbackDecode(String json, Class<T> clazz) {
        Map<String, Object> map = new HashMap<>();
        String s = json.trim();
        if (s.startsWith("{") && s.endsWith("}")) {
            s = s.substring(1, s.length() - 1);
            for (String pair : s.split(",")) {
                if (pair.isBlank()) continue;
                String[] kv = pair.split(":", 2);
                if (kv.length != 2) continue;
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String val = kv[1].trim();
                Object value;
                if (val.startsWith("\"") && val.endsWith("\"")) {
                    value = val.substring(1, val.length() - 1);
                } else if ("true".equals(val) || "false".equals(val)) {
                    value = Boolean.valueOf(val);
                } else {
                    try {
                        value = Double.valueOf(val);
                    } catch (NumberFormatException e) {
                        value = val;
                    }
                }
                map.put(key, value);
            }
        }
        try {
            return fromMap(map, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

