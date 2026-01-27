# EvoBot序列播��器 - 打包指南

## 📦 打包方式汇总

本项目提供了**三种打包方式**，根据你的使用场景选择：

---

## 方式1：Android Studio打包（推荐）

**适用场景**：需要在Android应用中集成

### 步骤1：使用Android Studio打开项目

```bash
# 如果还没有Android Studio，需要先安装
# 下载地址: https://developer.android.com/studio

# 方式A：直接打开项目目录
# Android Studio -> File -> Open -> 选择 EvoBotSequenceLibrary 目录

# 方式B：创建新项目并复制文件
# 1. 创建新的Android Library项目
# 2. 将以下文件复制到项目中：
#    - app/src/main/java/com/evobot/sequence/ (所有Java文件)
#    - app/src/main/assets/sequences/ (.ebs文件)
#    - app/build.gradle (修改为你的项目配置)
#    - app/src/main/AndroidManifest.xml
```

### 步骤2：配置build.gradle

确保`app/build.gradle`包含：

```gradle
plugins {
    id 'com.android.library'
}

android {
    namespace 'com.evobot.sequence'
    compileSdk 34

    defaultConfig {
        minSdk 21
        targetSdk 34
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    // 无需额外依赖
}
```

### 步骤3：构建AAR

**方式A：使用Android Studio GUI**
```
菜单：Build -> Build Bundle(s) / APK(s) -> Build Bundle(s)
```

**方式B：使用命令行**
```bash
# Mac/Linux
./gradlew :app:assembleRelease

# Windows
gradlew.bat :app:assembleRelease
```

### 步骤4：查找输出文件

```
EvoBotSequenceLibrary/app/build/outputs/aar/
├── app-debug.aar           # 调试版本
└── app-release.aar         # 发布版本（推荐）
```

### 步骤5：集成到其他Android项目

```gradle
// 方式1：使用AAR文件
repositories {
    flatDir {
        dirs 'libs'  // 将aar文件放到项目的libs目录
    }
}

dependencies {
    implementation(name: 'app-release', ext: 'aar')
}

// 方式2：直接作为模块导入
// settings.gradle:
include ':evobot-sequence-player'
project(':evobot-sequence-player').projectDir = new File('/path/to/EvoBotSequenceLibrary/app')
```

---

## 方式2：直接复制源码（最简单）

**适用场景**：快速集成、无需编译

### 步骤：

```bash
# 1. 复制Java源码到你的Android项目
cp -r EvoBotSequenceLibrary/app/src/main/java/com/evobot/sequence \
      YourAndroidProject/app/src/main/java/

# 2. 复制assets资源
cp -r EvoBotSequenceLibrary/app/src/main/assets \
      YourAndroidProject/app/src/main/

# 3. 在你的build.gradle中确保配置正确
# （参考方式1的build.gradle配置）
```

---

## 方式3：生成JAR文件（仅代码）

**适用场景**：
- 需要手动管理依赖
- 不包含Android资源
- 作为Java库分发

### 使用Java命令行（需Android SDK）

```bash
# 指定Android SDK路径
export ANDROID_HOME=/path/to/android/sdk

# 编译（包含Android库）
javac -d build/classes \
    -cp "$ANDROID_HOME/platforms/android-34/android.jar" \
    -source 1.8 \
    -target 1.8 \
    app/src/main/java/com/evobot/sequence/*.java

# 打包JAR
jar -cf build/evobot-sequence-player.jar -C build/classes com
```

### 使用Gradle（推荐）

```bash
# 在EvoBotSequenceLibrary目录下执行

# 方法1：使用系统已安装的Gradle
gradle :app:jar

# 方法2：使用Gradle Wrapper（需要先生成wrapper文件）
./gradlew :app:jar

# 输出位置：
# build/libs/evobot-sequence-player.jar
```

---

## 快速打包脚本

### 在Android环境打包

创建文件 `package_android.sh`：

```bash
#!/bin/bash

echo "=== EvoBot序列播放器打包脚本 ==="

# 检查Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "错误：请设置ANDROID_HOME环境变量"
    echo "示例: export ANDROID_HOME=/Users/yourname/Library/Android/sdk"
    exit 1
fi

# 编译
echo "编译Java源码..."
javac -d build/classes \
    -cp "$ANDROID_HOME/platforms/android-34/android.jar" \
    -source 1.8 \
    -target 1.8 \
    app/src/main/java/com/evobot/sequence/SequenceListener.java \
    app/src/main/java/com/evobot/sequence/SequenceData.java \
    app/src/main/java/com/evobot/sequence/PlayerState.java \
    app/src/main/java/com/evobot/sequence/SequenceLoader.java \
    app/src/main/java/com/evobot/sequence/EvoBotSequencePlayer.java

# 打包JAR
echo "打包JAR..."
jar -cf build/evobot-sequence-player.jar \
    -C build/classes com

echo "完成！输出文件：build/evobot-sequence-player.jar"
ls -lh build/evobot-sequence-player.jar
```

