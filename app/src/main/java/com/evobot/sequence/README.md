# EvoBot Sequence Library - Core Files

这个目录包含了EvoBot序列库的核心文件。

## 核心组件

### 播放器核心
- `EvoBotSequencePlayer.java` - 主要的序列播放器类
- `SequenceListener.java` - 序列播放监听器接口
- `PlayerState.java` - 播放器状态枚举

### 数据处理
- `SequenceData.java` - 序列数据结构
- `SequenceLoader.java` - 序列文件加载器

### 动作库管理
- `ActionLibraryManager.java` - 动作库管理器
- `ActionLibraryClient.java` - HTTP客户端
- `ActionLibraryConfig.java` - 配置类
- `ActionLibraryUpdater.java` - 更新管理器
- `ActionCacheManager.java` - 缓存管理器

### 工具类
- `ActionNameUtils.java` - 动作名称映射工具
- `ActionInfo.java` - 动作信息类

## 测试和示例

所有测试文件和示例代码已移动到 `app/src/test/java/com/evobot/sequence/` 目录中。

## 使用方法

```java
// 基本使用示例
Context context = getApplicationContext();
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context);

player.play("arm_movement_left_arm_wave", new SequenceListener() {
    @Override
    public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
        // 处理帧数据
    }
    
    @Override
    public void onComplete() {
        // 播放完成
    }
    
    @Override
    public void onError(String errorMessage) {
        // 处理错误
    }
    
    @Override
    public void onEmergencyStop() {
        // 急停处理
    }
});
```