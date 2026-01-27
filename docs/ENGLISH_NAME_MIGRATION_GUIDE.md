# 英文名称迁移指南

## 概述

为了解决机器人在使用动作序列时可能遇到的中文编码问题，系统现在支持英文名称（`english_name`）作为动作的标准标识符。系统采用**动态映射**的方式维护中英文名称的对应关系，映射关系通过解析下载的动作文件和API响应自动建立。

## 主要变化

### 1. API 接口变化

- **新增字段**：所有动作序列现在都包含 `english_name` 字段
- **文件命名**：下载的动作文件使用英文名称命名（如：`arm_movement_left_arm_wave.ebs`）
- **动态映射**：中英文名称映射关系通过解析文件内容和API响应动态建立

### 2. 动态映射机制

#### 映射建立时机
1. **文件下载时**：下载动作文件后，解析文件内容获取中文名称，与文件名（英文）建立映射
2. **API调用时**：获取动作列表时，从API响应中提取中英文名称对建立映射
3. **初始化时**：应用启动时扫描本地已有文件，解析并建立映射关系

#### 映射存储方式
- **内存存储**：映射关系存储在内存中，应用重启后需要重新建立
- **线程安全**：使用 `ConcurrentHashMap` 确保多线程环境下的安全性
- **动态更新**：随着新文件下载和API调用，映射关系持续更新

### 3. Java 代码更新

#### 核心工具类
- `ActionNameUtils.java`：动态维护中英文名称映射关系
- `ActionInfo.java`：存储从API获取的动作信息

#### 更新的类
- `EvoBotSequencePlayer.java`：初始化时建立映射关系
- `ActionLibraryUpdater.java`：文件下载后建立映射
- `ActionLibraryManager.java`：API调用时建立映射
- `ActionCacheManager.java`：使用标准化文件名

## 迁移步骤

### 1. 更新代码调用

**推荐方式（使用英文名称）：**
```java
// 旧方式
player.play("左臂挥手", listener);

// 新方式（推荐）
player.play("arm_movement_left_arm_wave", listener);
```

**兼容方式（仍支持中文名称）：**
```java
// 系统会自动转换中文名称为英文名称
player.play("左臂挥手", listener);  // 仍然有效
```

### 2. 常用动作名称映射

**重要说明**：映射关系现在是动态建立的，不再预定义。以下是一些可能的映射示例：

| 中文名称 | 英文名称 | 说明 |
|---------|---------|------|
| 左臂挥手 | `arm_movement_left_arm_wave` | 左臂挥手动作 |
| 右臂挥手 | `arm_movement_right_arm_wave` | 右臂挥手动作 |
| 左臂拥抱 | `arm_movement_left_arm_hug` | 左臂拥抱动作 |
| 右臂拥抱 | `arm_movement_right_arm_hug` | 右臂拥抱动作 |
| 双臂挥手 | `arm_movement_both_arms_wave` | 双臂挥手动作 |
| 点头确认 | `head_nod_confirm` | 点头确认动作 |
| 摇头拒绝 | `head_shake_refuse` | 摇头拒绝动作 |
| 微笑打招呼 | `smile_greeting` | 微笑打招呼动作 |

**注意**：实际的映射关系取决于服务器上的动作定义和文件内容。

### 3. 文件管理更新

**文件命名规则：**
- 下载的文件使用英文名称：`arm_movement_left_arm_wave.ebs`
- 缓存文件包含时间戳：`1642857600_arm_movement_left_arm_wave.ebs`

**文件查找逻辑：**
```java
// 系统会自动匹配中英文名称
File actionFile = actionLibraryUpdater.getLocalActionFile("左臂挥手");
// 或
File actionFile = actionLibraryUpdater.getLocalActionFile("arm_movement_left_arm_wave");
```

## 工具类使用

### ActionNameUtils 主要方法

```java
// 动态添加映射关系（通常由系统自动调用）
ActionNameUtils.addMapping("左臂挥手", "arm_movement_left_arm_wave");

// 从文件解析建立映射（系统自动调用）
ActionNameUtils.addMappingFromFile("arm_movement_left_arm_wave.ebs", sequenceData);

// 判断名称类型
boolean isEnglish = ActionNameUtils.isEnglishName("arm_movement_left_arm_wave");
boolean isChinese = ActionNameUtils.isChineseName("左臂挥手");

// 名称转换（基于动态映射）
String englishName = ActionNameUtils.chineseToEnglish("左臂挥手");
String chineseName = ActionNameUtils.englishToChinese("arm_movement_left_arm_wave");

// 智能名称匹配
boolean isMatch = ActionNameUtils.isNameMatch("左臂挥手", "arm_movement_left_arm_wave");

// 智能文件查找
File actionFile = ActionNameUtils.findMatchingFile("左臂挥手", availableFiles);

// 映射管理
int count = ActionNameUtils.getMappingCount();  // 获取当前映射数量
ActionNameUtils.clearMappings();  // 清除所有映射（谨慎使用）
```

