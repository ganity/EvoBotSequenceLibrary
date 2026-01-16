# EvoBot序列播放器 Android Library

## 简介

这是一个Android库，用于播放EvoBot机器人的动作序列。库内置了编译好的序列数据（.ebs二进制格式），可以按照指定频率（如40Hz）定时回调关节位置数据。

## 功能特性

- ✅ 解析.ebs二进制序列文件
- ✅ 按指定频率播放（默认40Hz，可配置1-100Hz）
- ✅ 通过Listener接口回调关节数据
- ✅ 支持暂停/恢复/停止
- ✅ 支持帧级跳转（seek）
- ✅ 基于Android Handler的精确定时
- ✅ 支持误差补偿
- ✅ 完整的状态管理

## 集成方式

### 方式1：使用AAR文件

```gradle
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    implementation(name: 'app-release', ext: 'aar')
}
```

### 方式2：直接复制源码

将 `app/src/main/java/com/evobot/sequence/` 目录下的所有Java文件复制到你的项目中。

## 使用方法

### 基本使用

```java
import com.evobot.sequence.EvoBotSequencePlayer;
import com.evobot.sequence.SequenceListener;

// 1. 创建播放器实例
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context);

// 2. 播放序列（使用默认40Hz频率）
player.play("左臂挥手右臂掐腰抱胸", new SequenceListener() {
    @Override
    public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
        // 接收到一帧数据
        // leftArm[0-9]: 左臂10个关节的位置
        // rightArm[0-9]: 右臂10个关节的位置
        // frameIndex: 当前帧索引（从0开始）

        // TODO: 通过串口/蓝牙发送到机器人
        sendToRobot(leftArm, rightArm);
    }

    @Override
    public void onComplete() {
        Log.d(TAG, "播放完成");
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(TAG, "播放错误: " + errorMessage);
    }
});
```

### 自定义播放频率

```java
// 使用50Hz频率播放
player.play("动作名称", 50, listener);
```

### 播放控制

```java
// 暂停播放
player.pause();

// 恢复播放
player.resume();

// 停止播放
player.stop();

// 跳转到指定帧
player.seek(200);  // 跳转到第200帧
```

### 状态查询

```java
// 获取当前状态
PlayerState state = player.getState();

// 获取播放进度
int currentFrame = player.getCurrentFrame();
int totalFrames = player.getTotalFrames();
float progress = player.getProgress();  // 0.0 - 1.0

// 获取序列信息
String info = player.getSequenceInfo();
```

### 资源释放

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    player.release();  // 释放资源
}
```

## 数据格式说明

### 关节位置数组

- `leftArm[0-9]`: 左臂关节0到关节9的位置
- `rightArm[0-9]`: 右臂关节0到关节9的位置
- 每个关节的范围：`0-4095`，特殊值 `-1` 表示保持当前位置

### 播放频率

- **默认频率**: 40Hz（每25ms一帧）
- **推荐频率**: 20-50Hz
- **支持范围**: 1-100Hz
- **计算公式**: `间隔(ms) = 1000 / 频率(Hz)`

### .ebs文件格式

```
文件头 (96 bytes):
- 魔数: "EBS1" (4 bytes)
- 帧数: uint32 (4 bytes)
- 采样率: float (4 bytes)
- 总时长: float (4 bytes)
- 编译时间: uint32 (4 bytes)
- 保留字段: 12 bytes
- 序列名称: 64 bytes (UTF-8)

数据区 (每帧40 bytes):
- 左臂关节0-9: 各2字节 (uint16)
- 右臂关节0-9: 各2字节 (uint16)
- 特殊值: 0xFFFF 表示 -1 (保持当前位置)
```

## 构建与打包

### 生成AAR文件

```bash
./gradlew :app:assembleRelease
# 输出: app/build/outputs/aar/app-release.aar
```

### 生成JAR文件

```bash
./gradlew :app:jar
# 输出: app/build/outputs/jar/evobot-sequence-player.jar

./gradlew :app:fatJar
# 输出: app/build/outputs/jar/evobot-sequence-player-full.jar (包含assets)
```

## API文档

### EvoBotSequencePlayer

| 方法 | 说明 |
|------|------|
| `play(actionName, listener)` | 使用默认40Hz播放 |
| `play(actionName, frequency, listener)` | 使用指定频率播放 |
| `pause()` | 暂停播放 |
| `resume()` | 恢复播放 |
| `stop()` | 停止播放 |
| `seek(frameIndex)` | 跳转到指定帧 |
| `getState()` | 获取当前状态 |
| `getCurrentFrame()` | 获取当前帧索引 |
| `getTotalFrames()` | 获取总帧数 |
| `getProgress()` | 获取播放进度(0-1) |
| `getSequenceInfo()` | 获取序列信息 |
| `release()` | 释放资源 |

### PlayerState

| 状态 | 说明 |
|------|------|
| `IDLE` | 空闲 |
| `LOADING` | 加载中 |
| `READY` | 就绪 |
| `PLAYING` | 播放中 |
| `PAUSED` | 已暂停 |
| `STOPPED` | 已停止 |
| `ERROR` | 错误 |

### SequenceListener

| 方法 | 说明 |
|------|------|
| `onFrameData(leftArm, rightArm, frameIndex)` | 帧数据回调 |
| `onComplete()` | 播放完成回调 |
| `onError(errorMessage)` | 错误回调 |

## 系统要求

- **minSdk**: 21 (Android 5.0)
- **targetSdk**: 34
- **compileSdk**: 34

## 依赖

无外部依赖，纯Android SDK实现。

## 注意事项

1. **回调在主线程**: `onFrameData()` 回调在主线程执行，需要快速处理，避免阻塞UI
2. **资源释放**: 务必在Activity/Fragment销毁时调用 `release()`
3. **频率限制**: 频率过高可能导致性能问题，建议不超过50Hz
4. **-1处理**: 接收到-1时表示保持该关节当前位置，不需要发送

## 示例应用

完整示例代码请参考：
```java
public class MainActivity extends AppCompatActivity {

    private EvoBotSequencePlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        player = new EvoBotSequencePlayer(this);
        player.play("左臂挥手右臂掐腰抱胸", 40, new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                // 发送到机器人
                String command = formatCommand(leftArm, rightArm);
                sendToSerialPort(command);
            }

            @Override
            public void onComplete() {
                Toast.makeText(MainActivity.this, "播放完成", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "错误: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
}
```

## 许可证

版权 © 2024 EvoBot
