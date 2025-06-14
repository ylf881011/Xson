# 顶层 Makefile，用于同时构建 Rust 和 Java 部分
# 使用 `make` 即可执行全部构建流程

# Rust 项目目录
RUST_DIR := rust
# Maven 项目目录(当前目录)
JAVA_DIR := .

# 默认目标：同时构建 Rust 和 Java
all: java

# 仅构建 Rust 动态库
rust:
cd $(RUST_DIR) && cargo build --release

# 构建 Java，并在过程中执行 Rust 构建
java:
mvn -B package

# 清理生成文件
clean:
cd $(RUST_DIR) && cargo clean
mvn -B clean

.PHONY: all rust java clean
