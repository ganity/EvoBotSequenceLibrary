package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

/**
 * 动作库功能测试类
 * 用于测试HTTP动作库的各项功能
 */
public class ActionLibraryTest {
    
    private static final String TAG = "ActionLibraryTest";
    
    private final Context context;
    private ActionLibraryConfig testConfig;
    private ActionLibraryClient testClient;
    private ActionCacheManager testCacheManager;
    
    public ActionLibraryTest(Context context) {
        this.context = context;
        setupTestEnvironment();
    }
    
    /**
     * 设置测试环境
     */
    private void setupTestEnvironment() {
        // 创建测试配置
        testConfig = new ActionLibraryConfig(
            "http://localhost:9189/api/v1",
            "EVOBOT-PRD-00000001",
            "ak_7x9m2n8p4q1r5s6t",
            true,  // 启用缓存
            true,  // 启用补偿
            true   // 启用安全检查
        );
        
        testClient = new ActionLibraryClient(testConfig);
        testCacheManager = new ActionCacheManager(context);
        
        Log.d(TAG, "测试环境设置完成");
    }
    
    /**
     * 测试1：配置类测试
     */
    public void testActionLibraryConfig() {
        Log.d(TAG, "=== 测试ActionLibraryConfig ===");
        
        // 测试默认配置
        ActionLibraryConfig defaultConfig = ActionLibraryConfig.createDefault();
        Log.d(TAG, "默认配置: " + defaultConfig.toString());
        
        // 测试自定义配置
        ActionLibraryConfig customConfig = new ActionLibraryConfig(
            "http://custom-server:8080/api/v1",
            "CUSTOM-ROBOT-001",
            "custom_api_key",
            false, false, false
        );
        Log.d(TAG, "自定义配置: " + customConfig.toString());
        
        // 验证配置值
        assert customConfig.getBaseUrl().equals("http://custom-server:8080/api/v1");
        assert customConfig.getRobotId().equals("CUSTOM-ROBOT-001");
        assert customConfig.getApiKey().equals("custom_api_key");
        assert !customConfig.isEnableCache();
        assert !customConfig.isEnableCompensation();
        assert !customConfig.isEnableSafetyCheck();
        
        Log.d(TAG, "ActionLibraryConfig测试通过");
    }
    
    /**
     * 测试2：HTTP客户端认证
     */
    public void testHttpClientAuthentication() {
        Log.d(TAG, "=== 测试HTTP客户端认证 ===");
        
        // 测试更新检查（需要服务器运行）
        testClient.checkUpdatesAsync("1.0.0", new ActionLibraryClient.UpdateCheckCallback() {
            @Override
            public void onSuccess(ActionLibraryClient.UpdateCheckResult result) {
                Log.d(TAG, "认证测试成功: " + result.toString());
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "认证测试失败（可能是服务器未运行）: " + error);
            }
        });
        
