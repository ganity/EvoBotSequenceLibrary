# EvoBot 动作库应用集成指南

## 概述

本指南展示如何在Android应用中集成EvoBot动作库AAR，实现手动触发检查和首次启动自动检查功能，并确保网络异常不影响应用启动和使用。

## 核心接口

### 1. 首次启动检查
```java
// 首次启动时自动检查更新（异步，不阻塞启动）
player.checkForUpdatesOnFirstLaunch(callback);
```

### 2. 手动触发检查
```java
// 用户主动触发检查更新
player.manualCheckForUpdates(callback);
```

### 3. 定期检查
```java
// 基于时间间隔的定期检查（24小时）
player.checkForUpdates(callback);
```

## 集成步骤

### 步骤1：Application类集成

```java
public class MyApplication extends Application {
    
    private EvoBotSequencePlayer globalPlayer;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 1. 创建全局播放器实例
        ActionLibraryConfig config = new ActionLibraryConfig(
            "http://your-server:9189/api/v1",
            "YOUR-ROBOT-ID",
            "YOUR-API-KEY"
        );
        
        globalPlayer = new EvoBotSequencePlayer(this, config);
        
        // 2. 延迟进行首次启动检查（不阻塞应用启动）
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            performFirstLaunchCheck();
        }, 3000); // 延迟3秒，确保应用完全启动
    }
    
    private void performFirstLaunchCheck() {
        globalPlayer.checkForUpdatesOnFirstLaunch(new ActionLibraryUpdater.UpdateCallback() {
            @Override
            public void onNoUpdateNeeded() {
                Log.d(TAG, "非首次启动，跳过检查");
            }
            
            @Override
            public void onNoUpdatesAvailable() {
                Log.d(TAG, "没有可用更新");
            }
            
            @Override
            public void onUpdatesFound(int updateCount, long totalSize) {
                Log.d(TAG, "发现更新，开始后台下载");
                // 可以显示不干扰的通知
                showUpdateNotification("正在下载新动作...");
            }
            
            @Override
            public void onDownloadCompleted(int savedCount, String newVersion) {
                Log.d(TAG, "下载完成，新增 " + savedCount + " 个动作");
                showUpdateNotification("动作库更新完成");
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "首次启动检查失败（不影响应用使用）: " + error);
                // 静默处理错误，不干扰用户
            }
            
            @Override
            public void onDownloadStarted(int fileCount) {
                Log.d(TAG, "开始下载 " + fileCount + " 个文件");
            }
        });
    }
    
    public EvoBotSequencePlayer getGlobalPlayer() {
        return globalPlayer;
    }
}
```

### 步骤2：MainActivity集成

```java
public class MainActivity extends AppCompatActivity {
    
    private EvoBotSequencePlayer player;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 获取全局播放器实例
        MyApplication app = (MyApplication) getApplication();
        player = app.getGlobalPlayer();
        
        // 设置手动更新按钮
        findViewById(R.id.btn_check_update).setOnClickListener(v -> {
            onUpdateButtonClick();
        });
    }
    
    private void onUpdateButtonClick() {
        // 显示加载对话框
        ProgressDialog dialog = ProgressDialog.show(this, "检查更新", "正在检查更新...", true);
        
        player.manualCheckForUpdates(new ActionLibraryUpdater.UpdateCallback() {
            @Override
            public void onNoUpdateNeeded() {
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "当前已是最新版本", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onNoUpdatesAvailable() {
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "当前已是最新版本", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onUpdatesFound(int updateCount, long totalSize) {
                dialog.setMessage(String.format("发现 %d 个更新，正在下载...", updateCount));
            }
            
            @Override
            public void onDownloadStarted(int fileCount) {
                dialog.setMessage("正在下载 " + fileCount + " 个动作文件...");
            }
            
            @Override
            public void onDownloadCompleted(int savedCount, String newVersion) {
                dialog.dismiss();
                Toast.makeText(MainActivity.this, 
                    String.format("更新完成！新增 %d 个动作", savedCount), 
                    Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onError(String error) {
                dialog.dismiss();
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("更新失败")
                    .setMessage(error)
                    .setPositiveButton("确定", null)
                    .show();
            }
        });
    }
}
```

