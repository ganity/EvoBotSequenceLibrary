package com.evobot.sequence;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * EvoBot序列播放器
 * 按照指定频率（如40Hz）播放序列数据并通过Listener回调
 * 支持本地assets和HTTP动作库两种加载方式
 */
public class EvoBotSequencePlayer {

    private static final String TAG = "EvoBotSequencePlayer";

    // 默认配置
    private static final int DEFAULT_FREQUENCY = 40;  // 默认40Hz
    private static final String ASSETS_PATH = "sequences/";
    private static final String DEFAULT_SEQUENCE_FILE = "左臂挥手右臂掐腰抱胸_20260116_142711.ebs";

    // 核心组件
    private final Context context;
    private final SequenceLoader loader;
    private final Handler handler;
    
    // 动作库更新器（可选）
    private ActionLibraryUpdater actionLibraryUpdater;

    // 播放状态
    private PlayerState state = PlayerState.IDLE;
    private SequenceData currentSequence;
    private SequenceListener listener;

    // 播放控制
    private int currentFrame = 0;
    private int targetFrequency = DEFAULT_FREQUENCY;
    private long intervalMs;           // 实际间隔（毫秒）
    private long lastFrameTime = 0;    // 上一帧的时间戳

    // -1值填充缓存：存储每个关节的最后一个非-1值
    private int[] lastValidLeftArm = new int[10];   // 左臂10个关节
    private int[] lastValidRightArm = new int[10];  // 右臂10个关节

    // 播放任务
    private Runnable playbackRunnable;
    
    // Native播放器实例ID
    private long nativePlayerId = -1;
    
    // 是否使用Native实现
    private boolean useNativePlayback = true;
    
    // Native方法声明
    static {
        try {
            System.loadLibrary("evobot_sequence_native");
            Log.i(TAG, "Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to load native library, falling back to Java implementation", e);
        }
    }
    
    // Native方法声明
    private static native long nativeCreate();
    private static native void nativeDestroy(long playerId);
    private static native boolean nativeRegisterListener(long playerId, SequenceListener listener);
    private static native void nativeUnregisterListener(long playerId);
    private static native boolean nativeLoadSequenceFromBytes(long playerId, byte[] data);
    private static native boolean nativePlayAsync(long playerId, int frequency);
    private static native void nativePause(long playerId);
    private static native void nativeResume(long playerId);
    private static native void nativeStop(long playerId);
    private static native void nativeEmergencyStop(long playerId);
    private static native boolean nativeSeek(long playerId, int frameIndex);
    private static native int nativeGetCurrentFrame(long playerId);
    private static native int nativeGetTotalFrames(long playerId);
    private static native void nativeClearCache();
    
    // RK3399专用方法
    private static native boolean nativeSetRK3399BigCores(long playerId, boolean useBigCores);
    private static native String nativeGetRK3399Stats(long playerId);
    private static native String nativeGetPerformanceStats();

    /**
     * 构造函数（仅支持本地assets）
     *
     * @param context Android上下文
     */
    public EvoBotSequencePlayer(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context不能为null");
        }
        this.context = context.getApplicationContext();
        this.loader = new SequenceLoader(this.context);
        this.handler = new Handler(Looper.getMainLooper());
        
        // 初始化Native播放器
        initializeNativePlayer();

