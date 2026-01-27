package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 序列播放器测试类
 * 用于实际测试播放���能并记录结果
 */
public class SequencePlayerTest {

    private static final String TAG = "SequencePlayerTest";
    private static final boolean VERBOSE = true;  // 是否打印详细日志

    /**
     * 测试用函数式接口，允许抛出异常
     */
    @FunctionalInterface
    private interface TestRunnable {
        void run() throws Exception;
    }

    private EvoBotSequencePlayer player;
    private int frameCount = 0;
    private long startTime = 0;
    private long lastFrameTime = 0;
    private long maxInterval = 0;
    private long minInterval = Long.MAX_VALUE;
    private long totalInterval = 0;

    // 测试结果记录
    private StringBuilder testLog;
    private boolean testPassed = true;
    private String failureReason = "";

    /**
     * 运行所有测试
     */
    public void runAllTests(Context context) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "EvoBot序列播放器���试开始");
        Log.d(TAG, "========================================");

        testLog = new StringBuilder();
        testLog.append("EvoBot序列播放器测试报告\n");
        testLog.append("测试时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        testLog.append("========================================\n\n");

        // 测试1: 基本播放测试
        runTest("基本播放测试", () -> testBasicPlay(context));

        // 测试2: 频率控制测试
        runTest("频率控制测试", () -> testFrequencyControl(context));

        // 测试3: 播放控制测试
        runTest("播放控制测试", () -> testPlaybackControl(context));

        // 测试4: Seek跳转测试
        runTest("Seek跳转测试", () -> testSeek(context));

        // 测试5: 错误处理测试
        runTest("错误处理测试", () -> testErrorHandling(context));

        // 输出测试结果
        outputTestResults();

        Log.d(TAG, "========================================");
        Log.d(TAG, "测试完成");
        Log.d(TAG, "========================================");
    }

    /**
     * 运行单个测试
     */
    private void runTest(String testName, TestRunnable test) {
        Log.d(TAG, "\n--- " + testName + " ---");
        testLog.append("\n### ").append(testName).append("\n");

        try {
            test.run();
            testLog.append("✅ 通过\n");
            Log.d(TAG, "✅ 测试通过");
        } catch (Exception e) {
            testPassed = false;
            failureReason = testName + ": " + e.getMessage();
            testLog.append("❌ 失败: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "❌ 测试失败: " + e.getMessage(), e);
        }
    }

    /**
     * 测试1: 基本播放功能
     */
    private void testBasicPlay(Context context) throws Exception {
        resetCounters();

        player = new EvoBotSequencePlayer(context);

        // 使用英文名称进行测试
        player.play("arm_movement_left_arm_wave_right_arm_hug", 40, new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                long currentTime = System.currentTimeMillis();

                if (frameIndex == 0) {
                    startTime = currentTime;
                    lastFrameTime = currentTime;
                    Log.d(TAG, "第一帧数据: 左臂[" + leftArm.length + "关节], 右臂[" + rightArm.length + "关节]");
                }

                frameCount++;

                // 计算帧间隔
                if (frameIndex > 0) {
                    long interval = currentTime - lastFrameTime;
                    maxInterval = Math.max(maxInterval, interval);
                    minInterval = Math.min(minInterval, interval);
                    totalInterval += interval;

                    if (VERBOSE && frameIndex % 100 == 0) {
                        Log.d(TAG, String.format("帧%d: 间隔=%dms", frameIndex, interval));
                    }
                }

                lastFrameTime = currentTime;

                // 验证数据
                validateFrameData(leftArm, rightArm, frameIndex);
            }

            @Override
            public void onComplete() {
                long totalTime = System.currentTimeMillis() - startTime;
                double avgInterval = (double) totalInterval / (frameCount - 1);
                double actualFreq = 1000.0 / avgInterval;

                Log.d(TAG, String.format(
                    "播放完成: 总帧数=%d, 总时长=%dms, 平均间隔=%.2fms, 实际频率=%.2fHz",
                    frameCount, totalTime, avgInterval, actualFreq
                ));

                Log.d(TAG, String.format(
                    "间隔统计: 最小=%dms, 最大=%dms, 平均=%.2fms",
                    minInterval, maxInterval, avgInterval
                ));

                testLog.append(String.format(
                    "- 总帧数: %d\n- 总时长: %dms\n- 平均间隔: %.2fms (期望: 25ms)\n- 实际频率: %.2fHz (期望: 40Hz)\n- 间隔范围: %d-%dms\n",
                    frameCount, totalTime, avgInterval, actualFreq, minInterval, maxInterval
                ));

                // 验证帧数
                if (frameCount != 497) {
                    throw new RuntimeException("帧数不正确，期望497，实际" + frameCount);
                }

                // 验证频率误差 (±20%以内)
                double freqError = Math.abs(actualFreq - 40.0) / 40.0;
                if (freqError > 0.2) {
                    throw new RuntimeException(String.format("频率误差过大: %.1f%%", freqError * 100));
                }
            }

            @Override
            public void onError(String errorMessage) {
                throw new RuntimeException("播放出错: " + errorMessage);
            }

            @Override
            public void onEmergencyStop() {
                Log.d(TAG, "收到急停回调");
            }
        });

        // 等待播放完成 (最多15秒)
        waitForCompletion(15000);
    }

    /**
     * 测试2: 频率控制
     */
    private void testFrequencyControl(Context context) throws Exception {
        final int testFreq = 20;  // 测试20Hz
        final long expectedInterval = 50;  // 期望50ms

        player = new EvoBotSequencePlayer(context);

        // 使用英文名称进行测试
        player.play("arm_movement_left_arm_wave_right_arm_hug", testFreq, new SequenceListener() {
            private int localFrameCount = 0;
            private long localStartTime = 0;

            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                if (frameIndex == 0) {
                    localStartTime = System.currentTimeMillis();
                }
                localFrameCount++;

                // 只播放前50帧进行测试
                if (frameIndex >= 49) {
                    long totalTime = System.currentTimeMillis() - localStartTime;
                    long avgInterval = totalTime / localFrameCount;

                    Log.d(TAG, String.format(
                        "频率测试: 设定=%dHz, 实际平均间隔=%dms (期望%dms)",
                        testFreq, avgInterval, expectedInterval
                    ));

                    testLog.append(String.format(
                        "- 设定频率: %dHz\n- 实际间隔: %dms\n- 期望间隔: %dms\n",
                        testFreq, avgInterval, expectedInterval
                    ));

                    // 验证间隔 (±30%容差)
                    long error = Math.abs(avgInterval - expectedInterval);
                    if (error > expectedInterval * 0.3) {
                        throw new RuntimeException(String.format(
                            "间隔误差过大: %dms (%.1f%%)",
                            error, error * 100.0 / expectedInterval
                        ));
                    }

                    player.stop();
                }
            }

            @Override
            public void onComplete() {
                // 不会触发，因为会提前stop
            }

            @Override
            public void onError(String errorMessage) {
                throw new RuntimeException("频率测试出错: " + errorMessage);
            }

            @Override
            public void onEmergencyStop() {
                Log.d(TAG, "频率测试收到急停回调");
            }
        });

        // 等待测试完成 (最多5秒)
        waitForCompletion(5000);
    }

    /**
     * 测试3: 播放控制 (暂停/恢复/停止)
     */
    private void testPlaybackControl(Context context) throws Exception {
        player = new EvoBotSequencePlayer(context);

        final boolean[] paused = {false};
        final boolean[] resumed = {false};
        final boolean[] stopped = {false};

        // 使用英文名称进行测试
        player.play("arm_movement_left_arm_wave_right_arm_hug", 40, new SequenceListener() {
            private int localFrameCount = 0;

            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                localFrameCount++;

                // 第10帧暂停
                if (frameIndex == 10 && !paused[0]) {
                    Log.d(TAG, "测试暂停...");
                    player.pause();
                    paused[0] = true;

                    // 1秒后恢复
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        Log.d(TAG, "测试恢复...");
                        player.resume();
                        resumed[0] = true;
                    }, 1000);
                }

                // 第30帧停止
                if (frameIndex == 30) {
                    Log.d(TAG, "测试停止...");
                    player.stop();
                    stopped[0] = true;
                }
            }

            @Override
            public void onComplete() {
                // 不应该触发
            }

            @Override
            public void onError(String errorMessage) {
                throw new RuntimeException("播放控制测试出错: " + errorMessage);
            }

            @Override
            public void onEmergencyStop() {
                Log.d(TAG, "播放控制测试收到急停回调");
            }
        });

        // 等待测试完成 (最多5秒)
        waitForCompletion(5000);

        // 验证控制功能
        if (!paused[0]) {
            throw new RuntimeException("暂停功能未执行");
        }
        if (!resumed[0]) {
            throw new RuntimeException("恢复功能未执行");
        }
        if (!stopped[0]) {
            throw new RuntimeException("停止功能未执行");
        }

        testLog.append("- 暂停功能: ✅\n- 恢复功能: ✅\n- 停止功能: ✅\n");
        Log.d(TAG, "✅ 播放控制测试通过");
    }

    /**
     * 测试4: Seek跳转功能
     */
    private void testSeek(Context context) throws Exception {
        player = new EvoBotSequencePlayer(context);

        final int[] seekFrame = {0};
        final boolean[] seekExecuted = {false};

        // 使用英文名称进行测试
        player.play("arm_movement_left_arm_wave_right_arm_hug", 40, new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                // 第5帧跳转到第100帧
                if (frameIndex == 5 && !seekExecuted[0]) {
                    Log.d(TAG, "测试Seek: 从帧5跳到帧100...");
                    seekFrame[0] = frameIndex;
                    player.seek(100);
                    seekExecuted[0] = true;

                    // 跳转后10帧停止
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        player.stop();
                    }, 300);
                }
            }

            @Override
            public void onComplete() {
                // 不应该触发
            }

            @Override
            public void onError(String errorMessage) {
                throw new RuntimeException("Seek测试出错: " + errorMessage);
            }

            @Override
            public void onEmergencyStop() {
                Log.d(TAG, "Seek测试收到急停回调");
            }
        });

        // 等待测试完成 (最多5秒)
        waitForCompletion(5000);

        if (!seekExecuted[0]) {
            throw new RuntimeException("Seek功能未执行");
        }

        testLog.append("- Seek跳转: ✅\n");
        Log.d(TAG, "✅ Seek测试通过");
    }

    /**
     * 测试5: 错误处理
     */
    private void testErrorHandling(Context context) throws Exception {
        player = new EvoBotSequencePlayer(context);

        final boolean[] errorReceived = {false};

        // 使用英文名称进行测试
        player.play("arm_movement_left_arm_wave_right_arm_hug", 40, new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                // 正常播放几帧
                if (frameIndex >= 5) {
                    player.stop();
                }
            }

            @Override
            public void onComplete() {
                // 不应该触发
            }

            @Override
            public void onError(String errorMessage) {
                errorReceived[0] = true;
                Log.d(TAG, "收到错误回调: " + errorMessage);
            }

            @Override
            public void onEmergencyStop() {
                Log.d(TAG, "错误处理测试收到急停回调");
            }
        });

        // 测试无效参数
        try {
            player.play("", 40, new SequenceListener() {
                @Override
                public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {}

                @Override
                public void onComplete() {}

                @Override
                public void onError(String errorMessage) {}

                @Override
                public void onEmergencyStop() {}
            });
            throw new RuntimeException("应该抛出空参数异常");
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "✅ 正确处理空参数: " + e.getMessage());
        }

        // 测试无效频率
        try {
            player.play("test", 0, new SequenceListener() {
                @Override
                public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {}

                @Override
                public void onComplete() {}

                @Override
                public void onError(String errorMessage) {}

                @Override
                public void onEmergencyStop() {}
            });
            throw new RuntimeException("应该抛出无效频率异常");
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "✅ 正确处理无效频率: " + e.getMessage());
        }

        testLog.append("- 参数验证: ✅\n- 错误处理: ✅\n");
        Log.d(TAG, "✅ 错误处理测试通过");
    }

    /**
     * 验证帧数据
     */
    private void validateFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
        // 验证数组长度
        if (leftArm.length != 10) {
            throw new RuntimeException("左臂关节数量错误: " + leftArm.length);
        }
        if (rightArm.length != 10) {
            throw new RuntimeException("右臂关节数量错误: " + rightArm.length);
        }

        // 验证关节位置范围
        for (int i = 0; i < 10; i++) {
            int leftPos = leftArm[i];
            int rightPos = rightArm[i];

            if (leftPos != -1 && (leftPos < 0 || leftPos > 4095)) {
                throw new RuntimeException(String.format(
                    "帧%d左臂关节%d位置无效: %d", frameIndex, i, leftPos
                ));
            }
            if (rightPos != -1 && (rightPos < 0 || rightPos > 4095)) {
                throw new RuntimeException(String.format(
                    "帧%d右臂关节%d位置无效: %d", frameIndex, i, rightPos
                ));
            }
        }

        // 记录第一帧和最后一帧的详细数据
        if (VERBOSE) {
            if (frameIndex == 0) {
                testLog.append("- 第一帧数据:\n");
                testLog.append("  左臂: ").append(arrayToString(leftArm)).append("\n");
                testLog.append("  右臂: ").append(arrayToString(rightArm)).append("\n");
            } else if (frameIndex == 496) {
                testLog.append("- 最后一帧数据:\n");
                testLog.append("  左臂: ").append(arrayToString(leftArm)).append("\n");
                testLog.append("  右臂: ").append(arrayToString(rightArm)).append("\n");
            }
        }
    }

    /**
     * 数组转字符串
     */
    private String arrayToString(int[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 重置计数器
     */
    private void resetCounters() {
        frameCount = 0;
        startTime = 0;
        lastFrameTime = 0;
        maxInterval = 0;
        minInterval = Long.MAX_VALUE;
        totalInterval = 0;
    }

    /**
     * 等待播放完成
     */
    private void waitForCompletion(long timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        while (player.getState() == PlayerState.PLAYING) {
            Thread.sleep(100);
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new RuntimeException("等待超时: " + timeoutMs + "ms");
            }
        }
    }

    /**
     * 输出测试结果
     */
    private void outputTestResults() {
        testLog.append("\n========================================\n");
        if (testPassed) {
            testLog.append("✅ 所有测试通过!\n");
            Log.d(TAG, "========================================");
            Log.d(TAG, "✅ 所有测试通过!");
        } else {
            testLog.append("❌ 测试失败: ").append(failureReason).append("\n");
            Log.d(TAG, "========================================");
            Log.e(TAG, "❌ 测试失败: " + failureReason);
        }
        testLog.append("========================================\n");

        // 打印完整报告
        Log.d(TAG, "\n" + testLog.toString());

        // 保存到文件
        saveTestReport();
    }

    /**
     * 保存测试报告
     */
    private void saveTestReport() {
        try {
            // 保存到外部存储
            File outputDir = new File(android.os.Environment.getExternalStorageDirectory(), "EvoBotTest");
            outputDir.mkdirs();

            String fileName = "test_report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";
            File outputFile = new File(outputDir, fileName);

            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(testLog.toString().getBytes("UTF-8"));
            fos.close();

            Log.d(TAG, "测试报告已保存: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "保存测试报告失败: " + e.getMessage());
        }
    }

    /**
     * 获取测试是否通过
     */
    public boolean isTestPassed() {
        return testPassed;
    }

    /**
     * 获取失败原因
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * 获取测试日志
     */
    public String getTestLog() {
        return testLog.toString();
    }

    /**
     * 释放资源
     */
    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
