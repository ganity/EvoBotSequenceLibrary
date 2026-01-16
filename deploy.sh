#!/bin/bash

# EvoBot序列播放器 - 快速部署脚本
# 将源码和资源复制到目标Android项目

echo "========================================"
echo "EvoBot序列播放器 - 快速部署脚本"
echo "========================================"
echo ""

# 检查参数
if [ -z "$1" ]; then
    echo "用法: $0 <目标Android项目路径>"
    echo ""
    echo "示例:"
    echo "  $0 /path/to/YourAndroidProject"
    echo "  $0 ~/AndroidStudioProjects/MyApp"
    echo ""
    exit 1
fi

TARGET_PROJECT="$1"

# 检查目标目录是否存在
if [ ! -d "$TARGET_PROJECT" ]; then
    echo "❌ 目标目录不存在: $TARGET_PROJECT"
    exit 1
fi

echo "目标项目: $TARGET_PROJECT"
echo ""

# 确认部署
read -p "确定要将EvoBot序列播放器部署到该项目吗？(y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 0
fi

echo ""
echo "开始部署..."
echo ""

# 1. 复制Java源码
echo "[1/3] 复制Java源码..."
mkdir -p "$TARGET_PROJECT/app/src/main/java/com"
cp -r app/src/main/java/com/evobot "$TARGET_PROJECT/app/src/main/java/com"
if [ $? -eq 0 ]; then
    echo "✅ Java源码复制完成"
else
    echo "❌ Java源码复制失败"
    exit 1
fi

# 2. 复制assets资源
echo "[2/3] 复制assets资源..."
mkdir -p "$TARGET_PROJECT/app/src/main/assets"
cp -r app/src/main/assets "$TARGET_PROJECT/app/src/main/"
if [ $? -eq 0 ]; then
    echo "✅ Assets资源复制完成"
else
    echo "❌ Assets资源复制失败"
    exit 1
fi

# 3. 检查并更新build.gradle
echo "[3/3] 检查build.gradle配置..."

TARGET_BUILD_GRADLE="$TARGET_PROJECT/app/build.gradle"

if [ -f "$TARGET_BUILD_GRADLE" ]; then
    echo "✅ 找到build.gradle"
    echo ""
    echo "请确保build.gradle包含以下配置："
    echo ""
    echo "android {"
    echo "    compileSdk 34"
    echo "    defaultConfig {"
    echo "        minSdk 21"
    echo "        targetSdk 34"
    echo "    }"
    echo "    compileOptions {"
    echo "        sourceCompatibility JavaVersion.VERSION_1_8"
    echo "        targetCompatibility JavaVersion.VERSION_1_8"
    echo "    }"
    echo "}"
    echo ""
else
    echo "⚠️  未找到build.gradle，需要手动配置"
fi

# 完成
echo ""
echo "========================================"
echo "✅ 部署完成！"
echo "========================================"
echo ""
echo "下一步操作："
echo "1. 在Android Studio中同步项目 (Sync Project with Gradle Files)"
echo "2. 等待Gradle同步完成"
echo "3. 在你的代码中使用："
echo ""
echo "   EvoBotSequencePlayer player = new EvoBotSequencePlayer(context);"
echo "   player.play(\"左臂挥手右臂掐腰抱胸\", 40, new SequenceListener() {"
echo "       @Override"
echo "       public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {"
echo "           // 发送关节数据到机器人"
echo "           sendToRobot(leftArm, rightArm);"
echo "       }"
echo "       @Override"
echo "       public void onComplete() { }"
echo "       @Override"
echo "       public void onError(String errorMessage) { }"
echo "   });"
echo ""
echo "========================================"
