use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use serde_json::Value;

#[no_mangle]
pub extern "system" fn Java_com_example_xson_Native_prettyPrint(
    env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    // Convert Java string to Rust string
    let input: String = match env.get_string(&input) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    // Parse and pretty-print JSON
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
