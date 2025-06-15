package com.xson;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向外提供通用的序列化与反序列化接口。
 */
public class Xson {

    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    private static Field[] getFields(Class<?> cls) {
        return FIELD_CACHE.computeIfAbsent(cls, c -> {
            Field[] fs = c.getDeclaredFields();
            for (Field f : fs) {
                f.setAccessible(true);
            }
            return fs;
        });
    }


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

    /**
     * 将任意对象转换为只包含基本类型、Map、List 的结构，方便传递给 Rust。
     */
    private static Object toPlain(Object obj) throws IllegalAccessException {
        if (obj == null) return null;
        Class<?> type = obj.getClass();
        if (type.isArray()) {
            int len = Array.getLength(obj);
            List<Object> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                list.add(toPlain(Array.get(obj, i)));
            }
            return list;
        } else if (obj instanceof Collection<?> col) {
            List<Object> list = new ArrayList<>(col.size());
            for (Object item : col) {
                list.add(toPlain(item));
            }
            return list;
        } else if (obj instanceof Map<?,?> map) {
            Map<String, Object> out = new HashMap<>();
            for (var e : ((Map<?,?>) map).entrySet()) {
                out.put(String.valueOf(e.getKey()), toPlain(e.getValue()));
            }
            return out;
        } else if (type.isPrimitive() || obj instanceof Number || obj instanceof Boolean || obj instanceof String) {
            return obj;
        } else {
            Map<String, Object> map = new HashMap<>();
            for (Field f : getFields(type)) {
                map.put(f.getName(), toPlain(f.get(obj)));
            }
            return map;
        }
    }

    private static Map<String, Object> toMap(Object obj) throws IllegalAccessException {
        return (Map<String, Object>) toPlain(obj);
    }

    private static <T> T fromMap(Map<String, Object> map, Class<T> clazz) throws ReflectiveOperationException {
        T instance = clazz.getDeclaredConstructor().newInstance();
        for (Field f : getFields(clazz)) {
            if (!map.containsKey(f.getName())) {
                continue;
            }
            Object value = map.get(f.getName());
            if (value == null) {
                continue;
            }

            Object conv = convertValue(value, f.getType());
            if (conv != null || !f.getType().isPrimitive()) {
                f.set(instance, conv);
            }
        }
        return instance;
    }

    /**
     * 根据字段类型将通用结构的值转换为目标对象。
     */
    @SuppressWarnings("unchecked")
    private static Object convertValue(Object value, Class<?> type) throws ReflectiveOperationException {
        if (value == null) return null;
        if (type.isPrimitive()) {
            if (!(value instanceof Number) && !(type == boolean.class && value instanceof Boolean)) {
                return null;
            }
            Number num = (value instanceof Number) ? (Number) value : 0;
            if (type == int.class) return num.intValue();
            if (type == long.class) return num.longValue();
            if (type == float.class) return num.floatValue();
            if (type == double.class) return num.doubleValue();
            if (type == short.class) return num.shortValue();
            if (type == byte.class) return num.byteValue();
            if (type == boolean.class) return value;
            return null;
        }
        if (Number.class.isAssignableFrom(type) && value instanceof Number num) {
            if (type == Integer.class) return num.intValue();
            if (type == Long.class) return num.longValue();
            if (type == Float.class) return num.floatValue();
            if (type == Short.class) return num.shortValue();
            if (type == Byte.class) return num.byteValue();
            if (type == Double.class) return num.doubleValue();
        }
        if (type == String.class && value instanceof String s) return s;
        if (type == Boolean.class && value instanceof Boolean b) return b;
        if (type.isArray() && value instanceof List<?> list) {
            Class<?> comp = type.getComponentType();
            Object arr = Array.newInstance(comp, list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(arr, i, convertValue(list.get(i), comp));
            }
            return arr;
        }
        if (List.class.isAssignableFrom(type) && value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(convertValue(item, Object.class));
            }
            return out;
        }
        if (Map.class.isAssignableFrom(type) && value instanceof Map<?,?> m) {
            Map<Object, Object> out = new HashMap<>();
            for (var e : ((Map<?,?>) m).entrySet()) {
                out.put(e.getKey(), convertValue(e.getValue(), Object.class));
            }
            return out;
        }
        if (value instanceof Map<?,?> m) {
            return fromMap((Map<String, Object>) m, type);
        }
        return value;
    }

    // 简易回退实现：直接使用字符串拼接
    private static String fallbackEncode(Object obj) {
        try {
            Object plain = toPlain(obj);
            return stringify(plain);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static String stringify(Object v) throws IllegalAccessException {
        if (v == null) return "null";
        if (v instanceof Map<?,?> m) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (var e : ((Map<String,Object>) m).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(e.getKey()).append('"').append(':');
                sb.append(stringify(e.getValue()));
            }
            sb.append('}');
            return sb.toString();
        } else if (v instanceof Collection<?> col) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(',');
                first = false;
                sb.append(stringify(item));
            }
            sb.append(']');
            return sb.toString();
        } else if (v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        } else {
            return '"' + String.valueOf(v) + '"';
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

