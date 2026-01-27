package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

/**
 * 集成测试示例
 * 验证手动触发检查和首次启动检查功能
 */
public class IntegrationTestExample {
    
    private static final String TAG = "IntegrationTestExample";
    
    private final Context context;
    private EvoBotSequencePlayer player;
    
    public IntegrationTestExample(Context context) {
        this.context = context;
        setupPlayer();
    }
    
    /**
     * 设置播放器
     */
    private void setupPlayer() {
        ActionLibraryConfig config = new ActionLibraryConfig(
            "http://localhost:9189/api/v1",
            "EVOBOT-PRD-00000001",
            "ak_7x9m2n8p4q1r5s6t"
        );
        
        player = new EvoBotSequencePlayer(context, config);
        Log.d(TAG, "播放器初始化完成");
    }
    
    /**
     * 测试1：首次启动检查
     */
    public void testFirstLaunchCheck() {
        Log.d(TAG, "=== 测试首次启动检查 ===");
        
        // 检查是否是首次启动
        boolean isFirstLaunch = player.isFirstLaunch();
        Log.d(TAG, "是否首次启动: " + isFirstLaunch);
        
        // 执行首次启动检查
        player.checkForUpdatesOnFirstLaunch(new ActionLibraryUpdater.UpdateCallback() {
            @Override
            public void onNoUpdateNeeded() {
                Log.d(TAG, "首次启动检查：非首次启动，跳过检查");
            }
            
            @Override
            public void onNoUpdatesAvailable() {
                Log.d(TAG, "首次启动检查：没有可用更新");
            }
            
            @Override
            public void onUpdatesFound(int updateCount, long totalSize) {
                Log.d(TAG, String.format("首次启动检查：发现 %d 个更新，总大小: %d bytes", 
                    updateCount, totalSize));
            }
            
            @Override
            public void onDownloadStarted(int fileCount) {
                Log.d(TAG, "首次启动检查：开始下载 " + fileCount + " 个文件");
            }
            
            @Override
            public void onDownloadCompleted(int savedCount, String newVersion) {
                Log.d(TAG, String.format("首次启动检查：下载完成，保存了 %d 个动作，新版本: %s", 
                    savedCount, newVersion));
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "首次启动检查失败（不影响应用使用）: " + error);
            }
        });
    }
    
    /**
     * 测试2：手动触发检查
     */
    public void testManualCheck() {
        Log.d(TAG, "=== 测试手动触发检查 ===");
        
        player.manualCheckForUpdates(new ActionLibraryUpdater.UpdateCallback() {
            @Override
            public void onNoUpdateNeeded() {
                Log.d(TAG, "手动检查：无需更新");
            }
            
            @Override
            public void onNoUpdatesAvailable() {
                Log.d(TAG, "手动检查：没有可用更新");
            }
            
            @Override
            public void onUpdatesFound(int updateCount, long totalSize) {
                Log.d(TAG, String.format("手动检查：发现 %d 个更新，总大小: %d bytes", 
                    updateCount, totalSize));
            }
            
            @Override
            public void onDownloadStarted(int fileCount) {
                Log.d(TAG, "手动检查：开始下载 " + fileCount + " 个文件");
            }
            
            @Override
            public void onDownloadCompleted(int savedCount, String newVersion) {
                Log.d(TAG, String.format("手动检查：下载完成，保存了 %d 个动作，新版本: %s", 
                    savedCount, newVersion));
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "手动检查失败: " + error);
            }
        });
    }
    
    /**
     * 测试3：定期检查
     */
    public void testPeriodicCheck() {
        Log.d(TAG, "=== 测试定期检查 ===");
        
        player.checkForUpdates(new ActionLibraryUpdater.UpdateCallback() {
            @Override
            public void onNoUpdateNeeded() {
                Log.d(TAG, "定期检查：距离上次检查时间不足，跳过");
            }
            
            @Override
            public void onNoUpdatesAvailable() {
                Log.d(TAG, "定期检查：没有可用更新");
            }
            
            @Override
            public void onUpdatesFound(int updateCount, long totalSize) {
                Log.d(TAG, String.format("定期检查：发现 %d 个更新，总大小: %d bytes", 
                    updateCount, totalSize));
            }
            
            @Override
            public void onDownloadStarted(int fileCount) {
                Log.d(TAG, "定期检查：开始下载 " + fileCount + " 个文件");
            }
            
            @Override
            public void onDownloadCompleted(int savedCount, String newVersion) {
                Log.d(TAG, String.format("定期检查：下载完成，保存了 %d 个动作，新版本: %s", 
                    savedCount, newVersion));
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "定期检查失败: " + error);
            }
        });
    }
    
    /**
     * 测试4：状态查询
     */
    public void testStatusQuery() {
        Log.d(TAG, "=== 测试状态查询 ===");
        
        // 获取当前版本
        String currentVersion = player.getCurrentLibraryVersion();
        Log.d(TAG, "当前动作库版本: " + currentVersion);
        
        // 获取存储信息
        String storageInfo = player.getStorageInfo();
        Log.d(TAG, "存储信息:\n" + storageInfo);
        
        // 获取下载的文件列表
        java.util.List<java.io.File> files = player.getDownloadedActionFiles();
        Log.d(TAG, "已下载动作文件数量: " + files.size());
        
        for (java.io.File file : files) {
            Log.d(TAG, String.format("- %s (大小: %d bytes, 修改时间: %s)", 
                file.getName(), file.length(), 
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date(file.lastModified()))));
        }
        
        // 检查是否是首次启动
        boolean isFirstLaunch = player.isFirstLaunch();
        Log.d(TAG, "是否首次启动: " + isFirstLaunch);
    }
    
    /**
     * 测试5：异常处理
     */
    public void testErrorHandling() {
        Log.d(TAG, "=== 测试异常处理 ===");
        
        // 创建一个使用无效服务器地址的配置
        ActionLibraryConfig invalidConfig = new ActionLibraryConfig(
            "http://invalid-server:9999/api/v1",
            "INVALID-ROBOT",
            "invalid_key"
        );
        
        EvoBotSequencePlayer invalidPlayer = new EvoBotSequencePlayer(context, invalidConfig);
        
        // 测试网络异常处理
        invalidPlayer.manualCheckForUpdates(new ActionLibraryUpdater.UpdateCallback() {
            @Override
            public void onNoUpdateNeeded() {
                Log.d(TAG, "异常测试：不应该到达这里");
            }
            
            @Override
            public void onNoUpdatesAvailable() {
                Log.d(TAG, "异常测试：不应该到达这里");
            }
            
            @Override
            public void onUpdatesFound(int updateCount, long totalSize) {
                Log.d(TAG, "异常测试：不应该到达这里");
            }
            
            @Override
            public void onDownloadStarted(int fileCount) {
                Log.d(TAG, "异常测试：不应该到达这里");
            }
            
            @Override
            public void onDownloadCompleted(int savedCount, String newVersion) {
                Log.d(TAG, "异常测试：不应该到达这里");
            }
            
            @Override
            public void onError(String error) {
                Log.d(TAG, "异常测试：正确捕获网络异常 - " + error);
                
                // 验证错误类型
                if (error.contains("网络连接失败") || error.contains("无法解析服务器地址") || 
                    error.contains("网络请求超时")) {
                    Log.d(TAG, "异常测试：网络异常处理正确");
                } else {
                    Log.d(TAG, "异常测试：其他类型异常 - " + error);
                }
            }
        });
        
        // 清理测试播放器
        invalidPlayer.release();
    }
    
    /**
     * 测试6：播放功能（离线优先）
     */
    public void testPlaybackWithOfflineFirst() {
        Log.d(TAG, "=== 测试离线优先播放 ===");
        
        // 播放动作（优先从下载的动作库加载）
        // 使用英文名称确保兼容性
        player.play("arm_movement_left_arm_wave", new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                if (frameIndex % 10 == 0) { // 每10帧打印一次
                    Log.v(TAG, String.format("播放帧 %d: 左臂=%s", 
                        frameIndex, java.util.Arrays.toString(leftArm)));
                }
            }
            
            @Override
            public void onComplete() {
                Log.d(TAG, "离线优先播放：动作播放完成");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "离线优先播放：播放错误 - " + error);
            }
            
            @Override
            public void onEmergencyStop() {
                Log.w(TAG, "离线优先播放：急停执行");
            }
        });
    }
    
    /**
     * 测试7：清理功能
     */
    public void testCleanup() {
        Log.d(TAG, "=== 测试清理功能 ===");
        
        // 显示清理前的状态
        testStatusQuery();
        
        // 清理下载的动作文件
        player.clearDownloadedActions();
        Log.d(TAG, "已清理下载的动作文件");
        
        // 显示清理后的状态
        testStatusQuery();
    }
    
    /**
     * 运行所有测试
     */
    public void runAllTests() {
        Log.d(TAG, "开始运行所有集成测试...");
        
        try {
            // 测试状态查询
            testStatusQuery();
            Thread.sleep(1000);
            
            // 测试首次启动检查
            testFirstLaunchCheck();
            Thread.sleep(3000);
            
            // 测试手动检查
            testManualCheck();
            Thread.sleep(5000);
            
            // 测试定期检查
            testPeriodicCheck();
            Thread.sleep(3000);
            
            // 测试异常处理
            testErrorHandling();
            Thread.sleep(3000);
            
            // 测试播放功能
            testPlaybackWithOfflineFirst();
            Thread.sleep(2000);
            
            // 停止播放
            player.stop();
            Thread.sleep(1000);
            
            // 测试清理功能
            testCleanup();
            
            Log.d(TAG, "所有集成测试完成");
            
        } catch (InterruptedException e) {
            Log.e(TAG, "测试被中断", e);
        } finally {
            // 清理资源
            cleanup();
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (player != null) {
            player.release();
            player = null;
        }
        Log.d(TAG, "测试资源已清理");
    }
}