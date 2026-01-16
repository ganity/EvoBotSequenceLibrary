#!/bin/bash

# GitHub快速设置脚本
# 帮助快速初始化Git仓库并推送到GitHub

echo "========================================"
echo "EvoBot序列播放器 - GitHub快速设置"
echo "========================================"
echo ""

# 检查是否在项目根目录
if [ ! -f "build.gradle" ] && [ ! -f "app/build.gradle" ]; then
    echo "❌ 请在EvoBotSequenceLibrary根目录下运行此脚本"
    exit 1
fi

# 初始化Git仓库
if [ ! -d ".git" ]; then
    echo "[1/5] 初始化Git仓库..."
    git init
    echo "✅ Git仓库初始化完成"
else
    echo "[1/5] Git仓库已存在，跳过初始化"
fi

echo ""

# 创建.gitignore
echo "[2/5] 创建.gitignore..."
cat > .gitignore << 'EOF'
# Build outputs
build/
.gradle/
app/build/
local.properties

# Android generated files
*.apk
*.ap_
*.aab
*.aar

# Java
*.class
*.jar
*.war

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# Logs
*.log

# Test outputs
playback_log_*.csv
test_reports/

# Gradle Wrapper (可选，如果使用wrapper就不注释)
# !gradle/wrapper/gradle-wrapper.jar
EOF
echo "✅ .gitignore创建完成"
echo ""

# 添加所有文件
echo "[3/5] 添加文件到Git..."
git add .
echo "✅ 文件添加完成"
echo ""

# 创建首次提交
echo "[4/5] 创建提交..."
git commit -m "feat: EvoBot��列播放器Android Library

- 实现完整播放器（40Hz定时，误差补偿）
- 支持暂停/恢复/停止/seek控制
- 解析.ebs二进制格式（29KB，497帧）
- 完整的状态管理和错误处理
- 包含测试类和使用文档
- 配置GitHub Actions自动构建
"
echo "✅ 提交完成"
echo ""

# 询问GitHub仓库信息
echo "[5/5] 推送到GitHub"
echo ""
echo "请按以下步骤操作："
echo ""
echo "1. 在浏览器中访问: https://github.com/new"
echo "2. 创建新仓库，仓库名建议: EvoBotSequencePlayer"
echo "3. ❌ 不要勾选 'Add a README file'"
echo "4. ❌ 不要勾选 'Add .gitignore'"
echo "5. 点击 'Create repository'"
echo ""
echo "创建后，GitHub会显示仓库地址，例如:"
echo "   https://github.com/YOUR_USERNAME/EvoBotSequencePlayer.git"
echo ""

read -p "输入你的GitHub仓库URL: " REPO_URL

if [ -z "$REPO_URL" ]; then
    echo "❌ 未输入仓库URL，取消操作"
    echo ""
    echo "提示: 你可以稍后手动执行:"
    echo "  git remote add origin https://github.com/YOUR_USERNAME/EvoBotSequencePlayer.git"
    echo "  git branch -M main"
    echo "  git push -u origin main"
    exit 0
fi

# 添加远程仓库并推送
echo ""
echo "推送代码到GitHub..."
git remote add origin "$REPO_URL" 2>/dev/null || git remote set-url origin "$REPO_URL"
git branch -M main
git push -u origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "✅ 成功推送到GitHub！"
    echo "========================================"
    echo ""
    echo "下一步操作:"
    echo "1. 访问GitHub仓库页面"
    echo "2. 点击 'Actions' 标签"
    echo "3. 查看 'Build Android Library' workflow"
    echo "4. 等待构建完成（约2-3分钟）"
    echo "5. 下载构建产物（AAR和JAR文件）"
    echo ""
    echo "构建完成后，你可以在Actions页面的Artifacts部分下载："
    echo "  - evobot-sequence-aar: 包含AAR文件（约30KB）"
    echo "  - evobot-sequence-jar: 包含JAR文件（约20-30KB）"
    echo ""
else
    echo ""
    echo "❌ 推送失败"
    echo ""
    echo "可能的原因:"
    echo "1. 仓库URL不正确"
    echo "2. 未设置GitHub SSH密钥"
    echo "3. 未设置GitHub身份验证"
    echo ""
    echo "解决方法:"
    echo "1. 检查仓库URL是否正确"
    echo "2. 访问 https://github.com/settings/tokens 生成Personal Access Token"
    echo "3. 使用Token作为密码推送:"
    echo "   git push https://YOUR_TOKEN@github.com/YOUR_USERNAME/EvoBotSequencePlayer.git main"
fi
