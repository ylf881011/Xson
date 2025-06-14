
# Xson

Xson 是一个基于 Rust 实现的 JSON 序列化库示例，使用 JNI 与 Java 交互。

这个项目展示如何通过 Maven 构建 Java 与 Rust 代码，并在本地环境编译动态库。


## 编译

1. 安装 Rust 工具链（需自行在系统中配置）。
2. 安装 JDK17 并确保 `mvn` 命令可用。
3. 项目根目录提供 `Makefile`，直接执行 `make` 即可完成全部构建。
   也可分别执行 `make rust` 或 `make java`。
4. 编译完成后，`libxson.so` 会被复制到打包的 jar 中。


> 由于当前环境无法从 crates.io 下载依赖，请确保你的编译环境可以访问网络。

## 使用

在项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>xson</artifactId>
    <version>0.1.0</version>
</dependency>
```

示例代码：

```java
String json = "{\"foo\":1}";
String pretty = com.example.xson.Xson.toJson(json);
System.out.println(pretty);
```

## 性能测试

执行 `PerfTest` 可以简单测试 JNI 调用的性能：

```bash
make
java -cp target/classes com.example.xson.PerfTest
```

