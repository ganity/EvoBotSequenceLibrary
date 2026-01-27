# EvoBot 动作库存储位置指南

## 存储位置选项

EvoBot动作库支持三种存储位置，每种都有不同的特性和适用场景：

### 1. 内部存储 (INTERNAL_FILES) - 默认推荐

```java
// 使用默认内部存储
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config);

// 或显式指定
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config, 
    ActionLibraryUpdater.StorageLocation.INTERNAL_FILES);
```

**存储路径**: `/data/data/com.evobot.sequence/files/downloaded_actions/`

**特性**:
- ✅ **程序重启保留** - 重启应用和系统后数据仍存在
- ✅ **应用升级保留** - 应用更新时数据不丢失
- ✅ **无需权限** - 不需要任何存储权限
- ✅ **安全私有** - 其他应用无法访问
- ❌ **应用卸载删除** - 卸载应用时数据会被清理
- ❌ **用户清理数据删除** - 用户在设置中清理应用数据时会删除

**适用场景**: 
- 大多数应用的默认选择
- 注重数据安全和隐私
- 不需要用户直接访问动作文件

### 2. 外部应用专用目录 (EXTERNAL_FILES)

```java
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config, 
    ActionLibraryUpdater.StorageLocation.EXTERNAL_FILES);
```

**存储路径**: `/Android/data/com.evobot.sequence/files/downloaded_actions/`

**特性**:
- ✅ **程序重启保留** - 重启应用和系统后数据仍存在
- ✅ **应用升级保留** - 应用更新时数据不丢失
- ✅ **无需权限** - Android 4.4+ 不需要存储权限
- ✅ **用户可访问** - 用户可通过文件管理器访问
- ❌ **应用卸载删除** - 卸载应用时数据会被清理
- ❌ **外部存储依赖** - 需要SD卡或外部存储可用

**适用场景**:
- 需要用户能够访问动作文件
- 调试和开发阶段
- 需要手动管理动作文件

### 3. 外部公共目录 (EXTERNAL_PUBLIC)

```java
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config, 
    ActionLibraryUpdater.StorageLocation.EXTERNAL_PUBLIC);
```

**存储路径**: `/EvoBot/Actions/`

**特性**:
- ✅ **程序重启保留** - 重启应用和系统后数据仍存在
- ✅ **应用升级保留** - 应用更新时数据不丢失
- ✅ **应用卸载保留** - 卸载应用后数据仍然存在
- ✅ **用户完全访问** - 用户可自由访问和管理
- ❌ **需要存储权限** - 需要WRITE_EXTERNAL_STORAGE权限
- ❌ **安全性较低** - 其他应用可能访问

**适用场景**:
- 需要在应用卸载后保留动作文件
- 多个应用共享动作库
- 企业部署需要集中管理动作文件

## 权限要求

### 内部存储 - 无需权限
```xml
<!-- 无需添加任何权限 -->
```

### 外部应用专用目录 - 无需权限 (Android 4.4+)
```xml
<!-- Android 4.4+ 无需权限 -->
<!-- Android 4.3及以下需要权限 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="18" />
```

### 外部公共目录 - 需要权限
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## 使用示例

### 基本使用（内部存储）
```java
ActionLibraryConfig config = new ActionLibraryConfig(
    "http://localhost:9189/api/v1",
    "EVOBOT-PRD-00000001", 
    "ak_7x9m2n8p4q1r5s6t"
);

// 默认使用内部存储
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config);
```

### 指定存储位置
```java
// 使用外部应用专用目录
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config, 
    ActionLibraryUpdater.StorageLocation.EXTERNAL_FILES);

// 使用外部公共目录
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config, 
    ActionLibraryUpdater.StorageLocation.EXTERNAL_PUBLIC);
```

### 查看存储信息
```java
String storageInfo = player.getStorageInfo();
Log.d(TAG, "存储信息:\n" + storageInfo);

// 输出示例:
// 存储位置: INTERNAL_FILES
// 存储路径: /data/data/com.evobot.sequence/files/downloaded_actions
// 目录存在: true
// 文件数量: 5
// 总大小: 2048576 bytes
```

## 存储位置对比

| 特性 | 内部存储 | 外部应用专用 | 外部公共 |
|------|----------|-------------|----------|
| 程序重启保留 | ✅ | ✅ | ✅ |
| 应用升级保留 | ✅ | ✅ | ✅ |
| 应用卸载保留 | ❌ | ❌ | ✅ |
| 需要权限 | ❌ | ❌ (4.4+) | ✅ |
| 用户可访问 | ❌ | ✅ | ✅ |
| 安全性 | 高 | 中 | 低 |
| 存储空间 | 内部 | 外部 | 外部 |

## 推荐选择

### 生产环境推荐
```java
// 推荐：内部存储（默认）
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config);
```

**理由**:
- 无需权限，兼容性最好
- 数据安全，其他应用无法访问
- 程序重启和升级都保留数据
- 应用卸载时自动清理，不留垃圾

### 开发调试推荐
```java
// 开发时：外部应用专用目录
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config, 
    ActionLibraryUpdater.StorageLocation.EXTERNAL_FILES);
```

**理由**:
- 可以通过文件管理器查看下载的动作文件
- 便于调试和验证文件内容
- 无需额外权限

### 企业部署推荐
```java
// 企业环境：外部公共目录
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config, 
    ActionLibraryUpdater.StorageLocation.EXTERNAL_PUBLIC);
```

**理由**:
- 应用卸载后动作文件仍保留
- 可以预装动作文件到设备
- 便于集中管理和维护

## 迁移和备份

### 检查现有数据
```java
String storageInfo = player.getStorageInfo();
List<File> actionFiles = player.getDownloadedActionFiles();

for (File file : actionFiles) {
    Log.d(TAG, "动作文件: " + file.getAbsolutePath());
}
```

### 清理旧数据
```java
// 清理当前存储位置的数据
player.clearDownloadedActions();
```

### 手动迁移（如需要）
```java
// 1. 获取旧位置的文件
List<File> oldFiles = oldPlayer.getDownloadedActionFiles();

// 2. 创建新存储位置的播放器
EvoBotSequencePlayer newPlayer = new EvoBotSequencePlayer(context, config, newLocation);

// 3. 重新下载（推荐）或手动复制文件
newPlayer.forceCheckForUpdates(callback);
```

## 故障排除

### 存储空间不足
```java
String storageInfo = player.getStorageInfo();
// 检查总大小，如果过大可以清理
if (totalSize > MAX_STORAGE_SIZE) {
    player.clearDownloadedActions();
}
```

### 权限被拒绝
```java
// 检查是否有存储权限（仅外部公共目录需要）
if (ContextCompat.checkSelfPermission(context, 
    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
    // 请求权限或切换到内部存储
}
```

### 外部存储不可用
```java
// 系统会自动回退到内部存储，检查日志
// "外部存储不可用，回退到内部存储"
```

## 总结

- **默认选择**: 内部存储 - 安全、无权限、兼容性好
- **开发调试**: 外部应用专用 - 可访问、无权限
- **企业部署**: 外部公共 - 持久化、可管理
- **数据持久性**: 程序重启和应用升级都不会丢失数据
- **清理时机**: 只有应用卸载或用户主动清理才会删除