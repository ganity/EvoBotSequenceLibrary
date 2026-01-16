# 构建指南

## 前置要求

- JDK 8 或更高版本
- Android SDK Build Tools 34.0.0
- Gradle 8.2.0

## 构建步骤

### 1. 生成AAR库

```bash
cd EvoBotSequenceLibrary
./gradlew :app:assembleRelease
```

输出文件:
- `app/build/outputs/aar/app-release.aar`

### 2. 生成JAR文件

```bash
./gradlew :app:jar
```

输出文件:
- `app/build/outputs/jar/evobot-sequence-player.jar` (仅代码)
- `app/build/outputs/jar/evobot-sequence-player-full.jar` (包含资源)

### 3. 导入到其他项目

#### 使用AAR

```gradle
repositories {
    flatDir {
        dirs 'libs'  // 将app-release.aar放入项目的libs目录
    }
}

dependencies {
    implementation(name: 'app-release', ext: 'aar')
}
```

#### 使用JAR

将 `evobot-sequence-player.jar` 和 assets目录复制到项目中。

## 项目结构

```
EvoBotSequenceLibrary/
├── app/
│   ├── src/main/
│   │   ├── java/com/evobot/sequence/
│   │   │   ├── EvoBotSequencePlayer.java    # 核心播放器
│   │   │   ├── SequenceLoader.java          # 二进制加载器
│   │   │   ├── SequenceListener.java        # 回调接口
│   │   │   ├── SequenceData.java            # 数据模型
│   │   │   └── PlayerState.java             # 状态枚举
│   │   ├── assets/sequences/
│   │   │   └── 左臂挥手右臂掐腰抱胸_20260116_142711.ebs  # 序列数据
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

## 常见问题

### Q: 如何修改内置序列？
A: 替换 `app/src/main/assets/sequences/` 目录下的.ebs文件，并更新代码中的文件名常量。

### Q: 如何支持多个动作？
A: 在assets中放入多个.ebs文件，通过actionName参数映射到不同的文件。

### Q: 播放不流畅怎么办？
A: 检查频率设置，建议不超过50Hz。确保onFrameData回调中不执行耗时操作。

### Q: 如何在后台播放？
A: 使用Foreground Service，并在Service中创建播放器实例。
