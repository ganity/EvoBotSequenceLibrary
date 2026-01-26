package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * 动作缓存管理器
 * 负责本地动作文件的缓存、验证和管理
 */
public class ActionCacheManager {
    
    private static final String TAG = "ActionCacheManager";
    
    private final Context context;
    private final File cacheDir;
    private final Map<String, CacheEntry> cacheIndex;
    
    /**
     * 构造函数
     */
    public ActionCacheManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context不能为null");
        }
        
        this.context = context.getApplicationContext();
        this.cacheDir = new File(this.context.getCacheDir(), ActionLibraryConfig.CACHE_DIR_NAME);
        this.cacheIndex = new HashMap<>();
        
        // 确保缓存目录存在
        if (!cacheDir.exists()) {
            boolean created = cacheDir.mkdirs();
            Log.d(TAG, "创建缓存目录: " + cacheDir.getAbsolutePath() + ", 结果: " + created);
        }
        
        // 加载缓存索引
        loadCacheIndex();
        
        Log.d(TAG, String.format("缓存管理器初始化完成，缓存目录: %s，已缓存文件: %d", 
            cacheDir.getAbsolutePath(), cacheIndex.size()));
    }
    
    /**
     * 检查动作是否已缓存
     */
    public boolean isCached(String actionName) {
        return isCached(actionName, null);
    }
    
    /**
     * 检查动作是否已缓存（带哈希验证）
     */
    public boolean isCached(String actionName, String expectedHash) {
        if (actionName == null || actionName.isEmpty()) {
            return false;
        }
        
        CacheEntry entry = cacheIndex.get(actionName);
        if (entry == null) {
            return false;
        }
        
        File file = new File(cacheDir, entry.fileName);
        if (!file.exists()) {
            // 文件不存在，从索引中移除
            cacheIndex.remove(actionName);
            return false;
        }
        
        // 如果提供了期望的哈希值，进行验证
        if (expectedHash != null && !expectedHash.isEmpty()) {
            try {
                String actualHash = calculateFileHash(file);
                if (!expectedHash.equals(actualHash)) {
                    Log.w(TAG, String.format("缓存文件哈希不匹配: %s, 期望: %s, 实际: %s", 
                        actionName, expectedHash, actualHash));
                    return false;
                }
            } catch (IOException e) {
                Log.e(TAG, "计算文件哈希失败: " + actionName, e);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取缓存的动作文件
     */
    public File getCachedFile(String actionName) {
        if (!isCached(actionName)) {
            return null;
        }
        
        CacheEntry entry = cacheIndex.get(actionName);
        return new File(cacheDir, entry.fileName);
    }
    
    /**
     * 缓存动作文件
     */
    public boolean cacheAction(String actionName, byte[] data, String fileHash) {
        if (actionName == null || actionName.isEmpty() || data == null) {
            Log.w(TAG, "缓存参数无效");
            return false;
        }
        
        try {
            // 生成文件名
            String fileName = generateFileName(actionName);
            File file = new File(cacheDir, fileName);
            
            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
                fos.flush();
            }
            
            // 验证文件哈希（如果提供）
            if (fileHash != null && !fileHash.isEmpty()) {
                String actualHash = calculateFileHash(file);
                if (!fileHash.equals(actualHash)) {
                    Log.e(TAG, String.format("缓存文件哈希验证失败: %s, 期望: %s, 实际: %s", 
                        actionName, fileHash, actualHash));
                    file.delete();
                    return false;
                }
            }
            
            // 更新缓存索引
            CacheEntry entry = new CacheEntry(fileName, fileHash, System.currentTimeMillis(), data.length);
            cacheIndex.put(actionName, entry);
            
            Log.d(TAG, String.format("动作已缓存: %s -> %s (%d bytes)", actionName, fileName, data.length));
            
            // 检查缓存大小限制
            checkCacheSize();
            
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "缓存动作失败: " + actionName, e);
            return false;
        }
    }
    
    /**
     * 删除缓存的动作
     */
    public boolean removeAction(String actionName) {
        CacheEntry entry = cacheIndex.remove(actionName);
        if (entry == null) {
            return false;
        }
        
        File file = new File(cacheDir, entry.fileName);
        boolean deleted = file.delete();
        
        Log.d(TAG, String.format("删除缓存动作: %s, 结果: %s", actionName, deleted));
        return deleted;
    }
    
    /**
     * 清空所有缓存
     */
    public void clearCache() {
        Log.d(TAG, "清空所有缓存");
        
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    boolean deleted = file.delete();
                    Log.v(TAG, String.format("删除缓存文件: %s, 结果: %s", file.getName(), deleted));
                }
            }
        }
        
        cacheIndex.clear();
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        long totalSize = 0;
        int fileCount = 0;
        
        for (CacheEntry entry : cacheIndex.values()) {
            totalSize += entry.fileSize;
            fileCount++;
        }
        
        return new CacheStats(fileCount, totalSize, ActionLibraryConfig.CACHE_MAX_SIZE);
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName(String actionName) {
        // 使用时间戳和动作名生成唯一文件名
        long timestamp = System.currentTimeMillis();
        String safeName = actionName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return String.format("%d_%s.ebs", timestamp, safeName);
    }
    
    /**
     * 计算文件MD5哈希
     */
    private String calculateFileHash(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            
            byte[] hash = md.digest();
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            
            return result.toString();
        } catch (Exception e) {
            throw new IOException("计算文件哈希失败", e);
        }
    }
    
    /**
     * 加载缓存索引
     */
    private void loadCacheIndex() {
        // 简化实现：扫描缓存目录重建索引
        File[] files = cacheDir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".ebs")) {
                try {
                    String fileName = file.getName();
                    String actionName = extractActionNameFromFileName(fileName);
                    String fileHash = calculateFileHash(file);
                    long fileSize = file.length();
                    long cacheTime = file.lastModified();
                    
                    CacheEntry entry = new CacheEntry(fileName, fileHash, cacheTime, fileSize);
                    cacheIndex.put(actionName, entry);
                    
                    Log.v(TAG, String.format("加载缓存条目: %s -> %s", actionName, fileName));
                    
                } catch (Exception e) {
                    Log.w(TAG, "加载缓存文件失败: " + file.getName(), e);
                }
            }
        }
    }
    
    /**
     * 从文件名提取动作名
     */
    private String extractActionNameFromFileName(String fileName) {
        // 从格式 "timestamp_actionName.ebs" 中提取动作名
        if (fileName.endsWith(".ebs")) {
            String nameWithoutExt = fileName.substring(0, fileName.length() - 4);
            int underscoreIndex = nameWithoutExt.indexOf('_');
            if (underscoreIndex > 0 && underscoreIndex < nameWithoutExt.length() - 1) {
                return nameWithoutExt.substring(underscoreIndex + 1);
            }
        }
        return fileName;
    }
    
    /**
     * 检查缓存大小限制
     */
    private void checkCacheSize() {
        CacheStats stats = getCacheStats();
        if (stats.totalSize > ActionLibraryConfig.CACHE_MAX_SIZE) {
            Log.w(TAG, String.format("缓存超出限制: %d/%d bytes，开始清理", 
                stats.totalSize, ActionLibraryConfig.CACHE_MAX_SIZE));
            
            // 简单的LRU清理：删除最旧的文件
            cleanupOldestFiles();
        }
    }
    
    /**
     * 清理最旧的文件
     */
    private void cleanupOldestFiles() {
        // 找到最旧的缓存条目并删除，直到缓存大小在限制内
        while (getCacheStats().totalSize > ActionLibraryConfig.CACHE_MAX_SIZE * 0.8) {
            String oldestAction = null;
            long oldestTime = Long.MAX_VALUE;
            
            for (Map.Entry<String, CacheEntry> entry : cacheIndex.entrySet()) {
                if (entry.getValue().cacheTime < oldestTime) {
                    oldestTime = entry.getValue().cacheTime;
                    oldestAction = entry.getKey();
                }
            }
            
            if (oldestAction != null) {
                removeAction(oldestAction);
                Log.d(TAG, "清理旧缓存: " + oldestAction);
            } else {
                break;
            }
        }
    }
    
    // 缓存条目类
    private static class CacheEntry {
        final String fileName;
        final String fileHash;
        final long cacheTime;
        final long fileSize;
        
        CacheEntry(String fileName, String fileHash, long cacheTime, long fileSize) {
            this.fileName = fileName;
            this.fileHash = fileHash;
            this.cacheTime = cacheTime;
            this.fileSize = fileSize;
        }
    }
    
    // 缓存统计类
    public static class CacheStats {
        public final int fileCount;
        public final long totalSize;
        public final long maxSize;
        
        CacheStats(int fileCount, long totalSize, long maxSize) {
            this.fileCount = fileCount;
            this.totalSize = totalSize;
            this.maxSize = maxSize;
        }
        
        public double getUsagePercentage() {
            return maxSize > 0 ? (double) totalSize / maxSize * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{files=%d, size=%d/%d bytes (%.1f%%)}", 
                fileCount, totalSize, maxSize, getUsagePercentage());
        }
    }
}