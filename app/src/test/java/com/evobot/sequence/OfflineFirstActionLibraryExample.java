package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.List;

/**
 * 离线优先动作库使用示例
 * 演示如何使用离线优先的HTTP动作库更新机制
 */
public class OfflineFirstActionLibraryExample {
    
    private static final String TAG = "OfflineFirstExample";
    
    private final Context context;
    private EvoBotSequencePlayer player;
    
    public OfflineFirstActionLibraryExample(Context context) {
        this.context = context;
    }
    
    /**
     * 示例1：基本的离线优先动作库使用
     */
    public void basicOfflineFirstExample() {
        Log.d(TAG, "=== 离线优先动作库使用示例 ===");
        
        // 1. 创建动作库配置
        ActionLibraryConfig config = new ActionLibraryConfig(
            "http://localhost:9189/api/v1",  // 服务器地址
            "EVOBOT-PRD-00000001",           // 机器人ID
            "ak_7x9m2n8p4q1r5s6t"            // API Key
        );
        
        // 2. 创建支持离线优先更新的播放器
        player = new EvoBotSequencePlayer(context, config);
        
        // 3. 播放动作（优先从下载的动作库加载，回退到assets）
        // 使用英文名称确保兼容性
        player.play("arm_movement_left_arm_wave", new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                // 处理每帧数据
                Log.v(TAG, String.format("帧 %d: 左臂=%s", frameIndex, java.util.Arrays.toString(leftArm)));
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
    }
    
    /**
     * 示例2：应用启动时检查更新
     */
    public void applicationStartupUpdateCheck() {
        Log.d(TAG, "=== 应用启动时更新检查示例 ===");
        
        if (player == null) {
            basicOfflineFirstExample();
        }
        
        // 检查是否需要更新（基于时间间隔）
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
    }
    
    /**
     * 示例3：用户手动检查更新
     */
    public void manualUpdateCheck() {
        Log.d(TAG, "=== 手动更新检查示例 ===");
        
        if (player == null) {
            basicOfflineFirstExample();
        }
        
        // 强制检查更新（忽略时间间隔）
        player.forceCheckForUpdates(new ActionLibraryUpdater.UpdateCallback() {
            @Override
            public void onNoUpdateNeeded() {
                // 强制检查不会触发这个回调
            }
            
            @Override
            public void onNoUpdatesAvailable() {
                Log.d(TAG, "手动检查：没有可用更新");
                // 可以显示"已是最新版本"的提示
            }
            
            @Override
            public void onUpdatesFound(int updateCount, long totalSize) {
                Log.d(TAG, String.format("手动检查：发现 %d 个更新", updateCount));
                // 可以显示更新确认对话框
            }
            
            @Override
            public void onDownloadStarted(int fileCount) {
                Log.d(TAG, "手动更新：开始下载");
                // 可以显示进度对话框
            }
            
            @Override
            public void onDownloadCompleted(int savedCount, String newVersion) {
                Log.d(TAG, "手动更新：完成");
                // 可以显示"更新成功"提示
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "手动更新失败: " + error);
                // 可以显示错误提示
            }
        });
    }
    