        Log.d(TAG, "EvoBotSequencePlayer初始化完成（仅本地模式）");
    }
    
    /**
     * 构造函数（支持HTTP动作库更新，默认内部存储）
     *
     * @param context Android上下文
     * @param actionLibraryConfig 动作库配置
     */
    public EvoBotSequencePlayer(Context context, ActionLibraryConfig actionLibraryConfig) {
        this(context, actionLibraryConfig, ActionLibraryUpdater.StorageLocation.INTERNAL_FILES);
    }
    
    /**
     * 构造函数（支持HTTP动作库更新，指定存储位置）
     *
     * @param context Android上下文
     * @param actionLibraryConfig 动作库配置
     * @param storageLocation 存储位置
     */
    public EvoBotSequencePlayer(Context context, ActionLibraryConfig actionLibraryConfig, 
                               ActionLibraryUpdater.StorageLocation storageLocation) {
        this(context);
        
        if (actionLibraryConfig != null) {
            this.actionLibraryUpdater = new ActionLibraryUpdater(context, actionLibraryConfig, storageLocation);
            
            // 初始化动作名称映射关系
            initializeActionMappings();
            
            Log.d(TAG, "EvoBotSequencePlayer初始化完成（支持HTTP动作库更新）");
        }
    }
    
    /**
     * 初始化Native播放器
     */
    private void initializeNativePlayer() {
        try {
            nativePlayerId = nativeCreate();
            if (nativePlayerId > 0) {
                useNativePlayback = true;
                Log.i(TAG, "Native player initialized with ID: " + nativePlayerId);
            } else {
                useNativePlayback = false;
                Log.w(TAG, "Failed to create native player, using Java implementation");
            }
        } catch (UnsatisfiedLinkError e) {
            useNativePlayback = false;
            Log.w(TAG, "Native library not available, using Java implementation", e);
        }
    }
    
    /**
     * 设置是否使用RK3399大核心
     * 
     * @param useBigCores 是否使用大核心（A72）
     * @return 设置是否成功
     */
    public boolean setRK3399BigCores(boolean useBigCores) {
        if (useNativePlayback && nativePlayerId > 0) {
            return nativeSetRK3399BigCores(nativePlayerId, useBigCores);
        }
        return false;
    }
    
    /**
     * 获取RK3399性能统计
     * 
     * @return 性能统计字符串
     */
    public String getRK3399Stats() {
        if (useNativePlayback && nativePlayerId > 0) {
            return nativeGetRK3399Stats(nativePlayerId);
        }
        return "Native playback not available";
    }
    
    /**
     * 获取全局性能统计
     * 
     * @return 全局性能统计字符串
     */
    public static String getGlobalPerformanceStats() {
        try {
            return nativeGetPerformanceStats();
        } catch (UnsatisfiedLinkError e) {
            return "Native library not available";
        }
    }
    
    /**
     * 清空Native缓存
     */
    public static void clearNativeCache() {
        try {
            nativeClearCache();
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to clear native cache", e);
        }
    }

    /**
     * 播放序列（使用默认40Hz频率）
     *
     * @param actionName 动作名称（优先使用英文名称，如 arm_movement_left_arm_wave）
     * @param listener   回调监听器
     */
    public void play(String actionName, SequenceListener listener) {
        play(actionName, DEFAULT_FREQUENCY, listener);
    }

    /**
     * 播放序列（指定频率）
     *
     * @param actionName 动作名称（优先使用英文名称，如 arm_movement_left_arm_wave）
     * @param frequency  播放频率（Hz），推荐40Hz
     * @param listener   回调监听器
     */
    public void play(final String actionName, final int frequency, final SequenceListener listener) {
        if (actionName == null || actionName.isEmpty()) {
            throw new IllegalArgumentException("actionName不能为空");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener不能为null");
        }
        if (frequency <= 0 || frequency > 100) {
            throw new IllegalArgumentException("频率必须在1-100Hz之间，当前值: " + frequency);
        }

        // 如果正在播放，先停止
        if (state == PlayerState.PLAYING) {
            stop();
        }

        // Log.d(TAG, String.format("准备播放: action=%s, frequency=%dHz, native=%s", 
        //     actionName, frequency, useNativePlayback));

        this.listener = listener;
        this.targetFrequency = frequency;
        this.intervalMs = 1000L / frequency;

        // 在后台线程加载序列
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadSequenceAsync(actionName);
            }
        }, "SequenceLoader").start();
    }
    
    /**
     * 首次启动时检查动作库更新（异步，不阻塞启动）
     * 网络异常不会影响应用启动和使用
     *
     * @param callback 更新检查回调（可选）
     */
    public void checkForUpdatesOnFirstLaunch(ActionLibraryUpdater.UpdateCallback callback) {
        if (actionLibraryUpdater == null) {
            Log.d(TAG, "未配置动作库更新器，跳过首次启动检查");
            if (callback != null) {
                callback.onNoUpdateNeeded();
            }
            return;
        }
        
        actionLibraryUpdater.checkForUpdatesOnFirstLaunch(callback);
    }
    
    /**
     * 手动触发检查动作库更新
     * 用户主动触发，可以显示进度和结果
     *
     * @param callback 更新检查回调
     */
    public void manualCheckForUpdates(ActionLibraryUpdater.UpdateCallback callback) {
        if (actionLibraryUpdater == null) {
            if (callback != null) {
                callback.onError("未配置动作库更新器");
            }
            return;
        }
        
        actionLibraryUpdater.manualCheckForUpdates(callback);
    }
    
    /**
     * 检查动作库更新（定期检查，基于时间间隔）
     *
     * @param callback 更新检查回调
     */
    public void checkForUpdates(ActionLibraryUpdater.UpdateCallback callback) {
        if (actionLibraryUpdater == null) {
            if (callback != null) {
                callback.onError("未配置动作库更新器");
            }
            return;
        }
        
        actionLibraryUpdater.checkAndDownloadUpdatesAsync(callback);
    }
    
    /**
     * 检查是否是首次启动
     */
    public boolean isFirstLaunch() {
        if (actionLibraryUpdater == null) {
            return false;
        }
        return actionLibraryUpdater.isFirstLaunch();
    }
    
    /**
     * 强制检查动作库更新（忽略时间间隔）
     *
     * @param callback 更新检查回调
     */
    public void forceCheckForUpdates(ActionLibraryUpdater.UpdateCallback callback) {
        // 重定向到手动检查，提供更好的语义
        manualCheckForUpdates(callback);
    }
    
    /**
     * 获取当前动作库版本
     */
    public String getCurrentLibraryVersion() {
        if (actionLibraryUpdater == null) {
            return "1.0.0";
        }
        return actionLibraryUpdater.getCurrentVersion();
    }
    
    /**
     * 获取所有本地下载的动作文件
     */
    public List<File> getDownloadedActionFiles() {
        if (actionLibraryUpdater == null) {
            return new ArrayList<>();
        }
        return actionLibraryUpdater.getAllLocalActionFiles();
    }
    
    /**
     * 获取存储位置信息
     */
    public String getStorageInfo() {
        if (actionLibraryUpdater == null) {
            return "未配置动作库更新器";
        }
        return actionLibraryUpdater.getStorageInfo();
    }
    
    /**
     * 清理下载的动作文件
     */
    public void clearDownloadedActions() {
        if (actionLibraryUpdater != null) {
            actionLibraryUpdater.clearLocalActions();
        }
    }

    /**
     * 异步加载序列
     *
     * @param actionName 动作名称
     */
    private void loadSequenceAsync(String actionName) {
        setState(PlayerState.LOADING);

        try {
            // 优先尝试从下载的动作库加载
            if (actionLibraryUpdater != null) {
                Log.d(TAG, "尝试从下载的动作库加载: " + actionName);
                loadFromDownloadedActions(actionName);
            } else {
                // 回退到本地assets加载
                Log.d(TAG, "从本地assets加载: " + actionName);
                loadFromAssets(actionName);
            }

        } catch (Exception e) {
            Log.e(TAG, "加载序列失败", e);
            handleError("加载序列失败: " + e.getMessage());
        }
    }
    
    /**
     * 从下载的动作库加载序列
     */
    private void loadFromDownloadedActions(String actionName) {
        try {
            // 查找本地下载的动作文件
            File actionFile = actionLibraryUpdater.getLocalActionFile(actionName);
            
            if (actionFile != null && actionFile.exists()) {
                Log.d(TAG, "从下载的动作文件加载: " + actionFile.getAbsolutePath());
                try (java.io.FileInputStream fis = new java.io.FileInputStream(actionFile)) {
                    SequenceData data = loader.parseEbsFile(fis);
                    onSequenceLoaded(data);
                    return;
                }
            }
            
            // 如果没有找到下载的动作，回退到assets
            Log.d(TAG, "未找到下载的动作，回退到assets: " + actionName);
            loadFromAssets(actionName);
            
        } catch (Exception e) {
            Log.w(TAG, "从下载动作加载失败，回退到assets: " + e.getMessage());
            try {
                loadFromAssets(actionName);
            } catch (Exception e2) {
                Log.e(TAG, "assets加载也失败", e2);
                handleError("加载序列失败: " + e2.getMessage());
            }
        }
    }
    
    /**
     * 从本地assets加载序列
     */
    private void loadFromAssets(String actionName) throws Exception {
        // 构建文件路径
        String assetPath = ASSETS_PATH + DEFAULT_SEQUENCE_FILE;

        // 加载序列
        Log.d(TAG, "正在加载序列文件: " + assetPath);
        SequenceData data = loader.loadFromAssets(assetPath);
        
        onSequenceLoaded(data);
    }
    
    /**
     * 序列加载完成处理
     */
    private void onSequenceLoaded(SequenceData data) {
        currentSequence = data;
        
        // 重置播放状态
        currentFrame = 0;
        lastFrameTime = 0;
        
        // 重置-1值填充缓存
        resetLastValidValues();

        setState(PlayerState.READY);
        
        // 如果使用Native播放，加载序列到Native层
        if (useNativePlayback && nativePlayerId > 0) {
            try {
                // 将序列数据转换为字节数组（这里需要实现序列化）
                byte[] sequenceBytes = serializeSequenceData(data);
                boolean loaded = nativeLoadSequenceFromBytes(nativePlayerId, sequenceBytes);
                
                if (loaded) {
                    // 注册Native回调监听器
                    boolean registered = nativeRegisterListener(nativePlayerId, listener);
                    if (registered) {
                        Log.d(TAG, "Native sequence loaded and listener registered");
                        startNativePlayback();
                        return;
                    } else {
                        Log.w(TAG, "Failed to register native listener, falling back to Java");
                        useNativePlayback = false;
                    }
                } else {
                    Log.w(TAG, "Failed to load sequence to native, falling back to Java");
                    useNativePlayback = false;
                }
            } catch (Exception e) {
                Log.w(TAG, "Native playback failed, falling back to Java", e);
                useNativePlayback = false;
            }
        }
        
        // 回退到Java实现
        startPlayback();
    }
    
    /**
     * 启动Native播放
     */
    private void startNativePlayback() {
        if (!useNativePlayback || nativePlayerId <= 0) {
            Log.w(TAG, "Native playback not available");
            startPlayback();
            return;
        }
        
        setState(PlayerState.PLAYING);
        
        // RK3399优化：高频率播放使用大核心
        if (targetFrequency > 60) {
            setRK3399BigCores(true);
        }
        
        boolean started = nativePlayAsync(nativePlayerId, targetFrequency);
        if (!started) {
            Log.w(TAG, "Failed to start native playback, falling back to Java");
            useNativePlayback = false;
            startPlayback();
        } else {
            Log.d(TAG, "Native async playback started at " + targetFrequency + "Hz");
        }
    }
    
    /**
     * 序列数据序列化（简化实现）
     * 实际项目中应该使用更高效的序列化方式
     */
    private byte[] serializeSequenceData(SequenceData data) {
        // 这里需要实现将SequenceData转换为字节数组的逻辑
        // 为了简化，我们假设已经有了原始的.ebs文件数据
        // 实际实现中应该从SequenceLoader获取原始字节数据
        
        try {
            // 临时实现：重新读取原始文件
            String assetPath = ASSETS_PATH + DEFAULT_SEQUENCE_FILE;
            return loader.loadRawBytesFromAssets(assetPath);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize sequence data", e);
            return new byte[0];
        }
    }

    /**
     * 开始播放
     */
    private void startPlayback() {
        if (state != PlayerState.READY && state != PlayerState.PAUSED) {
            Log.w(TAG, "无法在当前状态开始播放: " + state);
            return;
        }

        setState(PlayerState.PLAYING);
        lastFrameTime = SystemClock.elapsedRealtime();

        Log.d(TAG, String.format("开始播放: 总帧数=%d, 频率=%dHz, 间隔=%dms",
            currentSequence.totalFrames, targetFrequency, intervalMs));

        // 立即播放第一帧
        playNextFrame();
    }

    /**
     * 播放下一帧（定时回调）
     */
    private void playNextFrame() {
        if (state != PlayerState.PLAYING) {
            Log.d(TAG, "播放已停止，忽略帧回调");
            return;
        }

        // 检查是否播放完成
        if (currentFrame >= currentSequence.totalFrames) {
            Log.d(TAG, "播放完成");
            setState(PlayerState.STOPPED);
            if (listener != null) {
                listener.onComplete();
            }
            return;
        }

        // 获取当前帧数据
        int[] leftArm = currentSequence.leftArmSequence[currentFrame];
        int[] rightArm = currentSequence.rightArmSequence[currentFrame];

        // 处理-1值填充：用上一帧的非-1值替换当前帧的-1值
        int[] processedLeftArm = fillMinusOneValues(leftArm, lastValidLeftArm);
        int[] processedRightArm = fillMinusOneValues(rightArm, lastValidRightArm);

        // 回调监听器
        if (listener != null) {
            try {
                listener.onFrameData(processedLeftArm, processedRightArm, currentFrame);
            } catch (Exception e) {
                Log.e(TAG, "监听器回调异常", e);
                handleError("监听器回调异常: " + e.getMessage());
                return;
            }
        }

        currentFrame++;

        // 计算下一帧的间隔时间（带误差补偿）
        long currentTime = SystemClock.elapsedRealtime();
        if (lastFrameTime > 0) {
            long actualInterval = currentTime - lastFrameTime;
            long drift = actualInterval - intervalMs;

            // 误差补偿：调整间隔，但限制在±50%范围内
            long adjustedInterval = intervalMs - drift;
            adjustedInterval = Math.max(intervalMs / 2, Math.min(intervalMs * 3 / 2, adjustedInterval));

            if (Math.abs(drift) > 5) {
                Log.v(TAG, String.format("时间补偿: 实际间隔=%dms, 期望=%dms, 补偿后=%dms",
                    actualInterval, intervalMs, adjustedInterval));
            }

            // 使用调整后的间隔
            handler.postDelayed(playbackRunnable, adjustedInterval);
        } else {
            // 首次执行，使用标准间隔
            handler.postDelayed(playbackRunnable, intervalMs);
        }

        lastFrameTime = currentTime;
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (!state.canPause()) {
            Log.w(TAG, "当前状态无法暂停: " + state);
            return;
        }

        setState(PlayerState.PAUSED);
        
        if (useNativePlayback && nativePlayerId > 0) {
            nativePause(nativePlayerId);
        } else {
            handler.removeCallbacks(playbackRunnable);
        }

        Log.d(TAG, String.format("播放已暂停，当前帧: %d/%d", getCurrentFrame(), getTotalFrames()));
    }

    /**
     * 恢复播放
     */
    public void resume() {
        if (state != PlayerState.PAUSED) {
            Log.w(TAG, "当前状态无法恢复: " + state);
            return;
        }

        Log.d(TAG, "恢复播放");
        
        if (useNativePlayback && nativePlayerId > 0) {
            nativeResume(nativePlayerId);
            setState(PlayerState.PLAYING);
        } else {
            startPlayback();
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        if (state == PlayerState.IDLE || state == PlayerState.STOPPED) {
            return;
        }

        setState(PlayerState.STOPPED);
        
        if (useNativePlayback && nativePlayerId > 0) {
            nativeStop(nativePlayerId);
        } else {
            handler.removeCallbacks(playbackRunnable);
        }
        
        currentFrame = 0;
        lastFrameTime = 0;
        
        // 重置-1值填充缓存
        resetLastValidValues();

        Log.d(TAG, "播放已停止");
    }

    /**
     * 急停方法
     * 立即停止序列播放并通知监听器停止位置输出
     */
    public void emergencyStop() {
        Log.w(TAG, "执行急停操作");
        
        if (useNativePlayback && nativePlayerId > 0) {
            nativeEmergencyStop(nativePlayerId);
        } else {
            // 立即停止所有播放任务
            handler.removeCallbacks(playbackRunnable);
            
            // 立即通知监听器执行急停
            if (listener != null) {
                try {
                    listener.onEmergencyStop();
                } catch (Exception e) {
                    Log.e(TAG, "急停回调异常", e);
                }
            }
        }
        
        // 设置状态为停止
        setState(PlayerState.STOPPED);
        
        // 重置播放参数
        currentFrame = 0;
        lastFrameTime = 0;
        
        // 重置-1值填充缓存
        resetLastValidValues();
        
        Log.w(TAG, "急停操作完成");
    }

    /**
     * 跳转到指定帧
     *
     * @param frameIndex 目标帧索引
     */
    public void seek(int frameIndex) {
        if (currentSequence == null) {
            Log.w(TAG, "未加载序列，无法跳转");
            return;
        }

        if (frameIndex < 0 || frameIndex >= getTotalFrames()) {
            Log.w(TAG, String.format("无效的帧索引: %d，范围: 0-%d", frameIndex, getTotalFrames() - 1));
            return;
        }

        boolean wasPlaying = (state == PlayerState.PLAYING);
        
        if (useNativePlayback && nativePlayerId > 0) {
            boolean success = nativeSeek(nativePlayerId, frameIndex);
            if (!success) {
                Log.w(TAG, "Native seek failed, falling back to Java implementation");
                useNativePlayback = false;
            } else {
                Log.d(TAG, String.format("Native seek to frame: %d/%d", frameIndex, getTotalFrames()));
                return;
            }
        }

        // Java实现的跳转逻辑
        // 停止当前播放
        if (wasPlaying) {
            handler.removeCallbacks(playbackRunnable);
        }

        currentFrame = frameIndex;

        Log.d(TAG, String.format("跳转到帧: %d/%d", frameIndex, getTotalFrames()));

        // 如果之前在播放，立即播放跳转后的帧
        if (wasPlaying) {
            lastFrameTime = SystemClock.elapsedRealtime();
            playNextFrame();
        }
    }

    /**
     * 获取当前播放器状态
     *
     * @return 当前状态
     */
    public PlayerState getState() {
        return state;
    }

    /**
     * 获取当前帧索引
     *
     * @return 当前帧索引（从0开始），如果未加载序列返回-1
     */
    public int getCurrentFrame() {
        if (useNativePlayback && nativePlayerId > 0) {
            return nativeGetCurrentFrame(nativePlayerId);
        }
        return currentFrame;
    }

    /**
     * 获取总帧数
     *
     * @return 总帧数，如果未加载序列返回0
     */
    public int getTotalFrames() {
        if (useNativePlayback && nativePlayerId > 0) {
            return nativeGetTotalFrames(nativePlayerId);
        }
        return currentSequence != null ? currentSequence.totalFrames : 0;
    }

    /**
     * 获取播放进度
     *
     * @return 进度值 0.0-1.0，如果未加载序列返回0
     */
    public float getProgress() {
        if (currentSequence == null || currentSequence.totalFrames == 0) {
            return 0;
        }
        return (float) currentFrame / currentSequence.totalFrames;
    }

    /**
     * 获取当前序列信息
     *
     * @return 序列信息字符串，如果未加载返回"未加载"
     */
    public String getSequenceInfo() {
        return currentSequence != null ? currentSequence.getInfo() : "未加载";
    }

    /**
     * 设置播放器状态
     *
     * @param newState 新状态
     */
    private void setState(PlayerState newState) {
        if (this.state != newState) {
            PlayerState oldState = this.state;
            this.state = newState;
            Log.d(TAG, "状态变化: " + oldState.getDescription() + " -> " + newState.getDescription());
        }
    }

    /**
     * 处理错误
     *
     * @param errorMessage 错误信息
     */
    private void handleError(String errorMessage) {
        setState(PlayerState.ERROR);
        handler.removeCallbacks(playbackRunnable);

        if (listener != null) {
            listener.onError(errorMessage);
        }

        Log.e(TAG, "错误: " + errorMessage);
    }

    /**
     * 释放资源
     * 应在Activity/Fragment销毁时调用
     */
    public void release() {
        Log.d(TAG, "释放资源");
        stop();
        handler.removeCallbacksAndMessages(null);
        currentSequence = null;
        listener = null;
        setState(PlayerState.IDLE);
        
        // 释放Native资源
        if (useNativePlayback && nativePlayerId > 0) {
            try {
                nativeUnregisterListener(nativePlayerId);
                nativeDestroy(nativePlayerId);
                nativePlayerId = -1;
                Log.d(TAG, "Native player resources released");
            } catch (Exception e) {
                Log.w(TAG, "Failed to release native resources", e);
            }
        }
        
        // 释放动作库更新器资源
        if (actionLibraryUpdater != null) {
            actionLibraryUpdater.release();
        }
    }

    /**
     * 重置-1值填充缓存
     * 将所有关节的最后有效值重置为-1
     */
    private void resetLastValidValues() {
        for (int i = 0; i < 10; i++) {
            lastValidLeftArm[i] = -1;
            lastValidRightArm[i] = -1;
        }
        Log.d(TAG, "已重置-1值填充缓存");
    }

    /**
     * 填充-1值：将数组中的-1值替换为对应关节的最后一个非-1值
     * 
     * @param currentValues 当前帧的关节值数组
     * @param lastValidValues 存储最后有效值的缓存数组
     * @return 处理后的关节值数组
     */
    private int[] fillMinusOneValues(int[] currentValues, int[] lastValidValues) {
        if (currentValues == null || currentValues.length != 10) {
            Log.w(TAG, "无效的关节数据数组");
            return currentValues;
        }

        // 创建新数组避免修改原始数据
        int[] processedValues = new int[currentValues.length];
        
        for (int i = 0; i < currentValues.length; i++) {
            if (currentValues[i] == -1) {
                // 当前值为-1，使用缓存的最后有效值
                if (lastValidValues[i] != -1) {
                    processedValues[i] = lastValidValues[i];
                    Log.v(TAG, String.format("关节%d: -1 -> %d (使用缓存值)", i, lastValidValues[i]));
                } else {
                    // 如果缓存中也没有有效值，保持-1
                    processedValues[i] = -1;
                    Log.v(TAG, String.format("关节%d: -1 -> -1 (无缓存值)", i));
                }
            } else {
                // 当前值不为-1，更新缓存并使用当前值
                lastValidValues[i] = currentValues[i];
                processedValues[i] = currentValues[i];
            }
        }
        
        return processedValues;
    }

    // 初始化播放任务
    {
        playbackRunnable = new Runnable() {
            @Override
            public void run() {
                playNextFrame();
            }
        };
    }
    
    /**
     * 初始化动作名称映射关系
     */
    private void initializeActionMappings() {
        if (actionLibraryUpdater != null) {
            // 在后台线程中初始化映射，避免阻塞主线程
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        actionLibraryUpdater.initializeMappings();
                        Log.d(TAG, "动作名称映射初始化完成");
                    } catch (Exception e) {
                        Log.w(TAG, "动作名称映射初始化失败", e);
                    }
                }
            }, "MappingInitializer").start();
        }
    }
    
    /**
     * 获取所有可用的动作信息列表
     * 包括本地assets和下载的动作库
     * 
     * @return 动作信息列表
     */
    public List<ActionInfo> getAllAvailableActions() {
        List<ActionInfo> actionList = new ArrayList<>();
        
        // 1. 获取本地assets中的动作
        try {
            String[] assetFiles = context.getAssets().list(ASSETS_PATH);
            if (assetFiles != null) {
                for (String fileName : assetFiles) {
                    if (fileName.endsWith(".ebs")) {
                        ActionInfo info = new ActionInfo();
                        info.fileName = fileName;
                        info.name = ActionNameUtils.extractActionNameFromFileName(fileName);
                        info.englishName = ActionNameUtils.getStandardName(info.name);
                        info.category = "本地动作";
                        info.status = "可用";
                        actionList.add(info);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "获取本地assets动作失败", e);
        }
        
        // 2. 获取下载的动作库
        if (actionLibraryUpdater != null) {
            List<File> downloadedFiles = actionLibraryUpdater.getAllLocalActionFiles();
            for (File file : downloadedFiles) {
                ActionInfo info = new ActionInfo();
                info.fileName = file.getName();
                info.name = ActionNameUtils.extractActionNameFromFileName(file.getName());
                info.englishName = ActionNameUtils.getStandardName(info.name);
                info.category = "下载动作";
                info.status = "可用";
                info.fileSize = file.length();
                info.lastModified = file.lastModified();
                actionList.add(info);
            }
        }
        
        Log.d(TAG, String.format("获取到 %d 个可用动作", actionList.size()));
        return actionList;
    }
    
    /**
     * 异步获取所有可用的动作信息
     * 
     * @param callback 回调接口
     */
    public void getAllAvailableActionsAsync(ActionListCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<ActionInfo> actions = getAllAvailableActions();
                    if (callback != null) {
                        handler.post(() -> callback.onSuccess(actions));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "异步获取动作列表失败", e);
                    if (callback != null) {
                        handler.post(() -> callback.onError("获取动作列表失败: " + e.getMessage()));
                    }
                }
            }
        }, "ActionListLoader").start();
    }
    
    /**
     * 根据名称查找动作信息
     * 支持中文名称和英文名称查找
     * 
     * @param actionName 动作名称
     * @return 动作信息，如果未找到返回null
     */
    public ActionInfo findActionByName(String actionName) {
        if (actionName == null || actionName.isEmpty()) {
            return null;
        }
        
        List<ActionInfo> allActions = getAllAvailableActions();
        for (ActionInfo action : allActions) {
            if (ActionNameUtils.isNameMatch(actionName, action.name) || 
                ActionNameUtils.isNameMatch(actionName, action.englishName)) {
                return action;
            }
        }
        
        return null;
    }
    
    /**
     * 获取动作名称映射信息
     * 返回当前所有的中英文名称映射关系
     * 
     * @return 映射信息字符串
     */
    public String getActionNameMappings() {
        StringBuilder mappings = new StringBuilder();
        mappings.append("当前动作名称映射数量: ").append(ActionNameUtils.getMappingCount()).append("\n");
        
        List<ActionInfo> actions = getAllAvailableActions();
        for (ActionInfo action : actions) {
            mappings.append(String.format("文件: %s\n", action.fileName));
            mappings.append(String.format("  中文名: %s\n", action.name));
            mappings.append(String.format("  英文名: %s\n", action.englishName));
            mappings.append(String.format("  类别: %s\n", action.category));
            mappings.append("---\n");
        }
        
        return mappings.toString();
    }
    
    /**
     * 获取动作库统计信息
     * 
     * @return 统计信息对象
     */
    public ActionLibraryStats getActionLibraryStats() {
        ActionLibraryStats stats = new ActionLibraryStats();
        
        List<ActionInfo> allActions = getAllAvailableActions();
        stats.totalActionCount = allActions.size();
        
        for (ActionInfo action : allActions) {
            if ("本地动作".equals(action.category)) {
                stats.localActionCount++;
            } else if ("下载动作".equals(action.category)) {
                stats.downloadedActionCount++;
                stats.totalDownloadedSize += action.fileSize;
            }
        }
        
        stats.mappingCount = ActionNameUtils.getMappingCount();
        stats.currentVersion = getCurrentLibraryVersion();
        
        if (actionLibraryUpdater != null) {
            stats.storageInfo = actionLibraryUpdater.getStorageInfo();
        }
        
        return stats;
    }
    
    /**
     * 动作库统计信息类
     */
    public static class ActionLibraryStats {
        public int totalActionCount = 0;        // 总动作数量
        public int localActionCount = 0;        // 本地动作数量
        public int downloadedActionCount = 0;   // 下载动作数量
        public long totalDownloadedSize = 0;    // 下载动作总大小
        public int mappingCount = 0;            // 映射关系数量
        public String currentVersion = "1.0.0"; // 当前版本
        public String storageInfo = "";         // 存储信息
        
        @Override
        public String toString() {
            return String.format(
                "动作库统计:\n" +
                "  总动作数: %d\n" +
                "  本地动作: %d\n" +
                "  下载动作: %d\n" +
                "  下载大小: %d bytes\n" +
                "  映射数量: %d\n" +
                "  当前版本: %s\n" +
                "  存储信息: %s",
                totalActionCount, localActionCount, downloadedActionCount,
                totalDownloadedSize, mappingCount, currentVersion, storageInfo
            );
        }
    }
    
    /**
     * 动作列表回调接口
     */
    public interface ActionListCallback {
        void onSuccess(List<ActionInfo> actions);
        void onError(String error);
    }
}
