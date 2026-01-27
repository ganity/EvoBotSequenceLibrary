package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

/**
 * HTTP动作库测试运行器
 * 用于快速测试HTTP动作库功能
 */
public class HttpActionLibraryTestRunner {
    
    private static final String TAG = "HttpActionLibraryTestRunner";
    
    /**
     * 运行基本功能测试
     */
    public static void runBasicTest(Context context) {
        Log.d(TAG, "开始运行HTTP动作库基本功能测试...");
        
        try {
            // 1. 测试配置创建
            ActionLibraryConfig config = ActionLibraryConfig.createDefault();
            Log.d(TAG, "✓ 配置创建成功: " + config.toString());
            
            // 2. 测试缓存管理器
            ActionCacheManager cacheManager = new ActionCacheManager(context);
            ActionCacheManager.CacheStats stats = cacheManager.getCacheStats();
            Log.d(TAG, "✓ 缓存管理器初始化成功: " + stats.toString());
            
            // 3. 测试HTTP客户端
            ActionLibraryClient client = new ActionLibraryClient(config);
            Log.d(TAG, "✓ HTTP客户端创建成功");
            
            // 4. 测试动作库管理器
            ActionLibraryManager manager = new ActionLibraryManager(context, config);
            Log.d(TAG, "✓ 动作库管理器创建成功");
            
            // 5. 测试播放器集成
            EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, config);
            Log.d(TAG, "✓ 播放器创建成功");
            
            // 清理资源
            client.release();
            manager.release();
            player.release();
            
            Log.d(TAG, "✓ 所有基本功能测试通过");
            
        } catch (Exception e) {
            Log.e(TAG, "✗ 基本功能测试失败", e);
        }
    }
    
    /**
     * 运行网络连接测试
     */
    public static void runNetworkTest(Context context) {
        Log.d(TAG, "开始运行网络连接测试...");
        
        ActionLibraryConfig config = new ActionLibraryConfig(
            "http://localhost:9189/api/v1",
            "EVOBOT-PRD-00000001",
            "ak_7x9m2n8p4q1r5s6t"
        );
        
        ActionLibraryClient client = new ActionLibraryClient(config);
        
        // 测试更新检查
        client.checkUpdatesAsync("1.0.0", new ActionLibraryClient.UpdateCheckCallback() {
            @Override
            public void onSuccess(ActionLibraryClient.UpdateCheckResult result) {
                Log.d(TAG, "✓ 网络连接测试成功: " + result.toString());
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "✗ 网络连接测试失败（服务器可能未运行）: " + error);
            }
        });
        
        // 延迟清理
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                client.release();
            } catch (InterruptedException e) {
                Log.e(TAG, "清理被中断", e);
            }
        }).start();
    }
    
    /**
     * 运行完整测试套件
     */
    public static void runFullTestSuite(Context context) {
        Log.d(TAG, "开始运行完整测试套件...");
        
        // 运行基本测试
        runBasicTest(context);
        
        // 等待一段时间
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "测试被中断", e);
        }
        
        // 运行网络测试
        runNetworkTest(context);
        
        // 运行详细测试
        // ActionLibraryTest detailedTest = new ActionLibraryTest(context);
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Log.d(TAG, "详细测试已跳过 - ActionLibraryTest类未实现");
                // detailedTest.runAllTests();
            } catch (InterruptedException e) {
                Log.e(TAG, "详细测试被中断", e);
            }
        }).start();
        
        // 运行示例
        // ActionLibraryExample example = new ActionLibraryExample(context);
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Log.d(TAG, "示例运行已跳过 - ActionLibraryExample类未实现");
                // example.runAllExamples();
            } catch (Exception e) {
                Log.e(TAG, "示例运行失败", e);
            }
        }).start();
        
        Log.d(TAG, "完整测试套件已启动，请查看日志输出");
    }
}