package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

/**
 * 动作库使用示例
 * 演示如何使用HTTP动作库功能
 */
public class ActionLibraryExample {
    
    private static final String TAG = "ActionLibraryExample";
    
    private final Context context;
    private EvoBotSequencePlayer player;
    
    public ActionLibraryExample(Context context) {
        this.context = context;
    }
    
    /**
     * 示例1：基本的HTTP动作库使用
     */
    public void basicHttpLibraryExample() {
        Log.d(TAG, "=== 基本HTTP动作库使用示例 ===");
        
        // 1. 创建动作库配置
        ActionLibraryConfig config = new ActionLibraryConfig(
            "http://localhost:9189/api/v1",  // 服务器地址
            "EVOBOT-PRD-00000001",           // 机器人ID
            "ak_7x9m2n8p4q1r5s6t"            // API Key
        );
        
        // 2. 创建支持HTTP动作库的播放器
        player = new EvoBotSequencePlayer(context, config);
        
        // 3. 播放网络动作（自动下载和缓存）
        player.play("左臂挥手", new SequenceListener() {
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
     * 示例2：更新检查和预加载
     */
    public void updateAndPreloadExample() {
        Log.d(TAG, "=== 更新检查和预加载示例 ===");
        
        if (player == null) {
            basicHttpLibraryExample();
        }
        
        // 1. 检查动作库更新
        player.checkUpdates("1.0.0", new ActionLibraryManager.UpdateCheckCallback() {
            @Override
            public void onUpdateCheckComplete(boolean hasUpdates, int updateCount, String details) {
                Log.d(TAG, String.format("更新检查完成: hasUpdates=%s, count=%d", hasUpdates, updateCount));
                if (hasUpdates) {
                    Log.d(TAG, "发现更新，详情: " + details);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "更新检查失败: " + error);
            }
        });
        
        // 2. 预加载常用动作
        String[] commonActions = {"左臂挥手", "右臂挥手", "双臂抱胸"};
        player.preloadActions(commonActions, new ActionLibraryManager.PreloadCallback() {
            @Override
            public void onComplete(int successCount, int totalCount) {
                Log.d(TAG, String.format("预加载完成: %d/%d 成功", successCount, totalCount));
            }
        });
    }
    
    /**
     * 示例3：获取动作列表
     */
    public void getActionListExample() {
        Log.d(TAG, "=== 获取动作列表示例 ===");
        
        if (player == null) {
            basicHttpLibraryExample();
        }
        
        // 获取手臂动作分类的动作列表
        player.getActionList("arm_movement", new ActionLibraryManager.ActionListCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                Log.d(TAG, "动作列表获取成功:");
                Log.d(TAG, jsonResponse);
                
                // 这里可以解析JSON并显示动作列表
                parseAndDisplayActionList(jsonResponse);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "获取动作列表失败: " + error);
            }
        });
    }
    
    /**
     * 示例4：缓存管理
     */
    public void cacheManagementExample() {
        Log.d(TAG, "=== 缓存管理示例 ===");
        
        if (player == null) {
            basicHttpLibraryExample();
        }
        
        // 1. 获取缓存统计
        ActionCacheManager.CacheStats stats = player.getCacheStats();
        if (stats != null) {
            Log.d(TAG, "缓存统计: " + stats.toString());
        }
        
        // 2. 清空缓存（如果需要）
        if (stats != null && stats.getUsagePercentage() > 80) {
            Log.d(TAG, "缓存使用率过高，清空缓存");
            player.clearActionLibraryCache();
        }
    }
    
    /**
     * 示例5：混合模式（HTTP + 本地assets）
     */
    public void hybridModeExample() {
        Log.d(TAG, "=== 混合模式示例 ===");
        
        // 创建配置，启用缓存和安全检查
        ActionLibraryConfig config = new ActionLibraryConfig(
            "http://localhost:9189/api/v1",
            "EVOBOT-PRD-00000001", 
            "ak_7x9m2n8p4q1r5s6t",
            true,  // 启用缓存
            true,  // 启用补偿
            true   // 启用安全检查
        );
        
        player = new EvoBotSequencePlayer(context, config);
        
        // 播放动作时会自动：
        // 1. 首先尝试从HTTP动作库下载
        // 2. 如果网络失败，回退到本地assets
        // 3. 成功下载的动作会缓存到本地
        player.play("新动作", new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                // 处理帧数据
            }
            
            @Override
            public void onComplete() {
                Log.d(TAG, "混合模式播放完成");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "混合模式播放错误: " + error);
            }
            
            @Override
            public void onEmergencyStop() {
                Log.w(TAG, "混合模式急停");
            }
        });
    }
    
    /**
     * 解析并显示动作列表（简化实现）
     */
    private void parseAndDisplayActionList(String jsonResponse) {
        // 简单的字符串解析，实际使用时应该使用JSON解析库
        String[] lines = jsonResponse.split("\n");
        
        Log.d(TAG, "可用动作列表:");
        for (String line : lines) {
            if (line.contains("\"name\":")) {
                String name = extractJsonValue(line, "name");
                String category = extractJsonValue(line, "category");
                if (name != null) {
                    Log.d(TAG, String.format("- %s (%s)", name, category != null ? category : "未知分类"));
                }
            }
        }
    }
    
    /**
     * 从JSON行中提取值（简化实现）
     */
    private String extractJsonValue(String line, String key) {
        String pattern = "\"" + key + "\":\"";
        int startIndex = line.indexOf(pattern);
        if (startIndex >= 0) {
            startIndex += pattern.length();
            int endIndex = line.indexOf("\"", startIndex);
            if (endIndex > startIndex) {
                return line.substring(startIndex, endIndex);
            }
        }
        return null;
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
        Log.d(TAG, "开始运行所有动作库示例...");
        
        try {
            basicHttpLibraryExample();
            
            // 等待一段时间让第一个示例完成
            Thread.sleep(2000);
            
            updateAndPreloadExample();
            Thread.sleep(1000);
            
            getActionListExample();
            Thread.sleep(1000);
            
            cacheManagementExample();
            Thread.sleep(1000);
            
            hybridModeExample();
            
        } catch (InterruptedException e) {
            Log.e(TAG, "示例执行被中断", e);
        } finally {
            // 清理资源
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // 等待所有操作完成
                    cleanup();
                } catch (InterruptedException e) {
                    Log.e(TAG, "清理过程被中断", e);
                }
            }).start();
        }
    }
}