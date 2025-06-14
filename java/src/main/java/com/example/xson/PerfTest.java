package com.example.xson;

public class PerfTest {
    public static void main(String[] args) {
        String sample = "{\"key\":123,\"arr\":[1,2,3],\"msg\":\"hello\"}";
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            Xson.toJson(sample);
        }
        long end = System.currentTimeMillis();
        System.out.println("Rust prettyPrint time: " + (end - start) + "ms");
    }
}
