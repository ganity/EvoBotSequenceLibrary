#!/bin/bash

# RK3399优化构建脚本 - 编译Rust库为Android .so文件

set -e

echo "🚀 Building EvoBot Native Library for RK3399..."

# 检查环境变量
if [ -z "$ANDROID_NDK_ROOT" ]; then
    echo "Error: ANDROID_NDK_ROOT not set"
    echo "Please set: export ANDROID_NDK_ROOT=/path/to/ndk"
    exit 1
fi

# 添加Android目标
echo "📱 Adding Android targets..."
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi

# 安装cargo-ndk
if ! command -v cargo-ndk &> /dev/null; then
    echo "📦 Installing cargo-ndk..."
    cargo install cargo-ndk
fi

# RK3399优化构建 - 主要针对ARM64
echo "🔥 Building optimized for RK3399 ARM64 (Cortex-A72)..."
RUSTFLAGS="-C target-cpu=cortex-a72 -C target-feature=+neon" \
cargo ndk -t aarch64-linux-android build --profile release-rk3399

echo "📱 Building for ARMv7 compatibility..."
cargo ndk -t armv7-linux-androideabi build --release

# 创建输出目录
mkdir -p ../app/src/main/jniLibs/arm64-v8a
mkdir -p ../app/src/main/jniLibs/armeabi-v7a

# 复制.so文件到Android项目
echo "📂 Copying optimized .so files to Android project..."

# 使用RK3399优化版本
if [ -f "target/aarch64-linux-android/release-rk3399/libevobot_sequence_native.so" ]; then
    cp target/aarch64-linux-android/release-rk3399/libevobot_sequence_native.so ../app/src/main/jniLibs/arm64-v8a/
    echo "✅ RK3399 optimized ARM64 library copied"
else
    cp target/aarch64-linux-android/release/libevobot_sequence_native.so ../app/src/main/jniLibs/arm64-v8a/
    echo "⚠️  Standard ARM64 library copied (RK3399 optimized version not found)"
fi

cp target/armv7-linux-androideabi/release/libevobot_sequence_native.so ../app/src/main/jniLibs/armeabi-v7a/

# 显示文件信息
echo "📊 Library information:"
echo "ARM64 (RK3399 optimized):"
ls -lh ../app/src/main/jniLibs/arm64-v8a/libevobot_sequence_native.so
echo "ARMv7 (compatibility):"
ls -lh ../app/src/main/jniLibs/armeabi-v7a/libevobot_sequence_native.so

echo "✅ RK3399 optimized build completed successfully!"
echo "🎯 Libraries are ready for RK3399 deployment in app/src/main/jniLibs/"
echo ""
echo "RK3399 Optimization Features:"
echo "  🔥 Cortex-A72 specific optimizations"
echo "  ⚡ NEON SIMD instructions enabled"
echo "  🧠 Big.LITTLE core awareness"
echo "  📈 Adaptive timing algorithms"
echo "  🎛️  Performance monitoring"