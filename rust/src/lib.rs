//! Xson 库的核心实现，提供给 Java 调用的接口
//!
//! 提供 `encode` 与 `decode` 两个方法：
//! - `encode` 接收 `java.util.Map`，转换为 JSON 字符串返回
//! - `decode` 接收 JSON 字符串，返回 `java.util.HashMap`

use jni::objects::{JClass, JObject, JString, JMap, JList};
use jni::sys::{jobject, jstring};
use jni::JNIEnv;
use serde_json::{Map as JsonMap, Value};

/// 将 Java 对象递归转换为 serde_json::Value
fn to_value(env: &JNIEnv, obj: JObject) -> Result<Value, jni::errors::Error> {
    if env.is_instance_of(obj, "java/lang/String")? {
        let s: String = env.get_string(JString::from(obj))?.into();
        Ok(Value::String(s))
    } else if env.is_instance_of(obj, "java/lang/Boolean")? {
        let b: bool = env.call_method(obj, "booleanValue", "()Z", &[])?.z()?;
        Ok(Value::Bool(b))
    } else if env.is_instance_of(obj, "java/lang/Number")? {
        let d: f64 = env.call_method(obj, "doubleValue", "()D", &[])?.d()?;
        Ok(Value::from(d))
    } else if env.is_instance_of(obj, "java/util/Map")? {
        let map = JMap::from_env(env.clone(), obj)?;
        let mut json = JsonMap::new();
        for (k, v) in map.iter()? {
            let key: String = env.get_string(JString::from(k))?.into();
            json.insert(key, to_value(env, v)?);
        }
        Ok(Value::Object(json))
    } else if env.is_instance_of(obj, "java/util/List")? {
        let list = JList::from_env(env.clone(), obj)?;
        let mut arr = Vec::with_capacity(list.size()? as usize);
        for i in 0..list.size()? {
            let item = list.get(i)?;
            arr.push(to_value(env, item)?);
        }
        Ok(Value::Array(arr))
    } else {
        Ok(Value::Null)
    }
}

/// 将 serde_json::Value 转换为 java 对象
fn to_object<'a>(env: &JNIEnv<'a>, value: &Value) -> Result<JObject<'a>, jni::errors::Error> {
    match value {
        Value::String(s) => {
            let jstr = env.new_string(s)?;
            Ok(jstr.into())
        }
        Value::Bool(b) => {
            let class = env.find_class("java/lang/Boolean")?;
            let obj = env.new_object(class, "(Z)V", &[jni::objects::JValue::Bool(*b as u8)])?;
            Ok(obj)
        }
        Value::Number(n) => {
            let d = n.as_f64().unwrap_or(0.0);
            let class = env.find_class("java/lang/Double")?;
            let obj = env.new_object(class, "(D)V", &[jni::objects::JValue::Double(d)])?;
            Ok(obj)
        }
        Value::Array(arr) => {
            let class = env.find_class("java/util/ArrayList")?;
            let list = env.new_object(class, "()V", &[])?;
            let jlist = JList::from_env(env.clone(), list)?;
            for v in arr {
                let item = to_object(env, v)?;
                jlist.add(item)?;
            }
            Ok(list)
        }
        Value::Object(map) => {
            let class = env.find_class("java/util/HashMap")?;
            let obj = env.new_object(class, "()V", &[])?;
            let jmap = JMap::from_env(env.clone(), obj)?;
            for (k, v) in map {
                let key = env.new_string(k)?;
                let val = to_object(env, v)?;
                jmap.put(key, val)?;
            }
            Ok(obj)
        }
        _ => Ok(JObject::null()),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_xson_Native_encode(
    env: JNIEnv,
    _class: JClass,
    map: JObject,
) -> jstring {
    match to_value(&env, map) {
        Ok(value) => match serde_json::to_string(&value) {
            Ok(s) => match env.new_string(s) {
                Ok(out) => out.into_raw(),
                Err(_) => std::ptr::null_mut(),
            },
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_xson_Native_decode(
    env: JNIEnv,
    _class: JClass,
    json: JString,
) -> jobject {
    let input: String = match env.get_string(&json) {
        Ok(s) => s.into(),
        Err(_) => return JObject::null().into_inner(),
    };
    match serde_json::from_str::<Value>(&input) {
        Ok(v) => match to_object(&env, &v) {
            Ok(obj) => obj.into_raw(),
            Err(_) => JObject::null().into_inner(),
        },
        Err(_) => JObject::null().into_inner(),
    }
}