### 映射建立流程

```java
// 1. 系统初始化时自动扫描本地文件
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 创建播放器时会自动初始化映射
        EvoBotSequencePlayer player = new EvoBotSequencePlayer(
            this, actionLibraryConfig, storageLocation);
        // 映射会在后台线程中建立
    }
}

// 2. 下载文件时自动建立映射
ActionLibraryUpdater updater = new ActionLibraryUpdater(context, config, location);
updater.checkForUpdatesAsync(new UpdateCallback() {
    @Override
    public void onDownloadCompleted(int savedCount, String newVersion) {
        // 下载完成后，映射已自动建立
        Log.d(TAG, "映射数量: " + ActionNameUtils.getMappingCount());
    }
});

// 3. API调用时自动建立映射
ActionLibraryManager manager = new ActionLibraryManager(context, config);
manager.loadSequenceAsync("左臂挥手", new LoadSequenceCallback() {
    @Override
    public void onSuccess(SequenceData data) {
        // API调用过程中已建立映射关系
    }
});
```

## 兼容性说明

### 向后兼容
- **现有代码**：使用中文名称的现有代码仍然可以正常工作
- **自动转换**：系统会自动将中文名称转换为对应的英文名称
- **文件查找**：支持中英文名称混合查找

### 向前兼容
- **新功能**：新功能优先使用英文名称
- **API 调用**：建议新的 API 调用使用英文名称
- **文件存储**：新下载的文件使用英文名称

## 最佳实践

### 1. 新项目开发
```java
public class RobotController {
    private EvoBotSequencePlayer player;
    
    public void performGreeting() {
        // 推荐：使用英文名称
        player.play("arm_movement_left_arm_wave", new SequenceListener() {
            @Override
            public void onSequenceComplete() {
                // 继续下一个动作
                player.play("smile_greeting", listener);
            }
            // ... 其他回调方法
        });
    }
}
```

### 2. 批量动作管理
```java
public class ActionSequenceManager {
    private static final String[] GREETING_SEQUENCE = {
        "arm_movement_left_arm_wave",
        "arm_movement_right_arm_wave", 
        "head_nod_confirm",
        "smile_greeting"
    };
    
    public void playGreetingSequence() {
        playActionsSequentially(GREETING_SEQUENCE, 0);
    }
}
```

### 3. 配置文件管理
```json
{
  "common_actions": [
    {
      "display_name": "左臂挥手",
      "action_name": "arm_movement_left_arm_wave",
      "category": "greeting"
    },
    {
      "display_name": "右臂挥手", 
      "action_name": "arm_movement_right_arm_wave",
      "category": "greeting"
    }
  ]
}
```

## 故障排除

### 常见问题

1. **文件找不到**
   - 检查是否使用了正确的英文名称
   - 确认文件是否已下载到本地
   - 使用 `ActionNameUtils.isNameMatch()` 验证名称匹配

2. **名称转换失败**
   - 检查映射表是否包含该动作
   - 可以手动添加新的映射关系
   - 使用标准化名称作为备选方案

3. **缓存问题**
   - 清除旧的缓存文件
   - 重新下载动作文件
   - 检查缓存键是否使用了标准化名称

### 调试方法

```java
// 启用详细日志
Log.d("ActionDebug", "输入名称: " + actionName);
Log.d("ActionDebug", "是否英文: " + ActionNameUtils.isEnglishName(actionName));
Log.d("ActionDebug", "标准名称: " + ActionNameUtils.getStandardName(actionName));

// 检查文件存在性
File actionFile = actionLibraryUpdater.getLocalActionFile(actionName);
Log.d("ActionDebug", "文件路径: " + (actionFile != null ? actionFile.getAbsolutePath() : "null"));
```

## 总结

英文名称的引入提高了系统的跨平台兼容性和稳定性，同时保持了向后兼容性。建议在新项目中优先使用英文名称，现有项目可以逐步迁移。

通过 `ActionNameUtils` 工具类，开发者可以轻松处理中英文名称的转换和匹配，确保系统的稳定运行。