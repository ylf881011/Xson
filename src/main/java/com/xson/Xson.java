package com.xson;

/**
 * 对外暴露的高层 API，封装了 JSON 序列化功能。
 */
public class Xson {

    /**
     * 调用底层的 Rust 实现对 JSON 字符串进行格式化。
     *
     * @param json 输入的 JSON 字符串
     * @return 格式化后的 JSON 字符串
     */
    public static String toJson(String json) {
        return Native.prettyPrint(json);
    }
}
