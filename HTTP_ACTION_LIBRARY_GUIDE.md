# EvoBot HTTP动作库使用指南

## 概述

EvoBot序列播放器现已支持HTTP动作库功能，可以从远程服务器动态下载、缓存和播放动作序列。该功能完全向后兼容，现有的本地assets加载方式仍然可用。

## 主要特性

- ✅ **HTTP动作库支持** - 从远程服务器下载动作序列
- ✅ **HMAC-SHA256认证** - 安全的API认证机制
- ✅ **智能缓存** - 自动缓存下载的动作，支持离线使用
- ✅ **补偿计算** - 根据设备零位自动调整动作参数
- ✅ **安全检查** - 服务器端关节限位和安全验证
- ✅ **更新检查** - 自动检查动作库更新
- ✅ **混合模式** - HTTP优先，本地assets回退
- ✅ **向后兼容** - 现有代码无需修改

## 快速开始

### 1. 基本使用

```java
// 创建动作库配置
ActionLibraryConfig config = new ActionLibraryConfig(
    "http://localhost:9189/api/v1",  // 服务器地址
    "EVOBOT-PRD-00000001",           // 机器人ID
    "ak_7x9m2n8p4q1r5s6t"            // API Key
);

// 创建支持HTTP动作库的播放器
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config);

// 播放动作（自动从网络下载和缓存）
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

### 更新检查

```java
player.checkUpdates("1.0.0", new ActionLibraryManager.UpdateCheckCallback() {
    @Override
    public void onUpdateCheckComplete(boolean hasUpdates, int updateCount, String details) {
        if (hasUpdates) {
            Log.d(TAG, "发现 " + updateCount + " 个更新");
        }
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "更新检查失败: " + error);
    }
});
```

### 获取动作列表

```java
player.getActionList("arm_movement", new ActionLibraryManager.ActionListCallback() {
    @Override
    public void onSuccess(String jsonResponse) {
        // 解析JSON获取可用动作列表
        Log.d(TAG, "动作列表: " + jsonResponse);
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "获取列表失败: " + error);
    }
});
```

### 预加载常用动作

```java
String[] commonActions = {"左臂挥手", "右臂挥手", "双臂抱胸"};
player.preloadActions(commonActions, new ActionLibraryManager.PreloadCallback() {
    @Override
    public void onComplete(int successCount, int totalCount) {
        Log.d(TAG, String.format("预加载完成: %d/%d", successCount, totalCount));
    }
});
```

### 缓存管理

```java
// 获取缓存统计
ActionCacheManager.CacheStats stats = player.getCacheStats();
Log.d(TAG, "缓存统计: " + stats.toString());

// 清空缓存
if (stats.getUsagePercentage() > 80) {
    player.clearActionLibraryCache();
}
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