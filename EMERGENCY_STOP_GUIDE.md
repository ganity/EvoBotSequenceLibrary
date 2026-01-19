# 急停功能使用指南

## 概述

新增的急停功能允许在序列播放过程中立即停止 SequenceListener 的位置输出，确保机器人在紧急情况下能够安全停止。

## 功能特性

- **立即响应**: 调用 `emergencyStop()` 方法后立即停止所有播放任务
- **回调通知**: 通过 `onEmergencyStop()` 回调通知监听器执行急停操作
- **状态管理**: 自动将播放器状态设置为 STOPPED
- **资源清理**: 清理所有定时任务和播放参数

## API 说明

### EvoBotSequencePlayer 新增方法

```java
/**
 * 急停方法
 * 立即停止序列播放并通知监听器停止位置输出
 */
public void emergencyStop()
```

### SequenceListener 新增回调

```java
/**
 * 急停回调
 * 当调用急停方法后立即回调，通知监听器停止位置输出
 */
void onEmergencyStop();
```

## 使用示例

### 1. 实现 SequenceListener

```java
SequenceListener listener = new SequenceListener() {
    @Override
    public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
        // 正常播放时的位置输出
        sendPositionToRobot(leftArm, rightArm);
    }

    @Override
    public void onComplete() {
        Log.d(TAG, "播放完成");
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(TAG, "播放错误: " + errorMessage);
    }

    @Override
    public void onEmergencyStop() {
        // 急停回调 - 立即停止位置输出
        Log.w(TAG, "收到急停信号，立即停止机器人运动");
        stopRobotMovement();
    }
};
```

### 2. 触发急停

```java
// 开始播放
player.play("动作名称", 40, listener);

// 在需要时触发急停
if (emergencyConditionDetected()) {
    player.emergencyStop();  // 立即停止并触发 onEmergencyStop() 回调
}
```

### 3. 完整示例

参考 `EmergencyStopExample.java` 文件查看完整的使用示例。

## 实现细节

### 急停执行流程

1. 调用 `emergencyStop()` 方法
2. 立即移除所有定时回调任务
3. 设置播放器状态为 STOPPED
4. 重置播放参数（当前帧、时间戳等）
5. 调用监听器的 `onEmergencyStop()` 回调
6. 记录急停日志

### 与普通 stop() 的区别

| 方法 | 用途 | 回调 | 日志级别 |
|------|------|------|----------|
| `stop()` | 正常停止播放 | 无特殊回调 | DEBUG |
| `emergencyStop()` | 紧急停止 | `onEmergencyStop()` | WARNING |

## 安全建议

1. **立即响应**: 在 `onEmergencyStop()` 回调中立即停止向机器人发送位置指令
2. **安全位置**: 可选择让机器人保持当前位置或移动到安全位置
3. **事件记录**: 记录急停事件的时间、原因和上下文信息
4. **系统通知**: 通知其他系统组件急停事件的发生

## 测试

所有现有测试已更新以支持新的 `onEmergencyStop()` 回调方法。运行测试确保功能正常：

```bash
./gradlew test
```

## 注意事项

- 急停功能是不可逆的，调用后需要重新开始播放
- 确保在 `onEmergencyStop()` 回调中实现适当的安全措施
- 急停不会触发 `onComplete()` 回调
- 建议在关键安全场景中使用急停功能