package com.example.xson;


/**
 * 负责加载本地 Rust 动态库并声明 JNI 方法的类。
 */
public class Native {
    static {
        try {
            // 尝试加载名为 xson 的动态库，
            // 该库在 Maven 构建阶段由 Cargo 编译生成。
            System.loadLibrary("xson");
        } catch (UnsatisfiedLinkError e) {
            // 加载失败时输出错误，后续可以在此处加入回退逻辑

            e.printStackTrace();
        }
    }


    /**
     * 调用 Rust 实现的 prettyPrint 方法，对传入的 JSON 字符串进行格式化。
     *
     * @param json 输入的 JSON 字符串
     * @return 美化后的 JSON 字符串，如果失败返回 null
     */

    public static native String prettyPrint(String json);
}
