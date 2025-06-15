# 简化的 Makefile，实际构建由 Maven 完成

all:
	mvn -B package

clean:
	mvn -B clean

.PHONY: all clean

