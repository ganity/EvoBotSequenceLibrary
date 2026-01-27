package com.evobot.sequence;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 动作库更新器
 * 负责检查动作库更新并批量下载到本地存储
 */
public class ActionLibraryUpdater {
    
    private static final String TAG = "ActionLibraryUpdater";
    
    // SharedPreferences键名
    private static final String PREFS_NAME = "action_library_prefs";
    private static final String KEY_CURRENT_VERSION = "current_version";
    private static final String KEY_LAST_CHECK_TIME = "last_check_time";
    private static final String KEY_LOCAL_SEQUENCES = "local_sequences";
    private static final String KEY_FIRST_LAUNCH_CHECKED = "first_launch_checked";
    
    // 更新检查间隔（默认24小时）
    private static final long UPDATE_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L;
    
    // 网络超时配置
    private static final int FIRST_LAUNCH_TIMEOUT_MS = 15000;  // 首次启动检查超时15秒
    private static final int MANUAL_CHECK_TIMEOUT_MS = 30000;  // 手动检查超时30秒
    private static final int DEFAULT_TIMEOUT_MS = 30000;       // 默认超时30秒
    private static final int MAX_RETRY_ATTEMPTS = 2;           // 最大重试次数
    private static final int DEFAULT_MAX_RETRIES = 2;          // 默认最大重试次数
    
    // 存储位置选项
    public enum StorageLocation {
        INTERNAL_FILES,    // 内部存储 - 应用卸载时删除
        EXTERNAL_FILES,    // 外部应用专用目录 - 应用卸载时删除，但用户可访问
        EXTERNAL_PUBLIC    // 外部公共目录 - 应用卸载后保留，需要权限
    }
    
    private final Context context;
    private final ActionLibraryConfig config;
    private final ActionLibraryClient client;
    private final SharedPreferences prefs;
    private final ExecutorService executor;
    private final File localActionDir;
    private final StorageLocation storageLocation;
    
    /**
     * 构造函数（使用默认内部存储）
     */
    public ActionLibraryUpdater(Context context, ActionLibraryConfig config) {
        this(context, config, StorageLocation.INTERNAL_FILES);
    }
    
    /**
     * 构造函数（指定存储位置）
     */
    public ActionLibraryUpdater(Context context, ActionLibraryConfig config, StorageLocation storageLocation) {
        if (context == null) {
            throw new IllegalArgumentException("Context不能为null");
        }
        if (config == null) {
            throw new IllegalArgumentException("ActionLibraryConfig不能为null");
        }
        
        this.context = context.getApplicationContext();
        this.config = config;
        this.client = new ActionLibraryClient(config);
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
        this.storageLocation = storageLocation;
        
        // 根据存储位置创建本地动作存储目录
        this.localActionDir = createStorageDirectory(storageLocation);
        if (!localActionDir.exists()) {
            boolean created = localActionDir.mkdirs();
            Log.d(TAG, "创建本地动作目录: " + localActionDir.getAbsolutePath() + ", 结果: " + created);
        }
        
        Log.d(TAG, String.format("动作库更新器初始化完成，存储位置: %s (%s)", 
            storageLocation, localActionDir.getAbsolutePath()));
    }
    
