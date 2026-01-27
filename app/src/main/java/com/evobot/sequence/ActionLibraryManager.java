package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 动作库管理器
 * 整合HTTP客户端和缓存管理，提供统一的动作库访问接口
 */
public class ActionLibraryManager {
    
    private static final String TAG = "ActionLibraryManager";
    
    private final Context context;
    private final ActionLibraryConfig config;
    private final ActionLibraryClient client;
    private final ActionCacheManager cacheManager;
    private final SequenceLoader sequenceLoader;
    private final ExecutorService executor;
    
    /**
     * 构造函数
     */
    public ActionLibraryManager(Context context, ActionLibraryConfig config) {
        if (context == null) {
            throw new IllegalArgumentException("Context不能为null");
        }
        if (config == null) {
            throw new IllegalArgumentException("ActionLibraryConfig不能为null");
        }
        
        this.context = context.getApplicationContext();
        this.config = config;
        this.client = new ActionLibraryClient(config);
        this.cacheManager = new ActionCacheManager(this.context);
        this.sequenceLoader = new SequenceLoader(this.context);
        this.executor = Executors.newCachedThreadPool();
        
        Log.d(TAG, "动作库管理器初始化完成: " + config.toString());
    }
    
    /**
     * 异步加载动作序列
     * 优先从缓存加载，缓存未命中时从网络下载
     */
    public void loadSequenceAsync(String actionName, LoadSequenceCallback callback) {
        if (actionName == null || actionName.isEmpty()) {
            if (callback != null) {
                callback.onError("动作名称不能为空");
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                SequenceData data = loadSequence(actionName);
                if (callback != null) {
                    callback.onSuccess(data);
                }
            } catch (Exception e) {
                Log.e(TAG, "加载动作序列失败: " + actionName, e);
                if (callback != null) {
                    callback.onError("加载失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 同步加载动作序列
     */
    public SequenceData loadSequence(String actionName) throws IOException {
        Log.d(TAG, "开始加载动作序列: " + actionName);
        
        // 1. 检查本地缓存
        if (config.isEnableCache() && cacheManager.isCached(actionName)) {
            Log.d(TAG, "从缓存加载动作: " + actionName);
            File cachedFile = cacheManager.getCachedFile(actionName);
            if (cachedFile != null && cachedFile.exists()) {
                try (FileInputStream fis = new FileInputStream(cachedFile)) {
                    return sequenceLoader.parseEbsFile(fis);
                }
            }
        }
        
        // 2. 从网络下载
        Log.d(TAG, "从网络下载动作: " + actionName);
        return downloadAndCacheSequence(actionName);
    }
    
    /**
     * 从网络下载并缓存动作序列
     */
    private SequenceData downloadAndCacheSequence(String actionName) throws IOException {
        // 首先获取动作列表，找到对应的序列ID，同时建立映射关系
        String sequenceListJson = client.getSequenceList(null, 100, 0);
        
        // 解析动作列表并建立映射关系
        buildMappingsFromSequenceList(sequenceListJson);
        
        int sequenceId = findSequenceIdByName(sequenceListJson, actionName);
        
        if (sequenceId <= 0) {
            throw new IOException("未找到动作序列: " + actionName);
        }
        
        // 下载动作文件
        byte[] sequenceData = client.downloadSequence(sequenceId);
        
        // 缓存到本地
        if (config.isEnableCache()) {
            boolean cached = cacheManager.cacheAction(actionName, sequenceData, null);
            Log.d(TAG, String.format("动作缓存结果: %s -> %s", actionName, cached));
        }
        
        // 解析并返回
        return sequenceLoader.parseEbsFile(new java.io.ByteArrayInputStream(sequenceData));
    }
    
    /**
     * 异步检查动作库更新
     */
    public void checkUpdatesAsync(String currentVersion, UpdateCheckCallback callback) {
        client.checkUpdatesAsync(currentVersion, new ActionLibraryClient.UpdateCheckCallback() {
            @Override
            public void onSuccess(ActionLibraryClient.UpdateCheckResult result) {
                Log.d(TAG, "更新检查完成: " + result.toString());
                if (callback != null) {
                    callback.onUpdateCheckComplete(result.hasUpdates, result.updateCount, result.libraryVersion);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "更新检查失败: " + error);
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * 获取动作序列列表
     */
    public String getSequenceList(String category, int limit, int offset) throws IOException {
        return client.getSequenceList(category, limit, offset);
    }

    /**
     * 异步获取动作列表
     */
    public void getActionListAsync(String category, ActionListCallback callback) {
        client.getSequenceListAsync(category, 100, 0, new ActionLibraryClient.SequenceListCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                if (callback != null) {
                    callback.onSuccess(jsonResponse);
                }
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * 预加载常用动作到缓存
     */
    public void preloadCommonActionsAsync(String[] actionNames, PreloadCallback callback) {
        if (actionNames == null || actionNames.length == 0) {
            if (callback != null) {
                callback.onComplete(0, 0);
            }
            return;
        }
        
        executor.execute(() -> {
            int successCount = 0;
            int totalCount = actionNames.length;
            
            for (String actionName : actionNames) {
                try {
                    loadSequence(actionName);
                    successCount++;
                    Log.d(TAG, String.format("预加载成功: %s (%d/%d)", actionName, successCount, totalCount));
                } catch (Exception e) {
                    Log.w(TAG, "预加载失败: " + actionName, e);
                }
            }
            
            if (callback != null) {
                callback.onComplete(successCount, totalCount);
            }
        });
    }
    
    /**
     * 获取缓存统计信息
     */
    public ActionCacheManager.CacheStats getCacheStats() {
        return cacheManager.getCacheStats();
    }
    
    /**
     * 清空缓存
     */
    public void clearCache() {
        cacheManager.clearCache();
        Log.d(TAG, "缓存已清空");
    }
    
    /**
     * 从JSON响应中查找序列ID
     * 支持中文名称和英文名称查找
     * 简化实现，实际使用时应该使用JSON解析库
     */
    private int findSequenceIdByName(String jsonResponse, String actionName) {
        try {
            // 简单的字符串匹配查找序列ID
            String[] lines = jsonResponse.split("\n");
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                
                // 找到包含ID的行
                if (line.contains("\"id\":")) {
                    // 提取ID
                    String[] parts = line.split("\"id\":\\s*");
                    if (parts.length > 1) {
                        String idStr = parts[1].split("[,}]")[0].trim();
                        int id = Integer.parseInt(idStr);
                        
                        // 在接下来的几行中查找匹配的名称
                        for (int j = i + 1; j < Math.min(i + 10, lines.length); j++) {
                            String nextLine = lines[j].trim();
                            
                            // 如果遇到下一个对象的开始，停止搜索
                            if (nextLine.equals("{") || nextLine.contains("\"id\":")) {
                                break;
                            }
                            
                            // 优先匹配英文名称（考虑空格）
                            if (nextLine.contains("\"english_name\"") && nextLine.contains("\"" + actionName + "\"")) {
                                return id;
                            }
                            
                            // 兼容中文名称匹配（考虑空格，排除english_name字段）
                            if (nextLine.contains("\"name\"") && nextLine.contains("\"" + actionName + "\"") && 
                                !nextLine.contains("english_name")) {
                                return id;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析序列ID失败", e);
        }
        
        return -1;
    }
    
    /**
     * 从动作列表JSON中解析并建立映射关系
     * 简化实现，实际使用时应该使用JSON解析库
     */
    public void buildMappingsFromSequenceList(String jsonResponse) {
        try {
            String[] lines = jsonResponse.split("\n");
            String currentName = null;
            String currentEnglishName = null;
            
            for (String line : lines) {
                if (line.contains("\"name\":\"")) {
                    // 提取中文名称
                    String[] parts = line.split("\"name\":\"");
                    if (parts.length > 1) {
                        currentName = parts[1].split("\"")[0];
                    }
                } else if (line.contains("\"english_name\":\"")) {
                    // 提取英文名称
                    String[] parts = line.split("\"english_name\":\"");
                    if (parts.length > 1) {
                        currentEnglishName = parts[1].split("\"")[0];
                    }
                }
                
                // 如果两个名称都获取到了，建立映射
                if (currentName != null && currentEnglishName != null) {
                    ActionNameUtils.addMapping(currentName, currentEnglishName);
                    currentName = null;
                    currentEnglishName = null;
                }
            }
            
            Log.d(TAG, "从动作列表建立映射完成，当前映射数量: " + ActionNameUtils.getMappingCount());
            
        } catch (Exception e) {
            Log.w(TAG, "从动作列表建立映射失败", e);
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放动作库管理器资源");
        
        if (client != null) {
            client.release();
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    
    // 回调接口
    public interface LoadSequenceCallback {
        void onSuccess(SequenceData data);
        void onError(String error);
    }
    
    public interface UpdateCheckCallback {
        void onUpdateCheckComplete(boolean hasUpdates, int updateCount, String details);
        void onError(String error);
    }
    
    public interface ActionListCallback {
        void onSuccess(String jsonResponse);
        void onError(String error);
    }
    
    public interface PreloadCallback {
        void onComplete(int successCount, int totalCount);
    }
}