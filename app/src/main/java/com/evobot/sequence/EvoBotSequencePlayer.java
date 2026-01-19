package com.evobot.sequence;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

/**
 * EvoBot序列播放器
 * 按照指定频率（如40Hz）播放序列数据并通过Listener回调
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

    /**
     * 构造函数
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

        Log.d(TAG, "EvoBotSequencePlayer初始化完成");
    }

    /**
     * 播放序列（使用默认40Hz频率）
     *
     * @param actionName 动作名称（用于查找对应的.ebs文件）
     * @param listener   回调监听器
     */
    public void play(String actionName, SequenceListener listener) {
        play(actionName, DEFAULT_FREQUENCY, listener);
    }

    /**
     * 播放序列（指定频率）
     *
     * @param actionName 动作名称
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

        Log.d(TAG, String.format("准备播放: action=%s, frequency=%dHz", actionName, frequency));

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
     * 异步加载序列
     *
     * @param actionName 动作名称
     */
    private void loadSequenceAsync(String actionName) {
        setState(PlayerState.LOADING);

        try {
            // 构建文件路径
            String assetPath = ASSETS_PATH + DEFAULT_SEQUENCE_FILE;

            // 加载序列
            Log.d(TAG, "正在加载序列文件: " + assetPath);
            currentSequence = loader.loadFromAssets(assetPath);

            // 重置播放状态
            currentFrame = 0;
            lastFrameTime = 0;
            
            // 重置-1值填充缓存
            resetLastValidValues();

            setState(PlayerState.READY);

            // 开始播放
            startPlayback();

        } catch (Exception e) {
            Log.e(TAG, "加载序列失败", e);
            handleError("加载序列失败: " + e.getMessage());
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
        handler.removeCallbacks(playbackRunnable);

        Log.d(TAG, String.format("播放已暂停，当前帧: %d/%d", currentFrame, currentSequence.totalFrames));
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
        startPlayback();
    }

    /**
     * 停止播放
     */
    public void stop() {
        if (state == PlayerState.IDLE || state == PlayerState.STOPPED) {
            return;
        }

        setState(PlayerState.STOPPED);
        handler.removeCallbacks(playbackRunnable);
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
        
        // 立即停止所有播放任务
        handler.removeCallbacks(playbackRunnable);
        
        // 设置状态为停止
        setState(PlayerState.STOPPED);
        
        // 重置播放参数
        currentFrame = 0;
        lastFrameTime = 0;
        
        // 重置-1值填充缓存
        resetLastValidValues();
        
        // 立即通知监听器执行急停
        if (listener != null) {
            try {
                listener.onEmergencyStop();
            } catch (Exception e) {
                Log.e(TAG, "急停回调异常", e);
            }
        }
        
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

        if (frameIndex < 0 || frameIndex >= currentSequence.totalFrames) {
            Log.w(TAG, String.format("无效的帧索引: %d，范围: 0-%d", frameIndex, currentSequence.totalFrames - 1));
            return;
        }

        boolean wasPlaying = (state == PlayerState.PLAYING);

        // 停止当前播放
        if (wasPlaying) {
            handler.removeCallbacks(playbackRunnable);
        }

        currentFrame = frameIndex;

        Log.d(TAG, String.format("跳转到帧: %d/%d", frameIndex, currentSequence.totalFrames));

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
        return currentFrame;
    }

    /**
     * 获取总帧数
     *
     * @return 总帧数，如果未加载序列返回0
     */
    public int getTotalFrames() {
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
}