    /**
     * 根据存储位置创建目录
     */
    private File createStorageDirectory(StorageLocation location) {
        switch (location) {
            case INTERNAL_FILES:
                // 内部存储：/data/data/package/files/downloaded_actions
                // 特点：应用卸载时删除，其他应用无法访问，无需权限
                return new File(context.getFilesDir(), "downloaded_actions");
                
            case EXTERNAL_FILES:
                // 外部应用专用目录：/Android/data/package/files/downloaded_actions  
                // 特点：应用卸载时删除，用户可访问，无需权限（API 19+）
                File externalFilesDir = context.getExternalFilesDir(null);
                if (externalFilesDir != null) {
                    return new File(externalFilesDir, "downloaded_actions");
                } else {
                    Log.w(TAG, "外部存储不可用，回退到内部存储");
                    return new File(context.getFilesDir(), "downloaded_actions");
                }
                
            case EXTERNAL_PUBLIC:
                // 外部公共目录：/EvoBot/Actions
                // 特点：应用卸载后保留，需要存储权限
                File publicDir = new File(android.os.Environment.getExternalStorageDirectory(), "EvoBot/Actions");
                if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                    return publicDir;
                } else {
                    Log.w(TAG, "外部存储不可用，回退到内部存储");
                    return new File(context.getFilesDir(), "downloaded_actions");
                }
                
            default:
                return new File(context.getFilesDir(), "downloaded_actions");
        }
    }
    
    /**
     * 获取存储位置信息
     */
    public String getStorageInfo() {
        StringBuilder info = new StringBuilder();
        info.append("存储位置: ").append(storageLocation).append("\n");
        info.append("存储路径: ").append(localActionDir.getAbsolutePath()).append("\n");
        info.append("目录存在: ").append(localActionDir.exists()).append("\n");
        
        if (localActionDir.exists()) {
            File[] files = localActionDir.listFiles();
            int fileCount = files != null ? files.length : 0;
            long totalSize = 0;
            
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        totalSize += file.length();
                    }
                }
            }
            
            info.append("文件数量: ").append(fileCount).append("\n");
            info.append("总大小: ").append(totalSize).append(" bytes\n");
        }
        
        return info.toString();
    }
    
    /**
     * 检查是否需要更新（基于时间间隔）
     */
    public boolean shouldCheckForUpdates() {
        long lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastCheckTime) > UPDATE_CHECK_INTERVAL_MS;
    }
    
    /**
     * 检查是否是首次启动
     */
    public boolean isFirstLaunch() {
        return !prefs.getBoolean(KEY_FIRST_LAUNCH_CHECKED, false);
    }
    
    /**
     * 标记首次启动检查已完成
     */
    private void markFirstLaunchChecked() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_CHECKED, true).apply();
    }
    
    /**
     * 首次启动时检查更新（异步，不阻塞启动）
     * 网络异常不会影响应用启动
     */
    public void checkForUpdatesOnFirstLaunch(UpdateCallback callback) {
        if (!isFirstLaunch()) {
            Log.d(TAG, "非首次启动，跳过首次检查");
            if (callback != null) {
                callback.onNoUpdateNeeded();
            }
            return;
        }
        
        Log.d(TAG, "首次启动，开始检查动作库更新");
        
        // 使用单独的线程，设置较短的超时时间
        executor.execute(() -> {
            try {
                // 标记首次启动检查已开始（避免重复检查）
                markFirstLaunchChecked();
                
                String currentVersion = getCurrentVersion();
                List<LocalSequenceInfo> localSequences = getLocalSequences();
                
                Log.d(TAG, String.format("首次启动检查: 当前版本=%s, 本地动作数=%d", 
                    currentVersion, localSequences.size()));
                
                // 使用较短的超时时间进行首次检查
                performUpdateCheckWithTimeout(currentVersion, localSequences, callback, 
                    FIRST_LAUNCH_TIMEOUT_MS, 1); // 首次启动只重试1次
                
            } catch (Exception e) {
                Log.w(TAG, "首次启动检查失败，不影响应用启动: " + e.getMessage());
                if (callback != null) {
                    callback.onError("首次启动检查失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 手动触发检查更新（用户主动触发）
     */
    public void manualCheckForUpdates(UpdateCallback callback) {
        Log.d(TAG, "用户手动触发更新检查");
        
        executor.execute(() -> {
            try {
                String currentVersion = getCurrentVersion();
                List<LocalSequenceInfo> localSequences = getLocalSequences();
                
                Log.d(TAG, String.format("手动检查更新: 当前版本=%s, 本地动作数=%d", 
                    currentVersion, localSequences.size()));
                
                // 手动检查使用较长的超时时间和更多重试
                performUpdateCheckWithTimeout(currentVersion, localSequences, callback, 
                    MANUAL_CHECK_TIMEOUT_MS, MAX_RETRY_ATTEMPTS);
                
            } catch (Exception e) {
                Log.e(TAG, "手动检查更新失败", e);
                if (callback != null) {
                    callback.onError("手动检查更新失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 异步检查并下载更新
     */
    public void checkAndDownloadUpdatesAsync(UpdateCallback callback) {
        executor.execute(() -> {
            try {
                checkAndDownloadUpdates(callback);
            } catch (Exception e) {
                Log.e(TAG, "检查更新失败", e);
                if (callback != null) {
                    callback.onError("检查更新失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 强制检查并下载更新（忽略时间间隔）
     */
    public void forceCheckAndDownloadUpdatesAsync(UpdateCallback callback) {
        executor.execute(() -> {
            try {
                String currentVersion = getCurrentVersion();
                List<LocalSequenceInfo> localSequences = getLocalSequences();
                
                Log.d(TAG, String.format("强制检查更新: 当前版本=%s, 本地动作数=%d", 
                    currentVersion, localSequences.size()));
                
                performUpdateCheck(currentVersion, localSequences, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "强制检查更新失败", e);
                if (callback != null) {
                    callback.onError("强制检查更新失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 同步检查并下载更新
     */
    private void checkAndDownloadUpdates(UpdateCallback callback) {
        // 检查是否需要更新
        if (!shouldCheckForUpdates()) {
            Log.d(TAG, "距离上次检查时间不足，跳过更新检查");
            if (callback != null) {
                callback.onNoUpdateNeeded();
            }
            return;
        }
        
        String currentVersion = getCurrentVersion();
        List<LocalSequenceInfo> localSequences = getLocalSequences();
        
        Log.d(TAG, String.format("开始检查更新: 当前版本=%s, 本地动作数=%d", 
            currentVersion, localSequences.size()));
        
        performUpdateCheck(currentVersion, localSequences, callback);
    }
    
    /**
     * 执行更新检查（简化版本）
     */
    private void performUpdateCheck(String currentVersion, List<LocalSequenceInfo> localSequences, UpdateCallback callback) {
        try {
            performUpdateCheckWithTimeout(currentVersion, localSequences, callback, DEFAULT_TIMEOUT_MS, DEFAULT_MAX_RETRIES);
        } catch (IOException e) {
            Log.e(TAG, "更新检查失败", e);
            if (callback != null) {
                callback.onError("更新检查失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 执行更新检查（带超时和重试）
     */
    private void performUpdateCheckWithTimeout(String currentVersion, List<LocalSequenceInfo> localSequences, 
                                             UpdateCallback callback, int timeoutMs, int maxRetries) throws IOException {
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Log.d(TAG, String.format("更新检查尝试 %d/%d，超时: %dms", attempt, maxRetries, timeoutMs));
                
                // 1. 调用更新检查API（带超时）
                UpdateCheckResult checkResult = client.checkUpdatesWithTimeout(currentVersion, localSequences, timeoutMs);
                
                // 更新最后检查时间
                prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply();
                
                if (!checkResult.hasUpdates) {
                    Log.d(TAG, "没有可用更新");
                    if (callback != null) {
                        callback.onNoUpdatesAvailable();
                    }
                    return;
                }
                
                Log.d(TAG, String.format("发现 %d 个更新，总大小: %d bytes", 
                    checkResult.updateCount, checkResult.totalSize));
                
                if (callback != null) {
                    callback.onUpdatesFound(checkResult.updateCount, checkResult.totalSize);
                }
                
                // 2. 批量下载更新（带超时）
                if (checkResult.updateIds != null && !checkResult.updateIds.isEmpty()) {
                    downloadUpdatesWithTimeout(checkResult.updateIds, checkResult.libraryVersion, callback, timeoutMs);
                }
                
                return; // 成功完成，退出重试循环
                
            } catch (Exception e) {
                lastException = e;
                Log.w(TAG, String.format("更新检查尝试 %d/%d 失败: %s", attempt, maxRetries, e.getMessage()));
                
                if (attempt < maxRetries) {
                    try {
                        // 指数退避重试
                        long delay = 1000L * attempt;
                        Log.d(TAG, String.format("等待 %dms 后重试", delay));
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("更新检查被中断", ie);
                    }
                }
            }
        }
        
        // 所有重试都失败了
        throw new IOException("更新检查失败，已重试 " + maxRetries + " 次", lastException);
    }
    
    /**
     * 批量下载更新（带超时）
     */
    private void downloadUpdatesWithTimeout(List<Integer> sequenceIds, String newVersion, 
                                          UpdateCallback callback, int timeoutMs) {
        try {
            Log.d(TAG, "开始批量下载 " + sequenceIds.size() + " 个动作更新");
            
            if (callback != null) {
                callback.onDownloadStarted(sequenceIds.size());
            }
            
            // 调用批量下载API（带超时）
            byte[] zipData = client.batchDownloadWithTimeout(sequenceIds, timeoutMs);
            
            // 解压并保存到本地
            int savedCount = extractAndSaveActions(zipData, newVersion);
            
            // 更新本地版本信息
            updateLocalVersion(newVersion);
            
            Log.d(TAG, String.format("更新下载完成: 成功保存 %d 个动作", savedCount));
            
            if (callback != null) {
                callback.onDownloadCompleted(savedCount, newVersion);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "批量下载失败", e);
            if (callback != null) {
                callback.onError("下载失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 解压并保存动作文件
     */
    private int extractAndSaveActions(byte[] zipData, String version) throws IOException {
        // 简化实现：假设服务器返回的是单个动作文件而不是ZIP
        // 实际实现中需要使用ZipInputStream解压
        
        int savedCount = 0;
        
        // 这里应该解压ZIP文件，遍历每个.ebs文件
        // 为了简化，我们假设只有一个文件
        String fileName = "downloaded_action_" + System.currentTimeMillis() + ".ebs";
        File actionFile = new File(localActionDir, fileName);
        
        try (FileOutputStream fos = new FileOutputStream(actionFile)) {
            fos.write(zipData);
            savedCount = 1;
        }
        
        Log.d(TAG, "保存动作文件: " + actionFile.getAbsolutePath());
        
        // 下载完成后，解析文件并建立映射关系
        buildMappingForDownloadedFile(actionFile);
        
        // 更新本地动作索引
        updateLocalSequenceIndex(fileName, version);
        
        return savedCount;
    }
    
    /**
     * 更新本地版本信息
     */
    private void updateLocalVersion(String newVersion) {
        prefs.edit().putString(KEY_CURRENT_VERSION, newVersion).apply();
        Log.d(TAG, "更新本地版本: " + newVersion);
    }
    
    /**
     * 更新本地动作索引
     */
    private void updateLocalSequenceIndex(String fileName, String version) {
        // 简化实现：将文件名保存到SharedPreferences
        // 实际实现中应该维护一个完整的动作索引数据库
        String existingSequences = prefs.getString(KEY_LOCAL_SEQUENCES, "");
        String updatedSequences = existingSequences + fileName + ":" + version + ";";
        prefs.edit().putString(KEY_LOCAL_SEQUENCES, updatedSequences).apply();
    }
    
    /**
     * 获取当前版本
     */
    public String getCurrentVersion() {
        return prefs.getString(KEY_CURRENT_VERSION, "1.0.0");
    }
    
    /**
     * 获取本地动作序列信息
     */
    private List<LocalSequenceInfo> getLocalSequences() {
        List<LocalSequenceInfo> sequences = new ArrayList<>();
        
        // 扫描本地动作目录
        File[] files = localActionDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".ebs")) {
                    LocalSequenceInfo info = new LocalSequenceInfo();
                    info.fileName = file.getName();
                    info.filePath = file.getAbsolutePath();
                    info.fileSize = file.length();
                    info.lastModified = file.lastModified();
                    // 这里应该解析文件获取更多信息，简化处理
                    info.name = extractActionNameFromFileName(file.getName());
                    info.version = "1.0.0"; // 简化处理
                    sequences.add(info);
                }
            }
        }
        
        return sequences;
    }
    
    /**
     * 从文件名提取动作名
     */
    private String extractActionNameFromFileName(String fileName) {
        return ActionNameUtils.extractActionNameFromFileName(fileName);
    }
    
    /**
     * 获取本地动作文件
     * 支持中文名称和英文名称查找，使用动态映射
     */
    public File getLocalActionFile(String actionName) {
        File[] files = localActionDir.listFiles();
        if (files != null) {
            // 使用工具类进行智能文件匹配
            return ActionNameUtils.findMatchingFile(actionName, files);
        }
        return null;
    }
    
    /**
     * 获取所有本地动作文件
     */
    public List<File> getAllLocalActionFiles() {
        List<File> actionFiles = new ArrayList<>();
        File[] files = localActionDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".ebs")) {
                    actionFiles.add(file);
                }
            }
        }
        return actionFiles;
    }
    
    /**
     * 清理本地动作文件
     */
    public void clearLocalActions() {
        File[] files = localActionDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    boolean deleted = file.delete();
                    Log.d(TAG, "删除本地动作文件: " + file.getName() + ", 结果: " + deleted);
                }
            }
        }
        
        // 清理SharedPreferences
        prefs.edit()
            .remove(KEY_LOCAL_SEQUENCES)
            .remove(KEY_CURRENT_VERSION)
            .apply();
        
        Log.d(TAG, "本地动作文件已清理");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (client != null) {
            client.release();
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        
        Log.d(TAG, "动作库更新器资源已释放");
    }
    
    // 内部类：更新检查结果
    public static class UpdateCheckResult {
        public final boolean hasUpdates;
        public final int updateCount;
        public final long totalSize;
        public final String libraryVersion;
        public final List<Integer> updateIds;
        
        public UpdateCheckResult(boolean hasUpdates, int updateCount, long totalSize, 
                               String libraryVersion, List<Integer> updateIds) {
            this.hasUpdates = hasUpdates;
            this.updateCount = updateCount;
            this.totalSize = totalSize;
            this.libraryVersion = libraryVersion;
            this.updateIds = updateIds;
        }
    }
    
    // 内部类：本地动作序列信息
    public static class LocalSequenceInfo {
        public String name;
        public String fileName;
        public String filePath;
        public String version;
        public String fileHash;
        public long fileSize;
        public long lastModified;
    }
    
    /**
     * 为下载的文件建立中英文名称映射关系
     */
    private void buildMappingForDownloadedFile(File actionFile) {
        try {
            // 解析文件获取中文名称
            SequenceLoader loader = new SequenceLoader(context);
            try (java.io.FileInputStream fis = new java.io.FileInputStream(actionFile)) {
                SequenceData sequenceData = loader.parseEbsFile(fis);
                
                // 建立映射关系
                ActionNameUtils.addMappingFromFile(actionFile.getName(), sequenceData);
                
                Log.d(TAG, String.format("为文件建立映射: %s -> %s", 
                    actionFile.getName(), sequenceData.name));
            }
        } catch (Exception e) {
            Log.w(TAG, "建立文件映射失败: " + actionFile.getName(), e);
        }
    }
    
    /**
     * 初始化时扫描本地文件并建立映射关系
     */
    public void initializeMappings() {
        Log.d(TAG, "开始初始化动作名称映射...");
        
        File[] files = localActionDir.listFiles();
        if (files != null) {
            int mappingCount = 0;
            for (File file : files) {
                if (file.getName().endsWith(".ebs")) {
                    buildMappingForDownloadedFile(file);
                    mappingCount++;
                }
            }
            Log.d(TAG, String.format("映射初始化完成: 处理了 %d 个文件，当前映射数量: %d", 
                mappingCount, ActionNameUtils.getMappingCount()));
        }
    }
    
    // 回调接口
    public interface UpdateCallback {
        void onNoUpdateNeeded();
        void onNoUpdatesAvailable();
        void onUpdatesFound(int updateCount, long totalSize);
        void onDownloadStarted(int fileCount);
        void onDownloadCompleted(int savedCount, String newVersion);
        void onError(String error);
    }
}