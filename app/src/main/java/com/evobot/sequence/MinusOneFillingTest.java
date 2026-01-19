package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

/**
 * -1值填充功能测试类
 * 专门测试序列播放器中-1值用上一帧非-1值填充的功能
 */
public class MinusOneFillingTest {

    private static final String TAG = "MinusOneFillingTest";

    /**
     * 测试-1值填充功能
     * 
     * @param context Android上下文
     */
    public static void testMinusOneFilling(Context context) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "开始测试-1值填充功能");
        Log.d(TAG, "========================================");

        EvoBotSequencePlayer player = new EvoBotSequencePlayer(context);

        // 用于记录测试结果
        final boolean[] testPassed = {true};
        final StringBuilder errorLog = new StringBuilder();

        // 用于跟踪每个关节的预期值（模拟填充逻辑）
        final int[] expectedLeftArm = new int[10];
        final int[] expectedRightArm = new int[10];
        
        // 初始化为-1
        for (int i = 0; i < 10; i++) {
            expectedLeftArm[i] = -1;
            expectedRightArm[i] = -1;
        }

        player.play("左臂挥手右臂掐腰抱胸", 40, new SequenceListener() {
            private int frameCount = 0;

            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                frameCount++;

                try {
                    // 验证-1值填充逻辑
                    validateMinusOneFilling(leftArm, rightArm, frameIndex, 
                                          expectedLeftArm, expectedRightArm);

                    // 每100帧打印一次详细信息
                    if (frameIndex % 100 == 0) {
                        Log.d(TAG, String.format("帧%d验证通过", frameIndex));
                        logFrameDetails(leftArm, rightArm, frameIndex);
                    }

                    // 测试前50帧后停止（足够验证填充逻辑）
                    if (frameIndex >= 49) {
                        Log.d(TAG, "测试完成，停止播放");
                        player.stop();
                    }

                } catch (Exception e) {
                    testPassed[0] = false;
                    errorLog.append("帧").append(frameIndex).append("验证失败: ")
                            .append(e.getMessage()).append("\n");
                    Log.e(TAG, "帧" + frameIndex + "验证失败", e);
                    player.stop();
                }
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "播放完成");
            }

            @Override
            public void onError(String errorMessage) {
                testPassed[0] = false;
                errorLog.append("播放错误: ").append(errorMessage).append("\n");
                Log.e(TAG, "播放错误: " + errorMessage);
            }

            @Override
            public void onEmergencyStop() {
                Log.d(TAG, "收到急停回调");
            }
        });

        // 等待测试完成
        try {
            long startTime = System.currentTimeMillis();
            while (player.getState() == PlayerState.PLAYING) {
                Thread.sleep(100);
                if (System.currentTimeMillis() - startTime > 10000) {
                    Log.e(TAG, "测试超时");
                    testPassed[0] = false;
                    break;
                }
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "测试被中断", e);
            testPassed[0] = false;
        }

        // 输出测试结果
        Log.d(TAG, "========================================");
        if (testPassed[0]) {
            Log.d(TAG, "✅ -1值填充功能测试通过!");
        } else {
            Log.e(TAG, "❌ -1值填充功能测试失败:");
            Log.e(TAG, errorLog.toString());
        }
        Log.d(TAG, "========================================");

        // 释放资源
        player.release();
    }

    /**
     * 验证-1值填充逻辑
     * 
     * @param actualLeftArm 实际接收到的左臂数据
     * @param actualRightArm 实际接收到的右臂数据
     * @param frameIndex 当前帧索引
     * @param expectedLeftArm 预期的左臂数据（用于跟踪填充逻辑）
     * @param expectedRightArm 预期的右臂数据（用于跟踪填充逻辑）
     */
    private static void validateMinusOneFilling(int[] actualLeftArm, int[] actualRightArm, 
                                               int frameIndex, int[] expectedLeftArm, int[] expectedRightArm) {
        
        // 验证数组长度
        if (actualLeftArm.length != 10) {
            throw new RuntimeException("左臂关节数量错误: " + actualLeftArm.length);
        }
        if (actualRightArm.length != 10) {
            throw new RuntimeException("右臂关节数量错误: " + actualRightArm.length);
        }

        // 模拟原始数据（这里我们需要从实际的序列文件中获取原始数据来对比）
        // 由于我们无法直接获取原始数据，我们验证以下规则：
        // 1. 所有非-1值都应该在有效范围内 (0-4095)
        // 2. 如果当前帧某个关节不是-1，更新预期值
        // 3. 如果当前帧某个关节是-1，应该使用之前的有效值（如果有的话）

        for (int i = 0; i < 10; i++) {
            int leftValue = actualLeftArm[i];
            int rightValue = actualRightArm[i];

            // 验证左臂关节
            if (leftValue != -1) {
                // 非-1值应该在有效范围内
                if (leftValue < 0 || leftValue > 4095) {
                    throw new RuntimeException(String.format(
                        "左臂关节%d值超出范围: %d (应该在0-4095之间)", i, leftValue));
                }
                // 更新预期值
                expectedLeftArm[i] = leftValue;
            } else {
                // -1值应该被填充为上一个有效值（如果有的话）
                if (expectedLeftArm[i] != -1) {
                    throw new RuntimeException(String.format(
                        "左臂关节%d应该被填充为%d，但仍然是-1", i, expectedLeftArm[i]));
                }
                // 如果没有上一个有效值，-1是可以接受的
            }

            // 验证右臂关节
            if (rightValue != -1) {
                // 非-1值应该在有效范围内
                if (rightValue < 0 || rightValue > 4095) {
                    throw new RuntimeException(String.format(
                        "右臂关节%d值超出范围: %d (应该在0-4095之间)", i, rightValue));
                }
                // 更新预期值
                expectedRightArm[i] = rightValue;
            } else {
                // -1值应该被填充为上一个有效值（如果有的话）
                if (expectedRightArm[i] != -1) {
                    throw new RuntimeException(String.format(
                        "右臂关节%d应该被填充为%d，但仍然是-1", i, expectedRightArm[i]));
                }
                // 如果没有上一个有效值，-1是可以接受的
            }
        }
    }

    /**
     * 记录帧详细信息
     */
    private static void logFrameDetails(int[] leftArm, int[] rightArm, int frameIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("帧").append(frameIndex).append("详情:\n");
        
        sb.append("  左臂: [");
        for (int i = 0; i < leftArm.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(leftArm[i]);
        }
        sb.append("]\n");
        
        sb.append("  右臂: [");
        for (int i = 0; i < rightArm.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(rightArm[i]);
        }
        sb.append("]");
        
        Log.d(TAG, sb.toString());
    }

    /**
     * 创建测试用的模拟序列数据（包含-1值）
     * 这个方法可以用来创建特定的测试场景
     */
    public static SequenceData createTestSequenceWithMinusOnes() {
        // 创建一个包含-1值的测试序列
        int totalFrames = 10;
        int[][] leftArmSequence = new int[totalFrames][10];
        int[][] rightArmSequence = new int[totalFrames][10];

        // 第一帧：所有关节都有有效值
        for (int i = 0; i < 10; i++) {
            leftArmSequence[0][i] = 1000 + i * 100;  // 1000, 1100, 1200, ...
            rightArmSequence[0][i] = 2000 + i * 100; // 2000, 2100, 2200, ...
        }

        // 第二帧：部分关节为-1
        for (int i = 0; i < 10; i++) {
            leftArmSequence[1][i] = (i % 2 == 0) ? -1 : (1000 + i * 100 + 10);
            rightArmSequence[1][i] = (i % 3 == 0) ? -1 : (2000 + i * 100 + 10);
        }

        // 第三帧：更多关节为-1
        for (int i = 0; i < 10; i++) {
            leftArmSequence[2][i] = (i < 5) ? -1 : (1000 + i * 100 + 20);
            rightArmSequence[2][i] = (i > 5) ? -1 : (2000 + i * 100 + 20);
        }

        // 剩余帧：随机分布-1值
        for (int frame = 3; frame < totalFrames; frame++) {
            for (int joint = 0; joint < 10; joint++) {
                // 30%概率为-1
                if (Math.random() < 0.3) {
                    leftArmSequence[frame][joint] = -1;
                    rightArmSequence[frame][joint] = -1;
                } else {
                    leftArmSequence[frame][joint] = 1000 + joint * 100 + frame * 10;
                    rightArmSequence[frame][joint] = 2000 + joint * 100 + frame * 10;
                }
            }
        }

        return new SequenceData(leftArmSequence, rightArmSequence, totalFrames);
    }
}