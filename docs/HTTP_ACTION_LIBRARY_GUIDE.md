# EvoBot 离线优先动作库使用指南

## 概述

EvoBot序列播放器现已支持**离线优先的HTTP动作库更新机制**。该系统会在合适的时机检查服务器更新，批量下载新动作到本地存储，然后播放时优先从本地加载。这确保了最佳的性能和离线可用性。

## 主要特性

- ✅ **离线优先** - 播放时直接从本地加载，无网络延迟
- ✅ **智能更新检查** - 在合适时机自动检查更新
- ✅ **批量下载** - 发现更新后批量下载到本地
- ✅ **HMAC-SHA256认证** - 安全的API认证机制
- ✅ **补偿和安全检查** - 服务器端处理后下载
- ✅ **版本管理** - 跟踪本地动作库版本
- ✅ **优雅回退** - 下载失败时回退到assets
- ✅ **完全向后兼容** - 现有代码无需修改

## 工作原理

### 更新检查时机
1. **应用启动时** - 延迟检查，不影响启动速度
2. **定期检查** - 默认24小时间隔
3. **网络恢复时** - 从断网恢复后检查
4. **用户手动触发** - 用户点击"检查更新"

### 更新流程
1. **检查更新** - 调用 `/api/v1/updates/check`
2. **发现更新** - 服务器返回可用更新列表
3. **批量下载** - 调用 `/api/v1/updates/batch-download`
4. **本地存储** - 保存到应用内部存储
5. **版本更新** - 更新本地版本信息

### 播放流程
1. **优先本地** - 首先查找下载的动作文件
2. **回退assets** - 本地未找到时使用assets
3. **离线播放** - 完全不依赖网络连接

## 快速开始

### 1. 基本使用

```java
// 创建动作库配置
ActionLibraryConfig config = new ActionLibraryConfig(
    "http://localhost:9189/api/v1",  // 服务器地址
    "EVOBOT-PRD-00000001",           // 机器人ID
    "ak_7x9m2n8p4q1r5s6t"            // API Key
);

// 创建支持离线优先更新的播放器
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config);

// 播放动作（优先从下载的动作库加载，回退到assets）
player.play("左臂挥手", new SequenceListener() {
    @Override
    public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
        // 处理每帧数据
    }
    
    @Override
    public void onComplete() {
        Log.d(TAG, "动作播放完成");
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "播放错误: " + error);
    }
    
    @Override
    public void onEmergencyStop() {
        Log.w(TAG, "急停执行");
    }
});
```

### 2. 仅本地模式（向后兼容）

```java
// 不传入配置，仅使用本地assets
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context);
player.play("动作名称", listener); // 从assets加载
```

## 高级功能

### 应用启动时检查更新

```java
// 在Application.onCreate()或MainActivity.onCreate()中
player.checkForUpdates(new ActionLibraryUpdater.UpdateCallback() {
    @Override
    public void onNoUpdateNeeded() {
        Log.d(TAG, "距离上次检查时间不足，跳过更新");
    }
    
    @Override
    public void onNoUpdatesAvailable() {
        Log.d(TAG, "没有可用更新");
    }
    
    @Override
    public void onUpdatesFound(int updateCount, long totalSize) {
        Log.d(TAG, String.format("发现 %d 个更新，总大小: %d bytes", updateCount, totalSize));
    }
    
    @Override
    public void onDownloadStarted(int fileCount) {
        Log.d(TAG, "开始下载 " + fileCount + " 个动作文件");
    }
    
    @Override
    public void onDownloadCompleted(int savedCount, String newVersion) {
        Log.d(TAG, String.format("更新完成: 保存了 %d 个动作，新版本: %s", savedCount, newVersion));
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "更新检查失败: " + error);
    }
});
```

### 用户手动检查更新

```java
// 用户点击"检查更新"按钮时
refreshButton.setOnClickListener(v -> {
    player.forceCheckForUpdates(new ActionLibraryUpdater.UpdateCallback() {
        @Override
        public void onNoUpdatesAvailable() {
            showToast("已是最新版本");
        }
        
        @Override
        public void onUpdatesFound(int updateCount, long totalSize) {
            showUpdateDialog(updateCount, totalSize);
        }
        
        @Override
        public void onDownloadCompleted(int savedCount, String newVersion) {
            showToast("更新完成，新增 " + savedCount + " 个动作");
        }
        
        @Override
        public void onError(String error) {
            showToast("更新失败: " + error);
        }
    });
});
```

### 查看本地动作库状态

```java
// 获取当前版本
String currentVersion = player.getCurrentLibraryVersion();
Log.d(TAG, "当前动作库版本: " + currentVersion);

// 获取所有下载的动作文件
List<File> downloadedFiles = player.getDownloadedActionFiles();
Log.d(TAG, "已下载的动作文件数量: " + downloadedFiles.size());

for (File file : downloadedFiles) {
    Log.d(TAG, String.format("- %s (大小: %d bytes)", file.getName(), file.length()));
}
```

### 定期更新检查

```java
// 使用定时器每24小时检查一次更新
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    player.checkForUpdates(callback);
}, 0, 24, TimeUnit.HOURS);
```

### 清理本地动作库

```java
// 清理下载的动作文件（保留assets）
player.clearDownloadedActions();
Log.d(TAG, "本地动作库已清理");
```

## 配置选项

### ActionLibraryConfig参数

