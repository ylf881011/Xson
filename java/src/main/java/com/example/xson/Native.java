package com.example.xson;

public class Native {
    static {
        try {
            System.loadLibrary("xson");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public static native String prettyPrint(String json);
}
