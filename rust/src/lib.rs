//! Xson 库的核心实现，提供给 Java 调用的接口
//!
//! 当前仅包含一个示例函数 `prettyPrint`，演示如何在 Rust 中
//! 使用 serde 解析 JSON 并通过 JNI 返回结果。
// 与 Java 交互所需的 JNI 类型
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
// 使用 serde 解析和生成 JSON
use serde_json::Value;

#[no_mangle]
pub extern "system" fn Java_com_xson_Native_prettyPrint(
    env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    // 将 Java 字符串转换为 Rust 字符串
    let input: String = match env.get_string(&input) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    // 解析并美化 JSON 文本
    match serde_json::from_str::<Value>(&input)
        .and_then(|v| serde_json::to_string_pretty(&v))
    {
        Ok(pretty) => match env.new_string(pretty) {
            Ok(jstr) => jstr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}