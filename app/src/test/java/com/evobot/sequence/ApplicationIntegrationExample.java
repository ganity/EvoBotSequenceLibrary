package com.evobot.sequence;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * 应用集成示例
 * 展示如何在应用中集成EvoBot动作库更新功能
 */
public class ApplicationIntegrationExample {
    
    private static final String TAG = "AppIntegrationExample";
    
    /**
     * 示例1：Application类中的集成
     */
    public static class MyApplication extends Application {
        
        private EvoBotSequencePlayer globalPlayer;
        
        @Override
        public void onCreate() {
            super.onCreate();
            
            Log.d(TAG, "应用启动，初始化EvoBot动作库");
            
            // 1. 创建全局播放器实例
            ActionLibraryConfig config = new ActionLibraryConfig(
                "http://localhost:9189/api/v1",
                "EVOBOT-PRD-00000001",
                "ak_7x9m2n8p4q1r5s6t"
            );
            
            globalPlayer = new EvoBotSequencePlayer(this, config);
            
            // 2. 延迟进行首次启动检查（不阻塞应用启动）
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                performFirstLaunchCheck();
            }, 3000); // 延迟3秒，确保应用完全启动
        }
        
        /**
         * 首次启动检查
         */
        private void performFirstLaunchCheck() {
            Log.d(TAG, "开始首次启动检查");
            
            globalPlayer.checkForUpdatesOnFirstLaunch(new ActionLibraryUpdater.UpdateCallback() {
                @Override
                public void onNoUpdateNeeded() {
                    Log.d(TAG, "首次启动检查：非首次启动，跳过");
                }
                
                @Override
                public void onNoUpdatesAvailable() {
                    Log.d(TAG, "首次启动检查：没有可用更新");
                }
                
                @Override
                public void onUpdatesFound(int updateCount, long totalSize) {
                    Log.d(TAG, String.format("首次启动检查：发现 %d 个更新，开始后台下载", updateCount));
                    // 可以显示一个不干扰的通知
                    showUpdateNotification("正在下载新动作...", false);
                }
                
                @Override
                public void onDownloadStarted(int fileCount) {
                    Log.d(TAG, "首次启动检查：开始下载 " + fileCount + " 个文件");
                }
                
                @Override
                public void onDownloadCompleted(int savedCount, String newVersion) {
                    Log.d(TAG, String.format("首次启动检查：下载完成，新增 %d 个动作", savedCount));
                    // 显示完成通知
                    showUpdateNotification("动作库更新完成", true);
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "首次启动检查失败（不影响应用使用）: " + error);
                    // 静默处理错误，不干扰用户
                }
            });
        }
        
        /**
         * 显示更新通知（示例）
         */
        private void showUpdateNotification(String message, boolean autoHide) {
            Log.d(TAG, "通知: " + message);
            // 这里可以显示Toast、Snackbar或通知栏通知
            // 建议使用不干扰的方式，如状态栏通知
        }
        
        /**
         * 获取全局播放器实例
         */
        public EvoBotSequencePlayer getGlobalPlayer() {
            return globalPlayer;
        }
        
        @Override
        public void onTerminate() {
            super.onTerminate();
            if (globalPlayer != null) {
                globalPlayer.release();
            }
        }
    }
    
    /**
     * 示例2：MainActivity中的集成
     */
    public static class MainActivity {
        
        private EvoBotSequencePlayer player;
        private Context context;
        
        public void onCreate(Context context) {
            this.context = context;
            
            // 获取全局播放器实例
            if (context.getApplicationContext() instanceof MyApplication) {
                MyApplication app = (MyApplication) context.getApplicationContext();
                player = app.getGlobalPlayer();
            }
            
            // 设置UI事件
            setupUpdateButton();
        }
        
        /**
         * 设置手动更新按钮
         */
        private void setupUpdateButton() {
            // 模拟按钮点击事件
            onUpdateButtonClick();
        }
        
        /**
         * 用户点击"检查更新"按钮
         */
        public void onUpdateButtonClick() {
            Log.d(TAG, "用户点击检查更新按钮");
            
            // 显示加载状态
            showLoadingDialog("正在检查更新...");
            
            player.manualCheckForUpdates(new ActionLibraryUpdater.UpdateCallback() {
                @Override
                public void onNoUpdateNeeded() {
                    hideLoadingDialog();
                    showMessage("当前已是最新版本");
                }
                
                @Override
                public void onNoUpdatesAvailable() {
                    hideLoadingDialog();
                    showMessage("当前已是最新版本");
                }
                
                @Override
                public void onUpdatesFound(int updateCount, long totalSize) {
                    updateLoadingDialog(String.format("发现 %d 个更新，正在下载...", updateCount));
                }
                
                @Override
                public void onDownloadStarted(int fileCount) {
                    updateLoadingDialog("正在下载 " + fileCount + " 个动作文件...");
                }
                
                @Override
                public void onDownloadCompleted(int savedCount, String newVersion) {
                    hideLoadingDialog();
                    showMessage(String.format("更新完成！新增 %d 个动作", savedCount));
                    
                    // 可以刷新动作列表UI
                    refreshActionList();
                }
                
                @Override
                public void onError(String error) {
                    hideLoadingDialog();
                    showErrorDialog("更新失败", error);
                }
            });
        }
        
        /**
         * 显示加载对话框
         */
        private void showLoadingDialog(String message) {
            Log.d(TAG, "显示加载对话框: " + message);
            // 实现加载对话框显示逻辑
        }
        
        /**
         * 更新加载对话框内容
         */
        private void updateLoadingDialog(String message) {
            Log.d(TAG, "更新加载对话框: " + message);
            // 实现加载对话框更新逻辑
        }
        
        /**
         * 隐藏加载对话框
         */
        private void hideLoadingDialog() {
            Log.d(TAG, "隐藏加载对话框");
            // 实现加载对话框隐藏逻辑
        }
        
        /**
         * 显示消息
         */
        private void showMessage(String message) {
            Log.d(TAG, "显示消息: " + message);
            // 可以使用Toast、Snackbar等
        }
        
        /**
         * 显示错误对话框
         */
        private void showErrorDialog(String title, String message) {
            Log.e(TAG, title + ": " + message);
            // 实现错误对话框显示逻辑
        }
        
        /**
         * 刷新动作列表
         */
        private void refreshActionList() {
            Log.d(TAG, "刷新动作列表UI");
            // 实现动作列表刷新逻辑
        }
    }
    
    /**
     * 示例3：设置页面中的集成
     */
    public static class SettingsActivity {
        
        private EvoBotSequencePlayer player;
        
        public void onCreate(Context context) {
            // 获取播放器实例
            if (context.getApplicationContext() instanceof MyApplication) {
                MyApplication app = (MyApplication) context.getApplicationContext();
                player = app.getGlobalPlayer();
            }
            
            // 显示当前状态
            displayCurrentStatus();
        }
        
        /**
         * 显示当前动作库状态
         */
        private void displayCurrentStatus() {
            // 获取版本信息
            String currentVersion = player.getCurrentLibraryVersion();
            Log.d(TAG, "当前动作库版本: " + currentVersion);
            
            // 获取存储信息
            String storageInfo = player.getStorageInfo();
            Log.d(TAG, "存储信息:\n" + storageInfo);
            
            // 获取下载的文件列表
            java.util.List<java.io.File> files = player.getDownloadedActionFiles();
            Log.d(TAG, "已下载动作数量: " + files.size());
            
            // 检查是否是首次启动
            boolean isFirstLaunch = player.isFirstLaunch();
            Log.d(TAG, "是否首次启动: " + isFirstLaunch);
        }
        
        /**
         * 清理动作库
         */
        public void onClearActionLibraryClick() {
            Log.d(TAG, "用户点击清理动作库");
            
            // 显示确认对话框
            showConfirmDialog("确定要清理已下载的动作库吗？", () -> {
                player.clearDownloadedActions();
                showMessage("动作库已清理");
                displayCurrentStatus(); // 刷新状态显示
            });
        }
        
        /**
         * 显示确认对话框
         */
        private void showConfirmDialog(String message, Runnable onConfirm) {
            Log.d(TAG, "显示确认对话框: " + message);
            // 实现确认对话框，用户确认后执行onConfirm
            if (onConfirm != null) {
                onConfirm.run(); // 示例中直接执行
            }
        }
        
        /**
         * 显示消息
         */
        private void showMessage(String message) {
            Log.d(TAG, "显示消息: " + message);
        }
    }
    
    /**
     * 示例4：后台服务中的定期检查
     */
    public static class BackgroundUpdateService {
        
        private EvoBotSequencePlayer player;
        private java.util.concurrent.ScheduledExecutorService scheduler;
        
        public void onCreate(Context context) {
            // 获取播放器实例
            if (context.getApplicationContext() instanceof MyApplication) {
                MyApplication app = (MyApplication) context.getApplicationContext();
                player = app.getGlobalPlayer();
            }
            
            // 启动定期检查
            startPeriodicCheck();
        }
        
        /**
         * 启动定期检查
         */
        private void startPeriodicCheck() {
            scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            
            // 每24小时检查一次
            scheduler.scheduleAtFixedRate(() -> {
                Log.d(TAG, "后台定期检查动作库更新");
                
                player.checkForUpdates(new ActionLibraryUpdater.UpdateCallback() {
                    @Override
                    public void onNoUpdateNeeded() {
                        Log.d(TAG, "后台检查：无需更新");
                    }
                    
                    @Override
                    public void onNoUpdatesAvailable() {
                        Log.d(TAG, "后台检查：没有更新");
                    }
                    
                    @Override
                    public void onUpdatesFound(int updateCount, long totalSize) {
                        Log.d(TAG, "后台检查：发现更新，开始下载");
                        // 可以发送通知
                        sendUpdateNotification("正在更新动作库...");
                    }
                    
                    @Override
                    public void onDownloadCompleted(int savedCount, String newVersion) {
                        Log.d(TAG, "后台检查：更新完成");
                        // 发送完成通知
                        sendUpdateNotification("动作库更新完成，新增 " + savedCount + " 个动作");
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "后台检查失败: " + error);
                        // 静默处理，不干扰用户
                    }
                    
                    @Override
                    public void onDownloadStarted(int fileCount) {
                        Log.d(TAG, "后台检查：开始下载");
                    }
                });
                
            }, 1, 24, java.util.concurrent.TimeUnit.HOURS);
        }
        
        /**
         * 发送更新通知
         */
        private void sendUpdateNotification(String message) {
            Log.d(TAG, "发送通知: " + message);
            // 实现通知栏通知
        }
        
        public void onDestroy() {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
        }
    }
    
    /**
     * 示例5：网络状态监听
     */
    public static class NetworkStateListener {
        
        private EvoBotSequencePlayer player;
        
        public void onCreate(Context context) {
            // 获取播放器实例
            if (context.getApplicationContext() instanceof MyApplication) {
                MyApplication app = (MyApplication) context.getApplicationContext();
                player = app.getGlobalPlayer();
            }
            
            // 注册网络状态监听
            registerNetworkListener(context);
        }
        
        /**
         * 注册网络状态监听
         */
        private void registerNetworkListener(Context context) {
            // 模拟网络状态变化
            onNetworkAvailable();
        }
        
        /**
         * 网络可用时的处理
         */
        public void onNetworkAvailable() {
            Log.d(TAG, "网络已连接，检查动作库更新");
            
            // 网络恢复时检查更新
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
                    Log.d(TAG, "网络恢复检查：发现更新");
                }
                
                @Override
                public void onDownloadCompleted(int savedCount, String newVersion) {
                    Log.d(TAG, "网络恢复检查：更新完成");
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "网络恢复检查失败: " + error);
                }
                
                @Override
                public void onDownloadStarted(int fileCount) {
                    Log.d(TAG, "网络恢复检查：开始下载");
                }
            });
        }
    }
}