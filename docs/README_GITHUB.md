# 使用GitHub Actions自动打包Android Library

## 🎯 为什么使用GitHub Actions？

✅ **无需本地Android环境**
- 不需要安装Android Studio
- 不需要下载Android SDK
- 不需要配置本地Gradle

✅ **云端自动构建**
- 推送代码自动触发构建
- 下载现成的AAR/JAR文件
- 节省本地编译时间

✅ **CI/CD集成**
- 自动测试
- 自动发布
- 版本管理

---

## 🚀 三步开始

### 第一步：准备GitHub仓库

#### 方式A：使用快速脚本（推荐）

```bash
cd EvoBotSequenceLibrary
chmod +x setup_github.sh
./setup_github.sh
```

脚本会自动：
1. 初始化Git仓库
2. 创建.gitignore
3. 提交代码
4. 引导你创建GitHub仓库并推送

#### 方式B：手动操作

```bash
# 1. 初始化Git
cd EvoBotSequenceLibrary
git init
git add .
git commit -m "Initial commit: EvoBot序列播放器"

# 2. 在GitHub创建新仓库
# 访问: https://github.com/new
# 仓库名: EvoBotSequencePlayer
# ❌ 不要初始化README和.gitignore

# 3. 关联远程仓库并推送
git remote add origin https://github.com/YOUR_USERNAME/EvoBotSequencePlayer.git
git branch -M main
git push -u origin main
```

### 第二步：等待自动构建

推送代码后，GitHub会自动：

1. ✅ 检测到代码推送
2. ✅ 启动Android Library构建
3. ✅ 编译Java代码
4. ✅ 打包AAR和JAR
5. ✅ 上传构建产物

**等待时间**：约2-3分钟

### 第三步：下载构建产物

1. 访问GitHub仓库页面
2. 点击 **Actions** 标签
3. 点击最新的 **Build Android Library** workflow
4. 滚动到页面底部的 **Artifacts** 区域
5. 下载文件：
   - **evobot-sequence-aar** - AAR文件（推荐）
   - **evobot-sequence-jar** - JAR文件

---

## 📦 产物使用指南

### 下载AAR文件后的使用

```bash
# 1. 解压下载的zip
unzip evobot-sequence-aar.zip

# 2. 复制到Android项目
cp app-release.aar YourAndroidProject/app/libs/

# 3. 在build.gradle中添加依赖
```

```gradle
repositories {
    flatDir { dirs 'libs' }
}

dependencies {
    implementation(name: 'app-release', ext: 'aar')
}
```

### 下载JAR文件后的使用

```bash
# 解压JAR（包含源码和资源）
jar -xf evobot-sequence-player-full.jar

# 或直接作为依赖使用
```

---

## 🔄 持续集成工作流

### 修改代码后的流程

```bash
# 1. 修改代码
vim app/src/main/java/com/evobot/sequence/EvoBotSequencePlayer.java

# 2. 本地测试（可选）
javac app/src/main/java/com/evobot/sequence/SimpleSequenceTest.java
java com.evobot.sequence.SimpleSequenceTest

# 3. 提交并推送
git add .
git commit -m "fix: 修复定时精度问题"
git push origin main

# 4. GitHub自动构建
# 5. 下载新的构建产物
```

### 发布新版本

```bash
# 1. 创建版本标签
git tag -a v1.0.0 -m "Release v1.0.0: 稳定版本"
git push origin v1.0.0

# 2. GitHub自动创建Release并附带构建产物
```

---

## 🎛️ 自定义配置

### 修改构建触发条件

编辑 `.github/workflows/build.yml`：

```yaml
on:
  push:
    branches: [ main ]           # 只在main分支触发
    paths:                       # 只在特定文件改变时触发
      - 'app/src/**'
      - '.github/workflows/**'
  workflow_dispatch:              # 允许手动触发
```

### 修改产物保留时间

```yaml
- name: Upload AAR artifact
  uses: actions/upload-artifact@v4
  with:
    retention-days: 90  # 默认30天，改为90天
```

### 添加自动化测试

```yaml
      - name: Run Tests
        run: ./gradlew :app:test

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: app/build/reports/tests/
```

---

## 📊 监控构建状态

### 在项目README中添加徽章

```markdown
# EvoBot序列播放器

![Build Status](https://github.com/YOUR_USERNAME/EvoBotSequencePlayer/workflows/Build%20Android%20Library/badge.svg)

[![Download](https://img.shields.io/badge/download-AAR-brightgreen)](https://github.com/YOUR_USERNAME/EvoBotSequencePlayer/actions)
```

### 订阅构建通知

1. 访问GitHub仓库设置
2. 点击 **Notifications**
3. 配置Actions通知：
   - Email通知
   - Webhook通知
   - GitHub移动App通知

---

## 🐛 常见问题

### Q1: 构建失败怎么办？

**A**: 检查构建日志

1. 访问Actions页面
2. 点击失败的workflow run
3. 展开失败的步骤查看日志
4. 修复问题后重新推送

常见失败原因：
- 语法错误
- 配置文件错误
- 依赖问题

### Q2: 如何手动触发构建？

**A**: 使用workflow dispatch

1. 访问Actions标签
2. 选择 **Build Android Library**
3. 点击 **Run workflow** 按钮
4. 选择构建类型（debug/release）
5. 点击绿色按钮开始构建

### Q3: 构建产物过期了怎么办？

**A**: 重新构建或增加保留时间

**方案1**: 重新触发构建
```bash
# 创建空提交触发构建
git commit --allow-empty -m "trigger rebuild"
git push origin main
```

**方案2**: 修改`.github/workflows/build.yml`
```yaml
retention-days: 90  # 增加保留天数
```

### Q4: 如何下载历史版本的构建产物？

**A**: 构建产物按workflow run保存

1. 访问Actions页面
2. 找到对应版本的workflow run
3. 下载该run的Artifacts

**注意**: 产物有保留期限（默认30天）

---

## 💡 最佳实践

### 1. 版本管理

```bash
# 使用Git Tag管理版本
git tag -a v1.0.0 -m "稳定版本"
git push origin v1.0.0

# 查看所有标签
git tag

# 检出特定版本
git checkout v1.0.0
```

### 2. 分支策略

```bash
# main分支 - 稳定版本
git checkout main

# dev分支 - 开发版本
git checkout -b dev

# 功能分支
git checkout -b feature/add-seek-function
```

### 3. 提交信息规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
test: 测试相关
chore: 构建/工具链
```

---

## 📚 相关文档

- [GitHub Actions文档](https://docs.github.com/en/actions)
- [Android Library发布](https://developer.android.com/studio/projects#CreateLibrary)
- [AAR文件格式](https://developer.android.com/studio/projects/android-library#AAR)

---

## 🎉 开始使用

现在你就可以：

1. ✅ 运行 `./setup_github.sh` 快速设置
2. ✅ 推送代码到GitHub
3. ✅ 等待自动构建完成
4. ✅ 下载现成的AAR/JAR文件
5. ✅ 集成到Android项目

**完全不需要本地Android环境！**

---

## 📞 需要帮助？

- 查看完整指南: [GITHUB_ACTIONS_GUIDE.md](GITHUB_ACTIONS_GUIDE.md)
- 检查构建日志: GitHub Actions页面
- 提交Issue: GitHub Issues
