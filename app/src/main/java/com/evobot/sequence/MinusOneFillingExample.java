package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

/**
 * -1值填充功能使用示例
 * 展示如何使用改进后的SequenceListener来处理-1值填充
 */
public class MinusOneFillingExample {

    private static final String TAG = "MinusOneFillingExample";

    /**
     * 运行-1值填充示例
     * 
     * @param context Android上下文
     */
    public static void runExample(Context context) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "开始运行-1值填充示例");
        Log.d(TAG, "========================================");

        EvoBotSequencePlayer player = new EvoBotSequencePlayer(context);

        player.play("左臂挥手右臂掐腰抱胸", 40, new SequenceListener() {
            private int frameCount = 0;

            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                frameCount++;

                // 记录包含-1值的帧
                boolean hasMinusOne = false;
                for (int i = 0; i < leftArm.length; i++) {
                    if (leftArm[i] == -1 || rightArm[i] == -1) {
                        hasMinusOne = true;
                        break;
                    }
                }

                // 如果发现-1值，说明填充功能没有正常工作
                if (hasMinusOne) {
                    Log.w(TAG, String.format("帧%d仍包含-1值:", frameIndex));
                    logFrameData(leftArm, rightArm, frameIndex);
                } else if (frameIndex % 50 == 0) {
                    // 每50帧记录一次正常数据
                    Log.d(TAG, String.format("帧%d数据正常（无-1值）", frameIndex));
                }

                // 演示如何使用处理后的数据
                processJointData(leftArm, rightArm, frameIndex);

                // 运行100帧后停止示例
                if (frameIndex >= 99) {
                    Log.d(TAG, "示例运行完成，停止播放");
                    player.stop();
                }
            }

            @Override
            public void onComplete() {
                Log.d(TAG, String.format("播放完成，总共处理了%d帧", frameCount));
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "播放出错: " + errorMessage);
            }

            @Override
            public void onEmergencyStop() {
                Log.w(TAG, "收到急停信号");
            }
        });

        // 等待示例完成
        try {
            long startTime = System.currentTimeMillis();
            while (player.getState() == PlayerState.PLAYING) {
                Thread.sleep(100);
                if (System.currentTimeMillis() - startTime > 10000) {
                    Log.w(TAG, "示例运行超时，强制停止");
                    player.stop();
                    break;
                }
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "示例被中断", e);
        }

        Log.d(TAG, "========================================");
        Log.d(TAG, "-1值填充示例运行完成");
        Log.d(TAG, "========================================");

        // 释放资源
        player.release();
    }

    /**
     * 处理关节数据的示例方法
     * 这里展示如何使用经过-1值填充处理后的数据
     * 
     * @param leftArm 左臂关节数据（已处理-1值）
     * @param rightArm 右臂关节数据（已处理-1值）
     * @param frameIndex 当前帧索引
     */
    private static void processJointData(int[] leftArm, int[] rightArm, int frameIndex) {
        // 示例1: 计算关节角度变化
        if (frameIndex > 0) {
            // 这里可以与上一帧数据比较，计算角度变化率
            // 由于-1值已经被填充，可以安全地进行数值计算
        }

        // 示例2: 发送到硬件控制器
        // sendToHardware(leftArm, rightArm);

        // 示例3: 记录到数据库或文件
        // saveToDatabase(leftArm, rightArm, frameIndex);

        // 示例4: 实时可视化
        // updateVisualization(leftArm, rightArm);

        // 示例5: 安全检查（所有值都应该在有效范围内）
        validateJointSafety(leftArm, rightArm, frameIndex);
    }

    /**
     * 验证关节数据安全性
     * 确保所有关节值都在安全范围内
     */
    private static void validateJointSafety(int[] leftArm, int[] rightArm, int frameIndex) {
        for (int i = 0; i < leftArm.length; i++) {
            // 检查左臂关节
            if (leftArm[i] < 0 || leftArm[i] > 4095) {
                Log.e(TAG, String.format("帧%d左臂关节%d值异常: %d", frameIndex, i, leftArm[i]));
            }

            // 检查右臂关节
            if (rightArm[i] < 0 || rightArm[i] > 4095) {
                Log.e(TAG, String.format("帧%d右臂关节%d值异常: %d", frameIndex, i, rightArm[i]));
            }
        }
    }

    /**
     * 记录帧数据详情
     */
    private static void logFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("帧").append(frameIndex).append(":\n");
        
        sb.append("  左臂: [");
        for (int i = 0; i < leftArm.length; i++) {
            if (i > 0) sb.append(", ");
            if (leftArm[i] == -1) {
                sb.append("*-1*");  // 高亮显示-1值
            } else {
                sb.append(leftArm[i]);
            }
        }
        sb.append("]\n");
        
        sb.append("  右臂: [");
        for (int i = 0; i < rightArm.length; i++) {
            if (i > 0) sb.append(", ");
            if (rightArm[i] == -1) {
                sb.append("*-1*");  // 高亮显示-1值
            } else {
                sb.append(rightArm[i]);
            }
        }
        sb.append("]");
        
        Log.d(TAG, sb.toString());
    }

    /**
     * 模拟发送数据到硬件控制器
     * 这是一个示例方法，展示如何使用处理后的数据
     */
    private static void sendToHardware(int[] leftArm, int[] rightArm) {
        // 示例：构建硬件控制命令
        StringBuilder command = new StringBuilder();
        command.append("MOVE_JOINTS:");
        
        // 左臂关节
        command.append("L[");
        for (int i = 0; i < leftArm.length; i++) {
            if (i > 0) command.append(",");
            command.append(leftArm[i]);
        }
        command.append("]");
        
        // 右臂关节
        command.append("R[");
        for (int i = 0; i < rightArm.length; i++) {
            if (i > 0) command.append(",");
            command.append(rightArm[i]);
        }
        command.append("]");
        
        // 这里可以通过串口、网络等方式发送到实际硬件
        // Log.v(TAG, "发送到硬件: " + command.toString());
    }

    /**
     * 创建一个简单的测试监听器
     * 用于验证-1值填充功能
     */
    public static SequenceListener createTestListener() {
        return new SequenceListener() {
            private int[] lastLeftArm = new int[10];
            private int[] lastRightArm = new int[10];
            private boolean firstFrame = true;

            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                if (firstFrame) {
                    // 第一帧，记录初始值
                    System.arraycopy(leftArm, 0, lastLeftArm, 0, 10);
                    System.arraycopy(rightArm, 0, lastRightArm, 0, 10);
                    firstFrame = false;
                    Log.d(TAG, "记录第一帧数据作为基准");
                } else {
                    // 检查是否有-1值被正确填充
                    for (int i = 0; i < 10; i++) {
                        if (leftArm[i] == -1) {
                            Log.w(TAG, String.format("帧%d左臂关节%d仍为-1", frameIndex, i));
                        }
                        if (rightArm[i] == -1) {
                            Log.w(TAG, String.format("帧%d右臂关节%d仍为-1", frameIndex, i));
                        }
                    }
                    
                    // 更新上一帧数据
                    System.arraycopy(leftArm, 0, lastLeftArm, 0, 10);
                    System.arraycopy(rightArm, 0, lastRightArm, 0, 10);
                }
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "测试监听器：播放完成");
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "测试监听器：错误 - " + errorMessage);
            }

            @Override
            public void onEmergencyStop() {
                Log.w(TAG, "测试监听器：急停");
            }
        };
    }
}