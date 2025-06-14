package com.xson;

/**
 * 负责加载本地 Rust 动态库并声明 JNI 方法的类。
 */
public class Native {
    static {
        try {
            System.loadLibrary("xson");
        } catch (UnsatisfiedLinkError ex) {
            String name = System.mapLibraryName("xson");
            try (var in = Native.class.getResourceAsStream("/" + name)) {
                if (in == null) {
                    throw ex;
                }
                java.io.File temp = java.io.File.createTempFile("xson", name.substring(name.lastIndexOf('.')));
                temp.deleteOnExit();
                java.nio.file.Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.load(temp.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                throw ex;
            }
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
