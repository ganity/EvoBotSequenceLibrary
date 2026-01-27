# GitHub Actions 自动打包指南

## 📦 使用GitHub云端自动打包

由于本地没有Android环境，可以使用GitHub Actions在云端自动编译Android Library。

---

## 🚀 快速开始

### 步骤1：初始化Git仓库并推送到GitHub

```bash
cd EvoBotSequenceLibrary

# 初始化Git仓库
git init

# 添加所有文件
git add .

# 创建首次提交
git commit -m "Initial commit: EvoBot序列播放器Android Library"

# 在GitHub上创建新仓库（访问 https://github.com/new）
# 仓库名建议: EvoBotSequencePlayer
# 不要初始化README、.gitignore
# 创建后复制仓库URL

# 关联远程仓库（替换YOUR_USERNAME）
git remote add origin https://github.com/YOUR_USERNAME/EvoBotSequencePlayer.git

# 推送到GitHub
git branch -M main
git push -u origin main
```

### 步骤2：���看自动构建

推送代码后，GitHub会自动开始构建：

1. 访问你的GitHub仓库
2. 点击 **Actions** 标签
3. 选择 **Build Android Library** workflow
4. 等待构建完成（约2-3分钟）

### 步骤3：下载构建产物

构建完成后：

1. 在Actions页面，点击成功的workflow run
2. 滚动到页面底部的 **Artifacts** 部分
3. 下载以下文件：
   - **evobot-sequence-aar**: 包含 `.aar` 文件
   - **evobot-sequence-jar**: 包含 `.jar` 文件

---

## 📂 构建产物说明

### AAR文件（推荐用于Android项目）

```
evobot-sequence-aar/
└── app-release.aar  (约30KB)
```

**使用方法**：
```gradle
// 放到项目的 libs/ 目录
repositories {
    flatDir { dirs 'libs' }
}

dependencies {
    implementation(name: 'app-release', ext: 'aar')
}
```

### JAR文件（仅代码）

```
evobot-sequence-jar/
├── evobot-sequence-player.jar       (仅代码，约20KB)
└── evobot-sequence-player-full.jar  (包含assets，约30KB)
```

**使用方法**：
```bash
# 解压到项目
jar -xf evobot-sequence-player-full.jar

# 或直接作为依赖
```

---

## 🔄 手动触发构建

也可以手动触发构建，不需要推送代码：

1. 访问GitHub仓库
2. 点击 **Actions** 标签
3. 选择 **Build Android Library**
4. 点击 **Run workflow**
5. 选择 **release** 或 **debug**
6. 点击 **Run workflow** 按钮

---

## 🔧 自定义构建

### 修改构建配置

编辑 `.github/workflows/build.yml` 文件：

```yaml
# 修改触发条件
on:
  push:
    branches: [ main ]  # 只在main分支触发
  workflow_dispatch:      # 允许手动触发

# 修改Java版本
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'  # 改为其他版本

# 修改产物保留时间
      - name: Upload AAR artifact
        uses: actions/upload-artifact@v4
        with:
          retention-days: 90  # 改为保留天数
```

### 添加版本号

修改 `app/build.gradle`：

```gradle
android {
    defaultConfig {
        versionCode 1
        versionName "1.0.0"
    }
}
```

### 创建Git Tag触发发布

```bash
# 创建版本标签
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# GitHub会自动创建Release并附���构建产物
```

---

## 📊 监控构建状态

### 在项目中显示徽章

在README.md中添加：

```markdown
# EvoBot序列播放器

![Build Status](https://github.com/YOUR_USERNAME/EvoBotSequencePlayer/workflows/Build%20Android%20Library/badge.svg)
```

---

## 🎯 完整工作流示例

```bash
# 1. 开发新功能或修改代码
vim app/src/main/java/com/evobot/sequence/XXX.java

# 2. 测试代码
# 本地运行 SimpleSequenceTest.java

# 3. 提交代码
git add .
git commit -m "Add new feature: XXX"

# 4. 推送到GitHub
git push origin main

# 5. GitHub自动开始构建
# 访问 https://github.com/YOUR_USERNAME/EvoBotSequencePlayer/actions

# 6. 等待构建完成（2-3分钟）

# 7. 下载构建产物
# 点击 Artifacts -> evobot-sequence-aar -> 下载

# 8. 集成到Android项目
# 解压并使用AAR文件
```

---

## 🐛 故障排查

### 构建失败

**原因1**: Gradle配置错误
```bash
# 检查build.gradle语法
cat app/build.gradle

# 本地测试配置（如果有Android环境）
./gradlew :app:assembleDebug --dry-run
```

**原因2**: Java版本不兼容
```yaml
# 修改build.yml中的JDK版本
java-version: '17'  # 改为11或17
```

**原因3**: 依赖问题
```gradle
# 添加显式依赖
dependencies {
    compileOnly 'androidx.annotation:annotation:1.7.1'
}
```

### 无法下载Artifacts

1. 检查Actions权限
2. 确保workflow运行成功
3. 刷新页面
4. 检查Artifacts是否过期（默认保留30天）

---

## 📱 下载后的使用

### 方式1：使用AAR（推荐）

```bash
# 1. 解压下载的zip文件
unzip evobot-sequence-aar.zip

# 2. 复制app-release.aar到Android项目的libs/目录
cp app-release.aar YourAndroidProject/app/libs/

# 3. 在build.gradle中添加依赖
repositories {
    flatDir { dirs 'libs' }
}

dependencies {
    implementation(name: 'app-release', ext: 'aar')
}
```

### 方式2：直接使用源码

```bash
# 1. 下载源码zip（或直接从GitHub克隆）
git clone https://github.com/YOUR_USERNAME/EvoBotSequencePlayer.git

# 2. 复制到你的项目
cp -r EvoBotSequencePlayer/app/src/main/java/com/evobot/sequence \
      YourAndroidProject/app/src/main/java/

cp -r EvoBotSequencePlayer/app/src/main/assets \
      YourAndroidProject/app/src/main/
```

---

## 🎉 完成！

现在你可以：
- ✅ 在本地修改代码
- ✅ 推送到GitHub
- ✅ GitHub自动构建
- ✅ 下载AAR/JAR文件
- ✅ 集成到Android项目

**无需本地Android环境！**

---

## 📞 需要帮助？

如果遇到问题：
1. 查看 [GitHub Actions文档](https://docs.github.com/en/actions)
2. 检查workflow运行日志
3. 提交Issue并提供错误日志