---

## 当前环境打包

由于当前系统没有Android SDK，提供**最实用的方案**：

### 方案：提供源码 + 编译说明

创建一个分发包：

```bash
# 1. 创建分发包目录
mkdir -p EvoBotSequencePlayer-Distribution
cd EvoBotSequencePlayer-Distribution

# 2. 复制源码
cp -r ../app/src/main/java .
cp -r ../app/src/main/assets .
cp -r ../app/src/main/AndroidManifest.xml .

# 3. 复制文档
cp ../README.md .
cp ../BUILD.md .
cp ../TEST_REPORT.md .

# 4. 创建编译说明
cat > HOW_TO_BUILD.md << 'EOF'
# 编译指南

## 方法1：Android Studio（推荐）

1. 创建新的Android Library项目
2. 复制src目录内容到项目的app/src/main/
3. 同步Gradle
4. Build -> Make Project

## 方法2：命令行

```bash
# 设置Android SDK路径
export ANDROID_HOME=/path/to/android/sdk

# 编译
javac -d build/classes \
    -cp "$ANDROID_HOME/platforms/android-34/android.jar" \
    -source 1.8 \
    -target 1.8 \
    src/main/java/com/evobot/sequence/*.java

# 打包
jar -cf evobot-sequence-player.jar -C build/classes com
```

## 使用

将编译后的jar或源码集成到你的Android项目。
EOF

# 5. 打包分发
cd ..
tar -czf EvoBotSequencePlayer-Distribution.tar.gz EvoBotSequencePlayer-Distribution/

echo "分发包已创建: EvoBotSequencePlayer-Distribution.tar.gz"
```

---

## 输出文件对比

| 打包方式 | 输出文件 | 大小 | 优点 | 缺点 |
|---------|---------|------|------|------|
| **AAR** | app-release.aar | ~30KB | ✅ 完整（代码+资源+配置）<br>✅ Android IDE友好 | 仅Android环境 |
| **JAR** | evobot-sequence-player.jar | ~20KB | ✅ 纯代码<br>✅ 跨平台 | ❌ 不含资源 |
| **源码** | 源代码文件 | ~30KB | ✅ 完全可控<br>✅ 易于修改 | 需要编译 |

---

## 推荐方案

### 方案A：如果你有Android Studio

```bash
# 1. 用Android Studio打开项目
open -a "Android Studio" EvoBotSequenceLibrary

# 2. 等待Gradle同步

# 3. Build -> Rebuild Project

# 4. 获取AAR
ls app/build/outputs/aar/app-release.aar
```

### 方案B：如果没有Android Studio

**使用源码方式**：

```bash
# 直接复制源码到你的Android项目
cp -r EvoBotSequenceLibrary/app/src/main/java/com/evobot/sequence \
      YourAndroidProject/app/src/main/java/

cp -r EvoBotSequenceLibrary/app/src/main/assets \
      YourAndroidProject/app/src/main/
```

---

## 验证打包结果

```bash
# 查看JAR内容
jar -tf evobot-sequence-player.jar

# 查看AAR内容（AAR是ZIP格式）
unzip -l app-release.aar

# 解压AAR
mkdir -p aar-extracted
unzip app-release.aar -d aar-extracted/
```

---

## 故障排查

### 问题1：找不到Android SDK

```bash
# 解决方案1：设置ANDROID_HOME环境变量
export ANDROID_HOME=/path/to/android/sdk

# 解决方案2：在build.gradle中指定
sdk.dir=/path/to/android/sdk
```

### 问题2：编译失败

```bash
# 检查Java版本
java -version  # 需要 JDK 8+

# 检查文件编码
file app/src/main/java/com/evobot/sequence/*.java

# 重新编译
./gradlew clean
./gradlew :app:assembleRelease
```

### 问题3：AAR无法使用

```bash
# 检查AAR内容
unzip -l app-release.aar

# 应该包含：
# - AndroidManifest.xml
# - R.txt
# - classes.jar
# - res/ (如果有资源)
# - assets/ (.ebs文件应该在这里)
```

---

## 总结

**最快方式**：直接复制源码到Android项目
**最规范方式**：使用Android Studio生成AAR
**最灵活方式**：生成JAR包（需Android SDK）

选择适合你的方式即可！
