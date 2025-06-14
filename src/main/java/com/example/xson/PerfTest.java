package com.example.xson;

/**
 * 简单的性能压测类，通过循环调用测试 JNI 的开销。
 */
public class PerfTest {

    /**
     * 主函数：执行 1 万次 JSON 格式化并统计耗时。
     *
     * @param args 命令行参数
     */
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