        // 测试动作列表获取
        testClient.getSequenceListAsync(null, 10, 0, new ActionLibraryClient.SequenceListCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                Log.d(TAG, "动作列表获取成功，响应长度: " + jsonResponse.length());
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "动作列表获取失败: " + error);
            }
        });
    }
    
    /**
     * 测试3：缓存管理器
     */
    public void testCacheManager() {
        Log.d(TAG, "=== 测试缓存管理器 ===");
        
        // 测试缓存统计
        ActionCacheManager.CacheStats initialStats = testCacheManager.getCacheStats();
        Log.d(TAG, "初始缓存统计: " + initialStats.toString());
        
        // 测试缓存动作
        String testActionName = "测试动作";
        byte[] testData = "这是测试动作数据".getBytes();
        String testHash = "test_hash_123";
        
        boolean cached = testCacheManager.cacheAction(testActionName, testData, testHash);
        Log.d(TAG, "缓存测试动作结果: " + cached);
        
        // 测试缓存检查
        boolean isCached = testCacheManager.isCached(testActionName);
        Log.d(TAG, "缓存检查结果: " + isCached);
        
        // 测试获取缓存文件
        if (isCached) {
            java.io.File cachedFile = testCacheManager.getCachedFile(testActionName);
            Log.d(TAG, "缓存文件路径: " + (cachedFile != null ? cachedFile.getAbsolutePath() : "null"));
        }
        
        // 测试缓存统计更新
        ActionCacheManager.CacheStats updatedStats = testCacheManager.getCacheStats();
        Log.d(TAG, "更新后缓存统计: " + updatedStats.toString());
        
        // 测试删除缓存
        boolean removed = testCacheManager.removeAction(testActionName);
        Log.d(TAG, "删除缓存结果: " + removed);
        
        Log.d(TAG, "缓存管理器测试完成");
    }
    
    /**
     * 测试4：动作库管理器
     */
    public void testActionLibraryManager() {
        Log.d(TAG, "=== 测试动作库管理器 ===");
        
        ActionLibraryManager manager = new ActionLibraryManager(context, testConfig);
        
        // 测试更新检查
        manager.checkUpdatesAsync("1.0.0", new ActionLibraryManager.UpdateCheckCallback() {
            @Override
            public void onUpdateCheckComplete(boolean hasUpdates, int updateCount, String details) {
                Log.d(TAG, String.format("管理器更新检查: hasUpdates=%s, count=%d", hasUpdates, updateCount));
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "管理器更新检查失败: " + error);
            }
        });
        
        // 测试动作列表获取
        manager.getActionListAsync("arm_movement", new ActionLibraryManager.ActionListCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                Log.d(TAG, "管理器动作列表获取成功");
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "管理器动作列表获取失败: " + error);
            }
        });
        
        // 测试预加载
        String[] testActions = {"左臂挥手", "右臂挥手"};
        manager.preloadCommonActionsAsync(testActions, new ActionLibraryManager.PreloadCallback() {
            @Override
            public void onComplete(int successCount, int totalCount) {
                Log.d(TAG, String.format("预加载完成: %d/%d", successCount, totalCount));
            }
        });
        
        // 测试缓存统计
        ActionCacheManager.CacheStats stats = manager.getCacheStats();
        Log.d(TAG, "管理器缓存统计: " + (stats != null ? stats.toString() : "null"));
        
        // 清理
        manager.release();
        Log.d(TAG, "动作库管理器测试完成");
    }
    
    /**
     * 测试5：播放器集成
     */
    public void testPlayerIntegration() {
        Log.d(TAG, "=== 测试播放器集成 ===");
        
        // 创建支持HTTP动作库的播放器
        EvoBotSequencePlayer player = new EvoBotSequencePlayer(context, testConfig);
        
        // 测试缓存统计
        ActionCacheManager.CacheStats stats = player.getCacheStats();
        Log.d(TAG, "播放器缓存统计: " + (stats != null ? stats.toString() : "null"));
        
        // 测试更新检查
        player.checkUpdates("1.0.0", new ActionLibraryManager.UpdateCheckCallback() {
            @Override
            public void onUpdateCheckComplete(boolean hasUpdates, int updateCount, String details) {
                Log.d(TAG, "播放器更新检查完成");
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "播放器更新检查失败: " + error);
            }
        });
        
        // 测试动作列表获取
        player.getActionList("arm_movement", new ActionLibraryManager.ActionListCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                Log.d(TAG, "播放器动作列表获取成功");
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "播放器动作列表获取失败: " + error);
            }
        });
        
        // 测试预加载
        String[] actions = {"测试动作1", "测试动作2"};
        player.preloadActions(actions, new ActionLibraryManager.PreloadCallback() {
            @Override
            public void onComplete(int successCount, int totalCount) {
                Log.d(TAG, String.format("播放器预加载完成: %d/%d", successCount, totalCount));
            }
        });
        
        // 清理
        player.release();
        Log.d(TAG, "播放器集成测试完成");
    }
    
    /**
     * 测试6：错误处理
     */
    public void testErrorHandling() {
        Log.d(TAG, "=== 测试错误处理 ===");
        
        // 测试无效配置
        try {
            new ActionLibraryConfig(null, null, null);
            Log.e(TAG, "应该抛出异常但没有");
        } catch (Exception e) {
            Log.d(TAG, "正确处理了无效配置: " + e.getMessage());
        }
        
        // 测试无效上下文
        try {
            new ActionCacheManager(null);
            Log.e(TAG, "应该抛出异常但没有");
        } catch (Exception e) {
            Log.d(TAG, "正确处理了无效上下文: " + e.getMessage());
        }
        
        // 测试网络错误（使用无效服务器地址）
        ActionLibraryConfig invalidConfig = new ActionLibraryConfig(
            "http://invalid-server:9999/api/v1",
            "TEST-ROBOT",
            "invalid_key"
        );
        
        ActionLibraryClient invalidClient = new ActionLibraryClient(invalidConfig);
        invalidClient.checkUpdatesAsync("1.0.0", new ActionLibraryClient.UpdateCheckCallback() {
            @Override
            public void onSuccess(ActionLibraryClient.UpdateCheckResult result) {
                Log.e(TAG, "不应该成功");
            }
            
            @Override
            public void onError(String error) {
                Log.d(TAG, "正确处理了网络错误: " + error);
            }
        });
        
        invalidClient.release();
        Log.d(TAG, "错误处理测试完成");
    }
    
    /**
     * 运行所有测试
     */
    public void runAllTests() {
        Log.d(TAG, "开始运行所有动作库测试...");
        
        try {
            testActionLibraryConfig();
            Thread.sleep(500);
            
            testCacheManager();
            Thread.sleep(500);
            
            testHttpClientAuthentication();
            Thread.sleep(1000);
            
            testActionLibraryManager();
            Thread.sleep(1000);
            
            testPlayerIntegration();
            Thread.sleep(1000);
            
            testErrorHandling();
            Thread.sleep(1000);
            
            Log.d(TAG, "所有测试完成");
            
        } catch (InterruptedException e) {
            Log.e(TAG, "测试被中断", e);
        } finally {
            cleanup();
        }
    }
    
    /**
     * 清理测试资源
     */
    private void cleanup() {
        if (testClient != null) {
            testClient.release();
        }
        
        if (testCacheManager != null) {
            testCacheManager.clearCache();
        }
        
        Log.d(TAG, "测试资源清理完成");
    }
}