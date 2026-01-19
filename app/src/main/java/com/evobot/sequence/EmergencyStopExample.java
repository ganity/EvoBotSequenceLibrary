package com.evobot.sequence;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * 急停功能使用示例
 * 演示如何使用新增的急停方法
 */
public class EmergencyStopExample {

    private static final String TAG = "EmergencyStopExample";

    /**
     * 演示急停功能的使用
     */
    public static void demonstrateEmergencyStop(Context context) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "急停功能演示");
        Log.d(TAG, "========================================");

        EvoBotSequencePlayer player = new EvoBotSequencePlayer(context);

        // 创建监听器，实现急停回调
        SequenceListener listener = new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                Log.d(TAG, "播放帧 " + frameIndex + " - 左臂: " + arrayToString(leftArm) + 
                          ", 右臂: " + arrayToString(rightArm));
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "播放完成");
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "播放错误: " + errorMessage);
            }

            @Override
            public void onEmergencyStop() {
                Log.w(TAG, "🚨 收到急停信号！立即停止位置输出！");
                // 在这里实现急停逻辑：
                // 1. 立即停止向机器人发送位置指令
                // 2. 可选：发送安全位置或保持当前位置的指令
                // 3. 记录急停事件
                stopRobotMovement();
            }
        };

        // 开始播放
        Log.d(TAG, "开始播放序列...");
        player.play("左臂挥手右臂掐腰抱胸", 40, listener);

        // 模拟在播放过程中触发急停（3秒后）
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "⚠️ 检测到紧急情况，执行急停！");
                player.emergencyStop();
            }
        }, 3000);  // 3秒后触发急停

        Log.d(TAG, "急停演示已启动，将在3秒后触发急停");
    }

    /**
     * 模拟停止机器人运动的方法
     * 在实际应用中，这里应该包含停止机器人的具体逻辑
     */
    private static void stopRobotMovement() {
        Log.w(TAG, "🛑 执行机器人急停操作:");
        Log.w(TAG, "  - 停止发送位置指令");
        Log.w(TAG, "  - 机器人保持当前位置");
        Log.w(TAG, "  - 记录急停事件到日志");
        
        // 在实际应用中，这里应该包含：
        // 1. 停止向机器人硬件发送新的位置指令
        // 2. 可选：发送"保持当前位置"指令
        // 3. 记录急停时间和原因
        // 4. 通知其他系统组件
    }

    /**
     * 数组转字符串辅助方法
     */
    private static String arrayToString(int[] array) {
        if (array == null || array.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(array.length, 3); i++) {  // 只显示前3个元素
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        if (array.length > 3) {
            sb.append("...");
        }
        sb.append("]");
        return sb.toString();
    }
}