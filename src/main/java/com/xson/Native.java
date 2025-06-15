package com.xson;

/**
 * JNI 桥接类，负责加载本地 Rust 动态库并声明底层方法。
 */
public class Native {
    /** 是否成功加载了本地库 */
    public static final boolean LOADED;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("xson");
            loaded = true;
        } catch (UnsatisfiedLinkError ex) {
            String name = System.mapLibraryName("xson");
            // 兼容未通过 Maven 构建的场景，尝试从当前工作目录加载
            java.nio.file.Path local = java.nio.file.Paths.get(System.getProperty("user.dir"), name);
            try {
                System.load(local.toString());
                loaded = true;
            } catch (Throwable ignore) {
                try (var in = Native.class.getResourceAsStream("/" + name)) {
                    if (in == null) {
                        throw ex;
                    }
                    java.io.File temp = java.io.File.createTempFile("xson", name.substring(name.lastIndexOf('.')));
                    temp.deleteOnExit();
                    java.nio.file.Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.load(temp.getAbsolutePath());
                    loaded = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw ex;
                }
            }
        }
        LOADED = loaded;
    }

    /**
     * 将 Map 序列化为 JSON 字符串。
     *
     * @param map Java Map 对象
     * @return JSON 字符串
     */
    public static native String encode(java.util.Map<String, Object> map);

    /**
     * 将 JSON 字符串反序列化为 Map。
     *
     * @param json JSON 字符串
     * @return 解析后的 Map
     */
    public static native java.util.Map<String, Object> decode(String json);
}
