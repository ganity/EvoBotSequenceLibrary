# EvoBot序列播放器 - API接口文档

## 目录
1. [简介](#简介)
2. [初始化](#初始化)
3. [播放控制](#播放控制)
4. [播放器状态](#播放器状态)
5. [动作库管理](#动作库管理)
6. [监听器接口](#监听器接口)
7. [错误处理](#错误处理)
8. [Native功能](#native功能)

---

## 简介

`EvoBotSequencePlayer` 是一个Android序列播放器，支持按指定频率（默认40Hz）播放动作序列数据，支持本地assets和HTTP动作库两种加载方式。

### 主要特性
- 支持中英文名称自动映射
- 支持动作库热更新
- Native性能优化（RK3399平台）
- -1值填充（保持关节连续性）
- 急停保护

### 坐标系统
- **左臂数据**: 10个关节，每个关节范围 0-4095
- **右臂数据**: 10个关节，每个关节范围 0-4095
- **-1值含义**: 保持当前位置，不改变关节角度

---

## 初始化

### 构造函数1: 仅本地模式

```java
EvoBotSequencePlayer(Context context)
```

**说明**: 仅从本地assets加载动作序列

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| context | Context | 是 | Android上下文 |

**示例**:
```java
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context);
```

---

### 构造函数2: 支持HTTP动作库（内部存储）

```java
EvoBotSequencePlayer(Context context, ActionLibraryConfig actionLibraryConfig)
```

**说明**: 支持从HTTP服务器下载动作库，缓存到内部存储

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| context | Context | 是 | Android上下文 |
| actionLibraryConfig | ActionLibraryConfig | 是 | 动作库配置 |

**示例**:
```java
ActionLibraryConfig config = ActionLibraryConfig.createDefault();
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config);
```

---

### 构造函数3: 支持HTTP动作库（指定存储位置）

```java
EvoBotSequencePlayer(Context context, ActionLibraryConfig actionLibraryConfig,
                    ActionLibraryUpdater.StorageLocation storageLocation)
```

**说明**: 支持从HTTP服务器下载动作库，指定存储位置

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| context | Context | 是 | Android上下文 |
| actionLibraryConfig | ActionLibraryConfig | 是 | 动作库配置 |
| storageLocation | StorageLocation | 是 | 存储位置 |

**存储位置类型**:
| 值 | 说明 |
|------|------|
| `ActionLibraryUpdater.StorageLocation.INTERNAL_FILES` | 内部文件目录 `/data/data/包名/files/action_library` |
| `ActionLibraryUpdater.StorageLocation.EXTERNAL_PUBLIC` | 公共外部存储 `/Android/data/包名/files/action_library` |

**示例**:
```java
ActionLibraryConfig config = ActionLibraryConfig.createDefault()
    .setBaseUrl("http://your-server.com/api/v1")
    .setRobotId("EVOBOT-PRD-00000001");

EvoBotSequencePlayer player = new EvoBotSequencePlayer(
    context,
    config,
    ActionLibraryUpdater.StorageLocation.INTERNAL_FILES
);
```

---

## 播放控制

### play - 播放序列（默认40Hz）

```java
public void play(String actionName, SequenceListener listener)
```

**说明**: 使用默认40Hz频率播放指定的动作序列

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| actionName | String | 是 | 动作名称（支持中文或英文） |
| listener | SequenceListener | 是 | 回调监听器 |

**actionName示例**:
- 英文: `arm_movement_left_arm_wave`
- 中文: `左臂挥手`

**返回值**: 无

**示例**:
```java
player.play("左臂挥手", new SequenceListener() {
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
        // 错误处理
    }

    @Override
    public void onEmergencyStop() {
        // 急停
    }
});
```

---

### play - 播放序列（指定频率）

```java
public void play(final String actionName, final int frequency, final SequenceListener listener)
```

**说明**: 指定播放频率播放动作序列

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| actionName | String | 是 | 动作名称 |
| frequency | int | 是 | 播放频率（Hz），范围 1-100 |
| listener | SequenceListener | 是 | 回调监听器 |

**频率说明**:
| 频率范围 | 说明 |
|----------|------|
| 1-100Hz | 推荐范围 40Hz |
| >100Hz | 仅RK3399平台支持 |

**返回值**: 无

**示例**:
```java
player.play("arm_movement_wave_right_arm", 40, listener);
```

---

### pause - 暂停播放

```java
public void pause()
```

**说明**: 暂停当前正在播放的序列

**返回值**: 无

**前提条件**: 当前状态必须为 `PLAYING`

**示例**:
```java
player.pause();
```

---

### resume - 恢复播放

```java
public void resume()
```

**说明**: 恢复暂停的播放

**返回值**: 无

**前提条件**: 当前状态必须为 `PAUSED`

**示例**:
```java
player.resume();
```

---

### stop - 停止播放

```java
public void stop()
```

**说明**: 停止当前播放，重置到第一帧

**返回值**: 无

**示例**:
```java
player.stop();
```

---

### emergencyStop - 急停

```java
public void emergencyStop()
```

**说明**: 立即停止所有播放并回调 `onEmergencyStop()`

**返回值**: 无

**安全特性**: 立即停止播放，不受频率控制

**示例**:
```java
player.emergencyStop();
```

---

### seek - 跳转到指定帧

```java
public void seek(int frameIndex)
```

**说明**: 跳转到序列的指定帧

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| frameIndex | int | 是 | 目标帧索引（从0开始） |

**返回值**: 无

**示例**:
```java
player.seek(100);  // 跳转到第101帧
```

---

## 播放器状态

### getState - 获取当前状态

```java
public PlayerState getState()
```

**说明**: 获取当前播放器的状态

**返回值**: `PlayerState` - 当前状态

**状态列表**:
| 状态 | 值 | 说明 |
|------|-----|------|
| IDLE | "空闲" | 已初始化，未加载序列 |
| LOADING | "加载中" | 正在加载序列 |
| READY | "就绪" | 序列已加载完成 |
| PLAYING | "播放中" | 正在播放 |
| PAUSED | "已暂停" | 已暂停 |
| STOPPED | "已停止" | 已停止 |
| ERROR | "错误" | 发生错误 |

**示例**:
```java
PlayerState state = player.getState();
if (state == PlayerState.PLAYING) {
    // 正在播放
}
```

---

### getCurrentFrame - 获取当前帧

```java
public int getCurrentFrame()
```

**说明**: 获取当前播放的帧索引

**返回值**: int - 当前帧索引，未加载返回 -1

**示例**:
```java
int frame = player.getCurrentFrame();
```

---

### getTotalFrames - 获取总帧数

```java
public int getTotalFrames()
```

**说明**: 获取序列的总帧数

**返回值**: int - 总帧数，未加载返回 0

**示例**:
```java
int totalFrames = player.getTotalFrames();
```

---

### getProgress - 获取播放进度

```java
public float getProgress()
```

**说明**: 获取播放进度（0.0-1.0）

**返回值**: float - 进度值，未加载返回 0

**示例**:
```java
float progress = player.getProgress();
// 0.0 = 开始, 1.0 = 完成
```

---

### getSequenceInfo - 获取序列信息

```java
public String getSequenceInfo()
```

**说明**: 获取当前序列的详细信息

**返回值**: String - 序列信息字符串

**示例**:
```java
String info = player.getSequenceInfo();
```

---

## 动作库管理

### getAllAvailableActions - 获取所有可用动作

```java
public List<ActionInfo> getAllAvailableActions()
```

**说明**: 获取所有可用的动作列表（包括本地assets和下载的动作库）

**返回值**: `List<ActionInfo>` - 动作信息列表

**ActionInfo字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 动作编号 |
| name | String | 中文名称 |
| englishName | String | 英文名称 |
| description | String | 描述 |
| category | String | 类别（本地/下载） |
| subCategory | String | 子类别 |
| version | String | 版本号 |
| fileHash | String | 文件哈希 |
| fileSize | long | 文件大小（字节） |
| fileName | String | 文件名 |
| isPublic | boolean | 是否公开 |
| status | String | 状态 |
| lastModified | long | 最后修改时间戳 |

**示例**:
```java
List<ActionInfo> actions = player.getAllAvailableActions();
for (ActionInfo action : actions) {
    Log.d(TAG, "动作: " + action.englishName + " (" + action.name + ")");
}
```

---

### getAllAvailableActionsAsync - 异步获取所有动作

```java
public void getAllAvailableActionsAsync(ActionListCallback callback)
```

**说明**: 异步获取所有可用动作列表

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| callback | ActionListCallback | 是 | 回调接口 |

**ActionListCallback接口**:
```java
public interface ActionListCallback {
    void onSuccess(List<ActionInfo> actions);
    void onError(String error);
}
```

**示例**:
```java
player.getAllAvailableActionsAsync(new ActionListCallback() {
    @Override
    public void onSuccess(List<ActionInfo> actions) {
        // 成功，获取到动作列表
    }

    @Override
    public void onError(String error) {
        // 失败
    }
});
```

---

### findActionByName - 按名称查找动作

```java
public ActionInfo findActionByName(String actionName)
```

**说明**: 根据动作名称查找动作信息

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| actionName | String | 是 | 动作名称（支持中文或英文） |

**返回值**: `ActionInfo` - 动作信息，未找到返回 null

**匹配规则**:
- 支持中文和英文名称匹配
- 自动转换映射关系

**示例**:
```java
ActionInfo action = player.findActionByName("左臂挥手");
if (action != null) {
    player.play(action.englishName, listener);
}
```

---

### getActionNameMappings - 获取名称映射

```java
public String getActionNameMappings()
```

**说明**: 获取当前所有的中英文名称映射关系

**返回值**: String - 映射信息字符串

**示例**:
```java
String mappings = player.getActionNameMappings();
Log.d(TAG, mappings);
```

---

### getActionLibraryStats - 获取统计信息

```java
public ActionLibraryStats getActionLibraryStats()
```

**说明**: 获取动作库统计信息

**返回值**: `ActionLibraryStats` - 统计信息对象

**ActionLibraryStats字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| totalActionCount | int | 总动作数量 |
| localActionCount | int | 本地动作数量 |
| downloadedActionCount | int | 下载动作数量 |
| totalDownloadedSize | long | 下载动作总大小（字节） |
| mappingCount | int | 映射关系数量 |
| currentVersion | String | 当前版本 |
| storageInfo | String | 存储信息 |

**示例**:
```java
ActionLibraryStats stats = player.getActionLibraryStats();
Log.d(TAG, "总动作数: " + stats.totalActionCount);
Log.d(TAG, "下载大小: " + stats.totalDownloadedSize);
```

---

### checkForUpdatesOnFirstLaunch - 首次启动检查更新

```java
public void checkForUpdatesOnFirstLaunch(ActionLibraryUpdater.UpdateCallback callback)
```

**说明**: 首次启动时检查动作库更新（异步，不阻塞启动）

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| callback | UpdateCallback | 是 | 回调接口 |

**UpdateCallback接口**:
```java
public interface UpdateCallback {
    void onSuccess(boolean hasUpdate);
    void onError(String error);
    void onNoUpdateNeeded();
}
```

**返回值**: 无

**示例**:
```java
player.checkForUpdatesOnFirstLaunch(new ActionLibraryUpdater.UpdateCallback() {
    @Override
    public void onSuccess(boolean hasUpdate) {
        if (hasUpdate) {
            // 有更新，可以提示用户
        }
    }

    @Override
    public void onError(String error) {
        // 网络异常不影响应用使用
    }

    @Override
    public void onNoUpdateNeeded() {
        // 无需更新
    }
});
```

---

### manualCheckForUpdates - 手动检查更新

```java
public void manualCheckForUpdates(ActionLibraryUpdater.UpdateCallback callback)
```

**说明**: 手动触发检查动作库更新

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| callback | UpdateCallback | 是 | 回调接口 |

**返回值**: 无

**示例**:
```java
player.manualCheckForUpdates(new ActionLibraryUpdater.UpdateCallback() {
    @Override
    public void onSuccess(boolean hasUpdate) {
        // 更新成功
    }

    @Override
    public void onError(String error) {
        // 更新失败
    }
});
```

---

### checkForUpdates - 定期检查更新

```java
public void checkForUpdates(ActionLibraryUpdater.UpdateCallback callback)
```

**说明**: 定期检查动作库更新（基于时间间隔）

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| callback | UpdateCallback | 是 | 回调接口 |

**返回值**: 无

**示例**:
```java
player.checkForUpdates(new ActionLibraryUpdater.UpdateCallback() {
    @Override
    public void onSuccess(boolean hasUpdate) {
        // 检查完成
    }

    @Override
    public void onError(String error) {
        // 检查失败
    }
});
```

---

### getCurrentLibraryVersion - 获取版本

```java
public String getCurrentLibraryVersion()
```

**说明**: 获取当前动作库版本

**返回值**: String - 当前版本号

**示例**:
```java
String version = player.getCurrentLibraryVersion();
Log.d(TAG, "当前版本: " + version);
```

---

### getDownloadedActionFiles - 获取下载文件

```java
public List<File> getDownloadedActionFiles()
```

**说明**: 获取所有本地下载的动作文件

**返回值**: `List<File>` - 动作文件列表

**示例**:
```java
List<File> files = player.getDownloadedActionFiles();
for (File file : files) {
    Log.d(TAG, "文件: " + file.getName() + " 大小: " + file.length());
}
```

---

### getStorageInfo - 获取存储信息

```java
public String getStorageInfo()
```

**说明**: 获取存储位置信息

**返回值**: String - 存储信息字符串

**示例**:
```java
String info = player.getStorageInfo();
Log.d(TAG, info);
```

---

### clearDownloadedActions - 清理下载动作

```java
public void clearDownloadedActions()
```

**说明**: 清理所有下载的动作文件

**返回值**: 无

**示例**:
```java
player.clearDownloadedActions();
```

---

## 监听器接口

### SequenceListener

序列播放监听器接口，用于接收播放过程中的回调事件

#### 接口定义

```java
public interface SequenceListener {
    /**
     * 帧数据回调
     * @param leftArm     左臂10个关节的位置数据（0-4095，-1表示保持）
     * @param rightArm    右臂10个关节的位置数据（0-4095，-1表示保持）
     * @param frameIndex  当前帧索引（从0开始）
     */
    void onFrameData(int[] leftArm, int[] rightArm, int frameIndex);

    /**
     * 播放完成回调
     */
    void onComplete();

    /**
     * 错误回调
     * @param errorMessage 错误信息
     */
    void onError(String errorMessage);

    /**
     * 急停回调
     */
    void onEmergencyStop();
}
```

#### 回调说明

| 回调方法 | 说明 | 调用时机 |
|----------|------|----------|
| onFrameData | 帧数据 | 按设定频率定时调用（40Hz） |
| onComplete | 播放完成 | 所有帧播放完毕后调用一次 |
| onError | 错误 | 加载或播放失败时调用 |
| onEmergencyStop | 急停 | 调用emergencyStop()时立即调用 |

---

## 错误处理

### 常见错误类型

| 错误场景 | 说明 | 处理建议 |
|----------|------|----------|
| actionName为空 | 播放参数无效 | 检查传入的actionName是否为空 |
| listener为null | 播放参数无效 | 设置有效的SequenceListener |
| 频率超出范围 | 频率必须在1-100Hz之间 | 验证frequency参数 |
| 状态不匹配 | 当前状态不支持该操作 | 检查当前状态再操作 |

### 错误处理示例

```java
SequenceListener listener = new SequenceListener() {
    @Override
    public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
        // 正常处理帧数据
        // 左臂10个关节数据：leftArm[0]~leftArm[9]
        // 右臂10个关节数据：rightArm[0]~rightArm[9]
    }

    @Override
    public void onComplete() {
        // 播放完成，可以停止资源或执行后续逻辑
    }

    @Override
    public void onError(String errorMessage) {
        // 处理错误，建议用户重新加载
        Log.e(TAG, "播放错误: " + errorMessage);
        // 可以调用 stop() 清理状态
    }

    @Override
    public void onEmergencyStop() {
        // 急停处理，确保设备安全
        Log.w(TAG, "接收到急停指令");
        // 立即停止所有输出
    }
};

// 播放操作
player.play("动作名称", listener);
```

---

## Native功能

### setRK3399BigCores - 设置大核心

```java
public boolean setRK3399BigCores(boolean useBigCores)
```

**说明**: 设置是否使用RK3399的大核心（A72）

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| useBigCores | boolean | 是 | 是否使用大核心 |

**返回值**: boolean - 设置是否成功

**适用场景**:
- 高频率播放（>60Hz）建议启用
- 低频率播放可以禁用以节省功耗

**示例**:
```java
// 高频率播放启用大核心
if (targetFrequency > 60) {
    player.setRK3399BigCores(true);
}
```

---

### getRK3399Stats - 获取RK3399统计

```java
public String getRK3399Stats()
```

**说明**: 获取RK3399平台的性能统计信息

**返回值**: String - 性能统计字符串

**示例**:
```java
String stats = player.getRK3399Stats();
Log.d(TAG, stats);
```

---

### getGlobalPerformanceStats - 获取全局统计

```java
public static String getGlobalPerformanceStats()
```

**说明**: 获取全局性能统计信息

**返回值**: String - 性能统计字符串

**示例**:
```java
String stats = EvoBotSequencePlayer.getGlobalPerformanceStats();
Log.d(TAG, stats);
```

---

### clearNativeCache - 清空Native缓存

```java
public static void clearNativeCache()
```

**说明**: 清空Native层缓存

**返回值**: 无

**示例**:
```java
EvoBotSequencePlayer.clearNativeCache();
```

---

## 资源管理

### release - 释放资源

```java
public void release()
```

**说明**: 释放所有资源，应在Activity/Fragment销毁时调用

**返回值**: 无

**释放内容**:
- 停止播放
- 清理Handler消息
- 释放Native资源
- 清空序列数据
- 解除监听器引用

**示例**:
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (player != null) {
        player.release();
        player = null;
    }
}
```

---

## 完整使用示例

```java
public class MainActivity extends AppCompatActivity {
    private EvoBotSequencePlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 配置动作库
        ActionLibraryConfig config = ActionLibraryConfig.createDefault()
            .setBaseUrl("http://your-server.com/api/v1")
            .setRobotId("EVOBOT-PRD-00000001");

        // 2. 创建播放器
        player = new EvoBotSequencePlayer(this, config);

        // 3. 设置监听器
        player.play("左臂挥手", new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                // 发送数据到舵机控制器
                sendToServos(leftArm, rightArm);
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
                Log.w(TAG, "接收到急停");
                // 紧急停止舵机
            }
        });

        // 4. 获取动作列表
        List<ActionInfo> actions = player.getAllAvailableActions();
        for (ActionInfo action : actions) {
            Log.d(TAG, action.englishName + " - " + action.name);
        }
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

---

## 附录

### 坐标数据说明

**左臂数据结构**:
```
leftArm[0]  - 肩部前/后
leftArm[1]  - 肩部左/右
leftArm[2]  - 肩部上/下
leftArm[3]  - 肘部左/右
leftArm[4]  - 肘部前/后
leftArm[5]  - 腕部左/右
leftArm[6]  - 腕部前/后
leftArm[7]  - 手腕上/下
leftArm[8]  - 手腕左/右
leftArm[9]  - 手指开/合
```

**右臂数据结构**:
```
rightArm[0] - 肩部前/后
rightArm[1] - 肩部左/右
rightArm[2] - 肩部上/下
rightArm[3] - 肘部左/右
rightArm[4] - 肘部前/后
rightArm[5] - 腕部左/右
rightArm[6] - 腕部前/后
rightArm[7] - 手腕上/下
rightArm[8] - 手腕左/右
rightArm[9] - 手指开/合
```

**值范围**: 0-4095
**-1含义**: 保持当前位置

### 文件格式

**动作文件格式**: `.ebs` (EvoBot Sequence)

**文件命名规则**:
- 英文: `action_category_subcategory_actionname.ebs`
- 中文: `动作_类别_子类别_动作名_时间戳.ebs`

**示例**:
- `arm_movement_wave_left_arm_20260116_142711.ebs`
- `左臂挥手_20260116_142711.ebs`

### 支持的动作名称格式

支持通过以下任一名称播放动作：

1. **英文名称** (标准格式):
   - `arm_movement_wave_left_arm`

2. **中文名称** (自动映射):
   - `左臂挥手`

3. **混合查找** (自动转换):
   - `arm wave` → `左臂挥手`
   - `左挥` → `arm_movement_wave_left_arm`

---

## 版本信息

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-01-27 | 初始版本 |
