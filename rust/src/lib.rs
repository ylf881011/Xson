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
fn to_value(env: &mut JNIEnv, obj: JObject) -> Result<Value, jni::errors::Error> {
    if env.is_instance_of(&obj, "java/lang/String")? {
        let s: String = env.get_string(&JString::from(obj))?.into();
        Ok(Value::String(s))
    } else if env.is_instance_of(&obj, "java/lang/Boolean")? {
        let b: bool = env.call_method(obj, "booleanValue", "()Z", &[])?.z()?;
        Ok(Value::Bool(b))
    } else if env.is_instance_of(&obj, "java/lang/Number")? {
        if env.is_instance_of(&obj, "java/lang/Integer")? || env.is_instance_of(&obj, "java/lang/Long")? {
            let l: i64 = env.call_method(obj, "longValue", "()J", &[])?.j()?;
            Ok(Value::from(l))
        } else {
            let d: f64 = env.call_method(obj, "doubleValue", "()D", &[])?.d()?;
            Ok(Value::from(d))
        }
    } else if env.is_instance_of(&obj, "java/util/Map")? {
        let map = JMap::from_env(env, &obj)?;
        let mut json = JsonMap::new();
        let mut iter = map.iter(env)?;
        while let Some((k, v)) = iter.next(env)? {
            let key: String = env.get_string(&JString::from(k))?.into();
            json.insert(key, to_value(env, v)?);
        }
        Ok(Value::Object(json))
    } else if env.is_instance_of(&obj, "java/util/List")? {
        let list = JList::from_env(env, &obj)?;
        let size = list.size(env)? as usize;
        let mut arr = Vec::with_capacity(size);
        for i in 0..size as i32 {
            if let Some(item) = list.get(env, i)? {
                arr.push(to_value(env, item)?);
            }
        }
        Ok(Value::Array(arr))
    } else {
        Ok(Value::Null)
    }
}

/// 将 serde_json::Value 转换为 java 对象
fn to_object<'a>(env: &mut JNIEnv<'a>, value: &Value) -> Result<JObject<'a>, jni::errors::Error> {
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
            if let Some(i) = n.as_i64() {
                // 优先转为 Long，若范围较小可在 Java 层转换为 Integer
                let class = env.find_class("java/lang/Long")?;
                let obj = env.new_object(class, "(J)V", &[jni::objects::JValue::Long(i)])?;
                Ok(obj)
            } else {
                let d = n.as_f64().unwrap_or(0.0);
                let class = env.find_class("java/lang/Double")?;
                let obj = env.new_object(class, "(D)V", &[jni::objects::JValue::Double(d)])?;
                Ok(obj)
            }
        }
        Value::Array(arr) => {
            let class = env.find_class("java/util/ArrayList")?;
            let list = env.new_object(class, "()V", &[])?;
            let jlist = JList::from_env(env, &list)?;
            for v in arr {
                let item = to_object(env, v)?;
                jlist.add(env, &item)?;
            }
            Ok(list)
        }
        Value::Object(map) => {
            let class = env.find_class("java/util/HashMap")?;
            let obj = env.new_object(class, "()V", &[])?;
            let jmap = JMap::from_env(env, &obj)?;
            for (k, v) in map {
                let key = env.new_string(k)?;
                let val = to_object(env, v)?;
                jmap.put(env, &key, &val)?;
            }
            Ok(obj)
        }
        _ => Ok(JObject::null()),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_xson_Native_encode(
    mut env: JNIEnv,
    _class: JClass,
    map: JObject,
) -> jstring {
    match to_value(&mut env, map) {
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
    mut env: JNIEnv,
    _class: JClass,
    json: JString,
) -> jobject {
    let input: String = match env.get_string(&json) {
        Ok(s) => s.into(),
        Err(_) => return JObject::null().into_raw(),
    };
    match serde_json::from_str::<Value>(&input) {
        Ok(v) => match to_object(&mut env, &v) {
            Ok(obj) => obj.into_raw(),
            Err(_) => JObject::null().into_raw(),
        },
        Err(_) => JObject::null().into_raw(),
    }
}