    /**
     * 示例4：查看本地动作库状态
     */
    public void checkLocalLibraryStatus() {
        Log.d(TAG, "=== 本地动作库状态示例 ===");
        
        if (player == null) {
            basicOfflineFirstExample();
        }
        
        // 获取当前版本
        String currentVersion = player.getCurrentLibraryVersion();
        Log.d(TAG, "当前动作库版本: " + currentVersion);
        
        // 获取所有下载的动作文件
        List<File> downloadedFiles = player.getDownloadedActionFiles();
        Log.d(TAG, "已下载的动作文件数量: " + downloadedFiles.size());
        
        for (File file : downloadedFiles) {
            Log.d(TAG, String.format("- %s (大小: %d bytes, 修改时间: %s)", 
                file.getName(), file.length(), 
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(file.lastModified()))));
        }
    }
    
    /**
     * 示例5：定期更新检查（后台任务）
     */
    public void schedulePeriodicUpdateCheck() {
        Log.d(TAG, "=== 定期更新检查示例 ===");
        
        if (player == null) {
            basicOfflineFirstExample();
        }
        
        // 使用定时器每24小时检查一次更新
        java.util.concurrent.ScheduledExecutorService scheduler = 
            java.util.concurrent.Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            Log.d(TAG, "执行定期更新检查");
            
            player.checkForUpdates(new ActionLibraryUpdater.UpdateCallback() {
                @Override
                public void onNoUpdateNeeded() {
                    Log.d(TAG, "定期检查：无需更新");
                }
                
                @Override
                public void onNoUpdatesAvailable() {
                    Log.d(TAG, "定期检查：没有更新");
                }
                
                @Override
                public void onUpdatesFound(int updateCount, long totalSize) {
                    Log.d(TAG, String.format("定期检查：发现 %d 个更新，自动下载中...", updateCount));
                }
                
                @Override
                public void onDownloadStarted(int fileCount) {
                    Log.d(TAG, "定期检查：开始后台下载");
                }
                
                @Override
                public void onDownloadCompleted(int savedCount, String newVersion) {
                    Log.d(TAG, "定期检查：后台更新完成");
                    // 可以发送通知告知用户有新动作可用
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "定期检查失败: " + error);
                }
            });
            
        }, 0, 24, java.util.concurrent.TimeUnit.HOURS);
        
        // 注意：在实际应用中应该在适当时机关闭scheduler
        // scheduler.shutdown();
    }
    
    /**
     * 示例6：网络状态变化时检查更新
     */
    public void networkStateChangeUpdateCheck() {
        Log.d(TAG, "=== 网络状态变化更新检查示例 ===");
        
        if (player == null) {
            basicOfflineFirstExample();
        }
        
        // 模拟网络从断开恢复的情况
        Log.d(TAG, "模拟网络恢复，检查更新...");
        
        player.checkForUpdates(new ActionLibraryUpdater.UpdateCallback() {
            @Override
            public void onNoUpdateNeeded() {
                Log.d(TAG, "网络恢复检查：无需更新");
            }
            
            @Override
            public void onNoUpdatesAvailable() {
                Log.d(TAG, "网络恢复检查：没有更新");
            }
            
            @Override
            public void onUpdatesFound(int updateCount, long totalSize) {
                Log.d(TAG, "网络恢复检查：发现更新，开始下载");
            }
            
            @Override
            public void onDownloadStarted(int fileCount) {
                Log.d(TAG, "网络恢复检查：下载开始");
            }
            
            @Override
            public void onDownloadCompleted(int savedCount, String newVersion) {
                Log.d(TAG, "网络恢复检查：下载完成");
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "网络恢复检查失败: " + error);
            }
        });
    }
    
    /**
     * 示例7：清理本地动作库
     */
    public void clearLocalLibraryExample() {
        Log.d(TAG, "=== 清理本地动作库示例 ===");
        
        if (player == null) {
            basicOfflineFirstExample();
        }
        
        // 显示清理前的状态
        checkLocalLibraryStatus();
        
        // 清理下载的动作文件
        player.clearDownloadedActions();
        Log.d(TAG, "本地动作库已清理");
        
        // 显示清理后的状态
        checkLocalLibraryStatus();
    }
    
    /**
     * 示例8：完整的应用生命周期集成
     */
    public void applicationLifecycleIntegration() {
        Log.d(TAG, "=== 应用生命周期集成示例 ===");
        
        // 应用启动时
        Log.d(TAG, "应用启动 - 初始化播放器");
        basicOfflineFirstExample();
        
        // 延迟检查更新（避免影响启动速度）
        new Thread(() -> {
            try {
                Thread.sleep(3000); // 等待3秒
                Log.d(TAG, "应用启动后 - 检查更新");
                applicationStartupUpdateCheck();
            } catch (InterruptedException e) {
                Log.e(TAG, "启动更新检查被中断", e);
            }
        }).start();
        
        // 模拟用户操作
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 等待5秒
                Log.d(TAG, "用户操作 - 播放动作");
                
                // 播放动作（此时可能已经有更新的动作可用）
                // 使用英文名称确保兼容性
                player.play("arm_movement_right_arm_wave", new SequenceListener() {
                    @Override
                    public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                        // 处理帧数据
                    }
                    
                    @Override
                    public void onComplete() {
                        Log.d(TAG, "用户操作 - 动作播放完成");
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "用户操作 - 播放错误: " + error);
                    }
                    
                    @Override
                    public void onEmergencyStop() {
                        Log.w(TAG, "用户操作 - 急停");
                    }
                });
                
            } catch (InterruptedException e) {
                Log.e(TAG, "用户操作被中断", e);
            }
        }).start();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
    
    /**
     * 运行所有示例
     */
    public void runAllExamples() {
        Log.d(TAG, "开始运行所有离线优先动作库示例...");
        
        try {
            basicOfflineFirstExample();
            Thread.sleep(2000);
            
            applicationStartupUpdateCheck();
            Thread.sleep(3000);
            
            checkLocalLibraryStatus();
            Thread.sleep(1000);
            
            manualUpdateCheck();
            Thread.sleep(3000);
            
            schedulePeriodicUpdateCheck();
            Thread.sleep(2000);
            
            networkStateChangeUpdateCheck();
            Thread.sleep(3000);
            
            clearLocalLibraryExample();
            Thread.sleep(1000);
            
            applicationLifecycleIntegration();
            
        } catch (InterruptedException e) {
            Log.e(TAG, "示例执行被中断", e);
        } finally {
            // 延迟清理资源
            new Thread(() -> {
                try {
                    Thread.sleep(10000); // 等待所有操作完成
                    cleanup();
                } catch (InterruptedException e) {
                    Log.e(TAG, "清理过程被中断", e);
                }
            }).start();
        }
    }
}