### 步骤3：设置页面集成

```java
public class SettingsActivity extends AppCompatActivity {
    
    private EvoBotSequencePlayer player;
    private TextView tvVersion, tvStorageInfo, tvFileCount;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // 获取播放器实例
        MyApplication app = (MyApplication) getApplication();
        player = app.getGlobalPlayer();
        
        // 初始化UI
        initViews();
        displayCurrentStatus();
    }
    
    private void initViews() {
        tvVersion = findViewById(R.id.tv_version);
        tvStorageInfo = findViewById(R.id.tv_storage_info);
        tvFileCount = findViewById(R.id.tv_file_count);
        
        // 清理按钮
        findViewById(R.id.btn_clear_library).setOnClickListener(v -> {
            onClearActionLibraryClick();
        });
        
        // 手动检查按钮
        findViewById(R.id.btn_manual_check).setOnClickListener(v -> {
            onManualCheckClick();
        });
    }
    
    private void displayCurrentStatus() {
        // 显示版本信息
        String currentVersion = player.getCurrentLibraryVersion();
        tvVersion.setText("当前版本: " + currentVersion);
        
        // 显示存储信息
        String storageInfo = player.getStorageInfo();
        tvStorageInfo.setText(storageInfo);
        
        // 显示文件数量
        List<File> files = player.getDownloadedActionFiles();
        tvFileCount.setText("已下载动作: " + files.size() + " 个");
    }
    
    private void onClearActionLibraryClick() {
        new AlertDialog.Builder(this)
            .setTitle("清理动作库")
            .setMessage("确定要清理已下载的动作库吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                player.clearDownloadedActions();
                Toast.makeText(this, "动作库已清理", Toast.LENGTH_SHORT).show();
                displayCurrentStatus(); // 刷新状态显示
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void onManualCheckClick() {
        // 重用MainActivity的检查逻辑
        // ... 同MainActivity的onUpdateButtonClick()
    }
}
```

## 异常处理策略

### 1. 网络异常兜底

```java
// 所有网络操作都有完善的异常处理
player.checkForUpdatesOnFirstLaunch(new ActionLibraryUpdater.UpdateCallback() {
    @Override
    public void onError(String error) {
        // 网络异常不影响应用启动和使用
        Log.w(TAG, "网络检查失败，应用正常运行: " + error);
        
        // 可以记录到分析系统，但不显示给用户
        // Analytics.logEvent("action_library_check_failed", error);
    }
});
```

### 2. 超时控制

```java
// 首次启动检查：15秒超时，1次重试
// 手动检查：30秒超时，2次重试
// 定期检查：30秒超时，2次重试
```

### 3. 错误分类处理

```java
@Override
public void onError(String error) {
    if (error.contains("网络请求超时")) {
        // 超时错误 - 静默处理或显示简单提示
        Log.w(TAG, "网络超时，稍后重试");
    } else if (error.contains("网络连接失败")) {
        // 连接错误 - 检查网络状态
        Log.w(TAG, "网络连接失败，请检查网络");
    } else if (error.contains("无法解析服务器地址")) {
        // DNS错误 - 可能是服务器配置问题
        Log.e(TAG, "服务器地址错误");
    } else {
        // 其他错误
        Log.e(TAG, "未知错误: " + error);
    }
}
```

## 最佳实践

### 1. 应用启动优化

```java
// ✅ 正确做法：延迟检查，不阻塞启动
new Handler(Looper.getMainLooper()).postDelayed(() -> {
    player.checkForUpdatesOnFirstLaunch(callback);
}, 3000);

// ❌ 错误做法：在onCreate中同步检查
// player.manualCheckForUpdates(callback); // 会阻塞启动
```

### 2. 用户体验优化

