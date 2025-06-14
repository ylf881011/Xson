package com.example.xson;

public class Xson {
    public static String toJson(String json) {
        return Native.prettyPrint(json);
    }
}
