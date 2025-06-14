# Xson

Xson 是一个基于 Rust 实现的 JSON 序列化库示例，使用 JNI 与 Java 交互。

这个项目展示如何在 Maven 中同时编译 Java 与 Rust 代码，并将生成的动态库打包进 jar，用户引入依赖即可直接使用。

## 编译

1. 安装 Rust 工具链（需自行在系统中配置）。
2. 安装 JDK17 并确保 `mvn` 命令可用。
3. 根目录执行 `mvn package` 即可完成构建，Maven 会在编译阶段调用 Cargo 生成动态库并打入 jar 中。
   运行时 `Native` 类会自动从 jar 中解压该库，无需额外配置。

> 由于当前环境无法从 crates.io 下载依赖，请确保你的编译环境可以访问网络。

## 使用

在项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
<groupId>com.xson</groupId>
    <artifactId>xson</artifactId>
    <version>0.1.0</version>
</dependency>
```

示例代码：

```java
String json = "{\"foo\":1}";
String pretty = com.xson.Xson.toJson(json);
System.out.println(pretty);
```

## 性能测试

执行 `PerfTest` 可以简单测试 JNI 调用的性能：

```bash
mvn package
java -cp target/classes com.xson.PerfTest
```