package com.evobot.sequence;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 纯Java测试类 - 不依赖Android环境
 * 使用Java标准库直接解析和测试序列数据
 */
public class SimpleSequenceTest {

    private static final String EBS_FILE = "app/src/main/assets/sequences/左臂挥手右臂掐腰抱胸_20260116_142711.ebs";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("EvoBot序列播放器 - 纯Java测试");
        System.out.println("========================================");
        System.out.println("测试时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        System.out.println();

        try {
            // 测试1: 文件读取测试
            testFileRead();

            // 测试2: 数据解析测试
            testDataParse();

            // 测试3: 数据完整性测试
            testDataIntegrity();

            // 测试4: 模拟播放测试
            testSimulatedPlayback();

            System.out.println("\n========================================");
            System.out.println("✅ 所有测试通过!");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("❌ 测试失败: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
        }
    }

    /**
     * 测试1: 文件读取
     */
    private static void testFileRead() throws Exception {
        System.out.println("--- 测试1: 文件读取 ---");

        File file = new File(EBS_FILE);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + EBS_FILE);
        }

        long fileSize = file.length();
        System.out.println("✅ 文件路径: " + file.getAbsolutePath());
        System.out.println("✅ 文件大小: " + fileSize + " bytes (" + (fileSize / 1024.0) + " KB)");

