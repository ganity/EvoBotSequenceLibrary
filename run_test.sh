#!/bin/bash

# EvoBot序列播放器 - 快速测试脚本

echo "========================================="
echo "EvoBot序列播放�� - 快速测试"
echo "========================================="
echo ""

# 进入项目目录
cd "$(dirname "$0")"

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "❌ 未找到Java运行环境"
    echo "请安装JDK 8或更高版本"
    exit 1
fi

echo "✅ Java环境检测通过"
echo ""

# 编译测试类
echo "正在编译测试类..."
javac -d . app/src/main/java/com/evobot/sequence/SimpleSequenceTest.java

if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi

echo "✅ 编译成功"
echo ""

# 运行测试
echo "正在运行测试..."
echo ""
java com.evobot.sequence.SimpleSequenceTest

# 清理编译文件
echo ""
echo "清理临时文件..."
rm -rf com/

echo ""
echo "========================================="
echo "测试完成！"
echo "========================================="
echo ""
echo "详细测试报告请查看: TEST_REPORT.md"
echo ""