```java
ActionLibraryConfig config = new ActionLibraryConfig(
    baseUrl,              // 服务器基础URL
    robotId,              // 机器人ID
    apiKey,               // API密钥
    enableCache,          // 是否启用缓存 (默认true)
    enableCompensation,   // 是否启用补偿 (默认true)
    enableSafetyCheck     // 是否启用安全检查 (默认true)
);
```

### 默认配置

```java
// 使用默认配置
ActionLibraryConfig config = ActionLibraryConfig.createDefault();
```

## 服务器配置

### 基本信息
- **基础URL**: `http://localhost:9189/api/v1`
- **认证方式**: HMAC-SHA256 + API Key
- **JWT密钥**: `a8f5f167f44f4964e6c998dee827110c`
- **API Key**: `ak_7x9m2n8p4q1r5s6t`

### 权限配置
- `download`: 下载动作序列
- `sync`: 同步和更新检查
- `upload`: 上传动作序列（标准机器人）
- `standard`: 标准机器人权限
- `manage`: 管理分类
- `admin`: 管理员权限

## 工作流程

### 动作加载流程
1. **检查缓存** - 优先从本地缓存加载
2. **网络下载** - 缓存未命中时从服务器下载
3. **应用补偿** - 根据设备零位调整动作参数
4. **安全检查** - 验证关节限位和动作安全性
5. **本地缓存** - 下载成功后缓存到本地
6. **回退机制** - 网络失败时回退到本地assets

### 认证流程
1. 生成时间戳和随机nonce
2. 构建签名字符串：`robotId + timestamp + nonce + method + path`
3. 使用API Key进行HMAC-SHA256签名
4. 设置认证请求头

## 错误处理

### 常见错误码
- `4001`: 认证失败（API Key无效或签名错误）
- `4003`: 权限不足
- `4004`: 资源不存在
- `4006`: 安全检查失败
- `5001`: 服务器内部错误

### 重试机制
- 网络请求失败时自动重试（最多3次）
- 使用指数退避算法（1s, 2s, 4s）
- 认证错误不重试

## 性能优化

### 缓存策略
- **LRU淘汰** - 缓存满时删除最旧文件
- **大小限制** - 默认50MB缓存空间
- **完整性验证** - MD5哈希验证文件完整性

### 网络优化
- **连接复用** - 使用HttpURLConnection
- **超时设置** - 连接10s，读取30s
- **并发控制** - 使用线程池管理网络请求

## 测试和调试

### 运行测试
```java
ActionLibraryTest test = new ActionLibraryTest(context);
test.runAllTests(); // 运行所有测试
```

### 运行示例
```java
ActionLibraryExample example = new ActionLibraryExample(context);
example.runAllExamples(); // 运行所有示例
```

### 日志标签
- `ActionLibraryClient`: HTTP客户端日志
- `ActionCacheManager`: 缓存管理日志
- `ActionLibraryManager`: 动作库管理日志
- `EvoBotSequencePlayer`: 播放器日志

## 最佳实践

### 1. 配置管理
```java
// 在Application中初始化全局配置
public class MyApplication extends Application {
    private static ActionLibraryConfig globalConfig;
    
    @Override
    public void onCreate() {
        super.onCreate();
        globalConfig = new ActionLibraryConfig(
            BuildConfig.ACTION_LIBRARY_URL,
            getDeviceRobotId(),
            BuildConfig.API_KEY
        );
    }
    
    public static ActionLibraryConfig getActionLibraryConfig() {
        return globalConfig;
    }
}
```

### 2. 错误处理
```java
player.play("动作名称", new SequenceListener() {
    @Override
    public void onError(String error) {
        if (error.contains("网络")) {
            // 网络错误，可能需要检查网络连接
            showNetworkErrorDialog();
        } else if (error.contains("认证")) {
            // 认证错误，可能需要更新API Key
            refreshApiKey();
        } else {
            // 其他错误
            showGenericErrorDialog(error);
        }
    }
});
```

### 3. 生命周期管理
```java
public class MainActivity extends AppCompatActivity {
    private EvoBotSequencePlayer player;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        player = new EvoBotSequencePlayer(this, MyApplication.getActionLibraryConfig());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release(); // 释放资源
        }
    }
}
```

### 4. 预加载策略
```java
// 在应用启动时预加载常用动作
String[] essentialActions = {"待机", "问候", "告别"};
player.preloadActions(essentialActions, (success, total) -> {
    Log.d(TAG, String.format("预加载完成: %d/%d", success, total));
});
```

## 故障排除

### 网络连接问题
1. 检查服务器是否运行在 `http://localhost:9189`
2. 确认网络权限已添加到AndroidManifest.xml
3. 检查防火墙设置

### 认证失败
1. 验证API Key是否正确
2. 检查机器人ID格式
3. 确认服务器时间同步

### 缓存问题
1. 检查应用缓存目录权限
2. 清空缓存重新下载
3. 检查磁盘空间

### 播放问题
1. 检查动作文件格式
2. 验证关节限位设置
3. 确认补偿参数正确

## 更新日志

### v1.0.0 (2026-01-26)
- ✅ 初始版本发布
- ✅ HTTP动作库基础功能
- ✅ HMAC-SHA256认证
- ✅ 智能缓存机制
- ✅ 补偿和安全检查
- ✅ 向后兼容支持

## 联系支持

如有问题或建议，请联系：
- 邮箱: support@evobot.com
- 文档: [API Documentation](API_DOCUMENTATION.md)
- 示例: [ActionLibraryExample.java](app/src/main/java/com/evobot/sequence/ActionLibraryExample.java)