        if (fileSize < 96) {
            throw new RuntimeException("文件过小，不是有效的.ebs文件");
        }
    }

    /**
     * 测试2: 数据解析
     */
    private static void testDataParse() throws Exception {
        System.out.println("\n--- 测试2: 数据解析 ---");

        FileInputStream fis = new FileInputStream(EBS_FILE);
        byte[] fileData = new byte[fis.available()];
        fis.read(fileData);
        fis.close();

        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 读取文件头
        byte[] magicBytes = new byte[4];
        buffer.get(magicBytes);
        String magic = new String(magicBytes, StandardCharsets.US_ASCII);

        long frameCountLong = buffer.getInt() & 0xFFFFFFFFL;
        int frameCount = (int) frameCountLong;
        float sampleRate = buffer.getFloat();
        float totalDuration = buffer.getFloat();
        long compiledAtLong = buffer.getInt() & 0xFFFFFFFFL;
        int compiledAt = (int) compiledAtLong;

        buffer.position(buffer.position() + 12);  // 跳过保留字段

        byte[] nameBytes = new byte[64];
        buffer.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8).trim();

        // 验证魔数
        if (!"EBS1".equals(magic)) {
            throw new RuntimeException("无效的魔数: " + magic);
        }

        System.out.println("✅ 魔数: " + magic);
        System.out.println("✅ 序列名称: " + name);
        System.out.println("✅ 帧数: " + frameCount);
        System.out.println("✅ 采样率: " + sampleRate + " Hz");
        System.out.println("✅ 总时长: " + totalDuration + " s");
        System.out.println("✅ 编译时间: " + compiledAt);

        // 验证数据
        if (frameCount != 497) {
            throw new RuntimeException("帧数不匹配，期望497，实际" + frameCount);
        }
        if (sampleRate != 40.0f) {
            throw new RuntimeException("采样率不匹配，期望40.0，实际" + sampleRate);
        }
    }

    /**
     * 测试3: 数据完整性
     */
    private static void testDataIntegrity() throws Exception {
        System.out.println("\n--- 测试3: 数据完整性验证 ---");

        FileInputStream fis = new FileInputStream(EBS_FILE);
        byte[] fileData = new byte[fis.available()];
        fis.read(fileData);
        fis.close();

        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 跳过文件头
        buffer.position(96);

        int frameCount = 497;
        int errorCount = 0;

        // 定义要详细验证的帧：开头、中间、结尾
        int[] framesToVerify = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9,  // 前10帧
                                 100, 200, 300, 400,  // 中间帧
                                 487, 488, 489, 490, 491, 492, 493, 494, 495, 496};  // 后10帧

        System.out.println("验证帧数: " + framesToVerify.length + " 帧 (开头10帧 + 中间5帧 + 结尾10帧)");

        // 读取所有帧数据到内存
        int[][] allLeftFrames = new int[frameCount][10];
        int[][] allRightFrames = new int[frameCount][10];

        for (int frame = 0; frame < frameCount; frame++) {
            for (int joint = 0; joint < 20; joint++) {
                int pos = buffer.getShort() & 0xFFFF;

                // 转换0xFFFF为-1
                if (pos == 0xFFFF) {
                    pos = -1;
                }

                // 验证范围
                if (pos != -1 && (pos < 0 || pos > 4095)) {
                    System.err.println("❌ 帧" + frame + " 关节" + joint + " 位置无效: " + pos);
                    errorCount++;
                }

                // 存储数据
                if (joint < 10) {
                    allLeftFrames[frame][joint] = pos;
                } else {
                    allRightFrames[frame][joint - 10] = pos;
                }
            }
        }

        System.out.println("✅ 完整数据验证完成，总帧数: " + frameCount + "，错误数: " + errorCount);

        if (errorCount > 0) {
            throw new RuntimeException("数据完整性验证失败，发现" + errorCount + "个错误");
        }

        // 详细输出关键帧数据
        System.out.println("\n关键帧数据详情:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 输出前10帧
        System.out.println("\n【前10帧】");
        for (int i = 0; i < 10; i++) {
            System.out.println("帧" + i + ":");
            System.out.println("  左臂: " + arrayToString(allLeftFrames[i]));
            System.out.println("  右臂: " + arrayToString(allRightFrames[i]));
        }

        // 输出中间关键帧
        System.out.println("\n【中间关键帧】");
        int[] midFrames = {100, 200, 300, 400};
        for (int frame : midFrames) {
            System.out.println("帧" + frame + ":");
            System.out.println("  左臂: " + arrayToString(allLeftFrames[frame]));
            System.out.println("  右臂: " + arrayToString(allRightFrames[frame]));
        }

        // 输出后10帧 (487-496)
        System.out.println("\n【后10帧 (结尾)】");
        for (int i = 487; i < 497; i++) {
            System.out.println("帧" + i + ":");
            System.out.println("  左臂: " + arrayToString(allLeftFrames[i]));
            System.out.println("  右臂: " + arrayToString(allRightFrames[i]));
        }

        // 数据统计
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("数据统计:");
        System.out.println("-1(保持) 关节统计:");

        int[] leftHoldCount = new int[10];
        int[] rightHoldCount = new int[10];

        for (int frame = 0; frame < frameCount; frame++) {
            for (int joint = 0; joint < 10; joint++) {
                if (allLeftFrames[frame][joint] == -1) leftHoldCount[joint]++;
                if (allRightFrames[frame][joint] == -1) rightHoldCount[joint]++;
            }
        }

        System.out.print("  左臂各关节-1出现次数: ");
        System.out.println(arrayToString(leftHoldCount));
        System.out.print("  右臂各关节-1出现次数: ");
        System.out.println(arrayToString(rightHoldCount));

        // 计算关节位置范围
        int[] leftMin = new int[10];
        int[] leftMax = new int[10];
        int[] rightMin = new int[10];
        int[] rightMax = new int[10];

        for (int i = 0; i < 10; i++) {
            leftMin[i] = Integer.MAX_VALUE;
            leftMax[i] = Integer.MIN_VALUE;
            rightMin[i] = Integer.MAX_VALUE;
            rightMax[i] = Integer.MIN_VALUE;
        }

        for (int frame = 0; frame < frameCount; frame++) {
            for (int joint = 0; joint < 10; joint++) {
                int leftPos = allLeftFrames[frame][joint];
                int rightPos = allRightFrames[frame][joint];

                if (leftPos != -1) {
                    leftMin[joint] = Math.min(leftMin[joint], leftPos);
                    leftMax[joint] = Math.max(leftMax[joint], leftPos);
                }

                if (rightPos != -1) {
                    rightMin[joint] = Math.min(rightMin[joint], rightPos);
                    rightMax[joint] = Math.max(rightMax[joint], rightPos);
                }
            }
        }

        System.out.println("\n关节位置范围 (忽略-1):");
        System.out.println("  左臂:");
        for (int i = 0; i < 10; i++) {
            if (leftMin[i] != Integer.MAX_VALUE) {
                System.out.println("    关节" + i + ": " + leftMin[i] + " - " + leftMax[i]);
            } else {
                System.out.println("    关节" + i + ": 全部为-1 (保持)");
            }
        }

        System.out.println("  右臂:");
        for (int i = 0; i < 10; i++) {
            if (rightMin[i] != Integer.MIN_VALUE) {
                System.out.println("    关节" + i + ": " + rightMin[i] + " - " + rightMax[i]);
            } else {
                System.out.println("    关节" + i + ": 全部为-1 (保持)");
            }
        }

        System.out.println("\n✅ 数据完整性验证通过！");
    }

    /**
     * 测试4: 模拟播放
     */
    private static void testSimulatedPlayback() throws Exception {
        System.out.println("\n--- 测试4: 模拟播放 (前50帧) ---");

        int frequency = 40;  // 40Hz
        long intervalMs = 1000 / frequency;  // 25ms

        System.out.println("模拟参数:");
        System.out.println("- 频率: " + frequency + " Hz");
        System.out.println("- 间隔: " + intervalMs + " ms");

        // 模拟播放前50帧
        long startTime = System.currentTimeMillis();
        long minInterval = Long.MAX_VALUE;
        long maxInterval = 0;
        long totalInterval = 0;

        for (int frame = 0; frame < 50; frame++) {
            long frameStart = System.currentTimeMillis();

            // 模拟帧数据处理
            simulateFrameProcessing(frame);

            // 控制帧间隔
            long elapsed = System.currentTimeMillis() - frameStart;
            long sleepTime = Math.max(0, intervalMs - elapsed);

            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
            }

            long actualInterval = System.currentTimeMillis() - frameStart;
            minInterval = Math.min(minInterval, actualInterval);
            maxInterval = Math.max(maxInterval, actualInterval);
            totalInterval += actualInterval;
        }

        long totalTime = System.currentTimeMillis() - startTime;
        double avgInterval = (double) totalInterval / 50;
        double actualFreq = 1000.0 / avgInterval;

        System.out.println("\n播放统计:");
        System.out.println("- 总帧数: 50");
        System.out.println("- 总时长: " + totalTime + " ms");
        System.out.println("- 平均间隔: " + String.format("%.2f", avgInterval) + " ms (期望: " + intervalMs + " ms)");
        System.out.println("- 实际频率: " + String.format("%.2f", actualFreq) + " Hz (期望: " + frequency + " Hz)");
        System.out.println("- 间隔范围: " + minInterval + " - " + maxInterval + " ms");

        // 验证频率误差 (±30%容差)
        double freqError = Math.abs(actualFreq - frequency) / frequency;
        if (freqError > 0.3) {
            throw new RuntimeException(String.format("频率误差过大: %.1f%%", freqError * 100));
        }

        System.out.println("✅ 播放测试通过，频率误差: " + String.format("%.1f%%", freqError * 100));
    }

    /**
     * 模拟帧数据处理
     */
    private static void simulateFrameProcessing(int frameIndex) {
        // 模拟关节数据处理（实际应用中这里会发送到机器人）
        if (frameIndex == 0 || frameIndex == 49) {
            // 只打印第一帧和最后一帧
            System.out.println("  帧" + frameIndex + ": 处理关节数据 [10左臂 + 10右臂]");
        }
    }

    /**
     * 数组转字符串
     */
    private static String arrayToString(int[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
