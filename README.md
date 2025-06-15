# Xson
Xson 是一个基于 **Rust** + **JNI** 的高性能 JSON 序列化库。Maven 在构建时会自动编译 Rust 代码，并将生成的 `libxson.so` 打入 jar 包，使用者只需在 `pom.xml` 中声明依赖即可。

## 编译
1. 安装 Rust 工具链并确保可以访问 crates.io 下载依赖；
2. 安装 JDK17，保证 `mvn` 命令可用；
3. 运行 `mvn package` 完成编译，Rust 部分会使用 `target-cpu=native` 优化指令集。

## 使用
在项目的 `pom.xml` 中加入：
```xml
<dependency>
    <groupId>com.xson</groupId>
    <artifactId>xson</artifactId>
    <version>0.1.0</version>
</dependency>
```
示例：
```java
class User {
    public int id;
    public String name;
}

User u = new User();
u.id = 1;
u.name = "foo";
String json = Xson.toJson(u);
User other = Xson.fromJson(json, User.class);
```

## 性能测试
执行 `PerfTest` 可简单体验 JNI 版本的性能，并与 fastjson2 做对比：
```bash
mvn package
java -cp target/classes com.xson.PerfTest
```

若运行环境无法加载本地库，`Xson` 会自动回退到纯 Java 实现，但速度会有所下降。

