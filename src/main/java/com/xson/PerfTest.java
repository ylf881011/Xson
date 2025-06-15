package com.xson;

/**
 * 简单的性能压测类，通过循环调用测试 JNI 的开销。
 */
import com.alibaba.fastjson2.JSON;
import lombok.Data;

public class PerfTest {

    /**
     * 主函数：执行 1 万次 JSON 格式化并统计耗时。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        Sample sample = buildSample();


        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            String json = Xson.toJson(sample);
            Xson.fromJson(json, Sample.class);
        }
        long end = System.currentTimeMillis();
        System.out.println("Xson time: " + (end - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            String json = JSON.toJSONString(sample);
            JSON.parseObject(json, Sample.class);
        }
        end = System.currentTimeMillis();
        System.out.println("Fastjson2 time: " + (end - start) + "ms");
    }


    private static Sample buildSample() {
        Sample s = new Sample();
        s.key = 123;
        s.arr = new int[]{1,2,3};
        s.msg = "hello";
        s.nested = new Nested();
        s.nested.name = "inner";
        s.nested.matrix = java.util.List.of(new int[]{1,2}, new int[]{3,4});
        s.list = java.util.List.of(s.nested);
        java.util.Map<String, Nested> m = new java.util.HashMap<>();
        m.put("n1", s.nested);
        s.map = m;
        return s;
    }

    @Data
    public static class Sample {
        private int key;
        private int[] arr;
        private String msg;
        private java.util.List<Nested> list;
        private java.util.Map<String, Nested> map;
        private Nested nested;
    }
    @Data
    public static class Nested {
        private String name;
        private java.util.List<int[]> matrix;
    }
}