```java
// ✅ 首次启动：静默检查，不干扰用户
player.checkForUpdatesOnFirstLaunch(new ActionLibraryUpdater.UpdateCallback() {
    @Override
    public void onUpdatesFound(int updateCount, long totalSize) {
        // 显示不干扰的通知，如状态栏通知
        showStatusBarNotification("正在更新动作库...");
    }
    
    @Override
    public void onError(String error) {
        // 静默处理，不显示错误给用户
        Log.w(TAG, "后台更新失败: " + error);
    }
});

// ✅ 手动检查：显示进度，提供反馈
player.manualCheckForUpdates(new ActionLibraryUpdater.UpdateCallback() {
    @Override
    public void onError(String error) {
        // 显示错误对话框，让用户知道
        showErrorDialog("更新失败", error);
    }
});
```

### 3. 资源管理

```java
public class MyApplication extends Application {
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        // 释放播放器资源
        if (globalPlayer != null) {
            globalPlayer.release();
        }
    }
}
```

### 4. 权限处理

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 如果使用外部公共存储 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## 配置选项

### 1. 服务器配置

```java
ActionLibraryConfig config = new ActionLibraryConfig(
    "http://your-server:9189/api/v1",  // 服务器地址
    "YOUR-ROBOT-ID",                   // 机器人ID
    "YOUR-API-KEY",                    // API密钥
    true,                              // 启用缓存
    true,                              // 启用补偿
    true                               // 启用安全检查
);
```

### 2. 存储位置配置

```java
// 默认内部存储（推荐）
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config);

// 外部应用专用目录（调试用）
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config, 
    ActionLibraryUpdater.StorageLocation.EXTERNAL_FILES);

// 外部公共目录（企业部署）
EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config, 
    ActionLibraryUpdater.StorageLocation.EXTERNAL_PUBLIC);
```

### 3. 超时配置

```java
// 在ActionLibraryUpdater中可以修改超时设置
private static final int FIRST_LAUNCH_TIMEOUT_MS = 15000;  // 首次启动15秒
private static final int MANUAL_CHECK_TIMEOUT_MS = 30000;  // 手动检查30秒
```

## 调试和监控

### 1. 日志标签

```java
// 使用这些标签过滤日志
adb logcat | grep -E "(ActionLibraryUpdater|ActionLibraryClient|EvoBotSequencePlayer)"
```

### 2. 状态查询

```java
// 检查当前状态
String version = player.getCurrentLibraryVersion();
String storageInfo = player.getStorageInfo();
List<File> files = player.getDownloadedActionFiles();
boolean isFirstLaunch = player.isFirstLaunch();

Log.d(TAG, "版本: " + version);
Log.d(TAG, "存储: " + storageInfo);
Log.d(TAG, "文件数: " + files.size());
Log.d(TAG, "首次启动: " + isFirstLaunch);
```

### 3. 错误监控

```java
@Override
public void onError(String error) {
    // 记录到崩溃分析系统
    // Crashlytics.log("ActionLibrary Error: " + error);
    
    // 记录到分析系统
    // Analytics.logEvent("action_library_error", error);
    
    Log.e(TAG, "动作库错误: " + error);
}
```

## 常见问题

### Q: 首次启动检查会影响应用启动速度吗？
A: 不会。首次启动检查是异步进行的，延迟3秒执行，不会阻塞应用启动。

### Q: 网络异常会导致应用崩溃吗？
A: 不会。所有网络操作都有完善的异常处理，网络异常只会记录日志，不影响应用正常使用。

### Q: 如何知道动作库是否更新成功？
A: 可以通过回调接口获得详细的更新状态，也可以查询当前版本和文件列表。

### Q: 可以禁用自动更新吗？
A: 可以。只需要不调用首次启动检查和定期检查接口，仅保留手动检查功能。

### Q: 更新的动作文件存储在哪里？
A: 默认存储在应用内部存储目录，程序重启不会丢失，应用卸载时会自动清理。

## 总结

通过以上集成方式，你的应用将具备：

- ✅ **首次启动自动检查** - 不影响启动速度
- ✅ **手动触发检查** - 用户主动更新
- ✅ **完善异常处理** - 网络异常不影响应用
- ✅ **优秀用户体验** - 后台更新，前台反馈
- ✅ **资源管理** - 自动清理，无内存泄漏