package com.xson;

/**
 * 简单的性能压测类，通过循环调用测试 JNI 的开销。
 */
import com.alibaba.fastjson2.JSON;

public class PerfTest {

    /**
     * 主函数：执行 1 万次 JSON 格式化并统计耗时。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        Sample sample = new Sample();
        sample.key = 123;
        sample.arr = new int[]{1,2,3};
        sample.msg = "hello";

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

    public static class Sample {
        public int key;
        public int[] arr;
        public String msg;
    }
}
