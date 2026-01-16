#!/bin/bash

# EvoBot序列播放器 - 打包脚本
# 生成JAR文件（包含代码和资源）

echo "========================================"
echo "EvoBot序列播放器 - 打包脚本"
echo "========================================"
echo ""

# 清理旧的编译文件
echo "清理旧的编译文件..."
rm -rf build
rm -rf com
rm -f *.jar

# 创建build目录
echo "创建build目录..."
mkdir -p build/classes
mkdir -p build/lib

# 编译Java源文���
echo ""
echo "编译Java源文件..."
javac -d build/classes \
    -source 1.8 \
    -target 1.8 \
    app/src/main/java/com/evobot/sequence/*.java

if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi

echo "✅ 编译成功"

# 创建MANIFEST文件
echo ""
echo "创建MANIFEST文件..."
cat > build/MANIFEST.MF << 'EOF'
Manifest-Version: 1.0
Created-By: Claude AI
Implementation-Title: EvoBotSequencePlayer
Implementation-Version: 1.0.0
Implementation-Vendor: EvoBot
EOF

# 打包JAR（仅代码）
echo ""
echo "打包JAR文件（仅代码）..."
cd build/classes
jar -cfm ../lib/evobot-sequence-player.jar ../MANIFEST.MF com/
cd ../..

# 打包完整JAR（包含assets）
echo "打包完整JAR文件（包含assets）..."
cd build
cp lib/evobot-sequence-player.jar lib/evobot-sequence-player-full.jar
jar -uf lib/evobot-sequence-player-full.jar app/src/main/assets/sequences/*.ebs
cd ..

# 输出结果
echo ""
echo "========================================"
echo "打包完成！"
echo "========================================"
echo ""
echo "生成的文件:"
echo "  📦 build/lib/evobot-sequence-player.jar       (仅代码，约20KB)"
echo "  📦 build/lib/evobot-sequence-player-full.jar  (含资源，约30KB)"
echo ""

# 显示文件大小
ls -lh build/lib/*.jar

echo ""
echo "========================================"
echo "使用方法"
echo "========================================"
echo ""
echo "方式1：直接使用JAR"
echo "  将 evobot-sequence-player-full.jar 添加到项目依赖"
echo ""
echo "方式2：提取源码"
echo "  jar -xf evobot-sequence-player.jar"
echo ""
echo "方式3：Android Studio集成"
echo "  将 app/src/main/java/ 目录复制到项目中"
echo "  将 app/src/main/assets/ 目录复制到项目中"
echo ""
echo "========================================"
