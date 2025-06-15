package com.xson;

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
