package com.evobot.sequence;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 完整播放测试 - 播放全部497帧
 * 用于验证定时精度和时间控制
 */
public class FullPlaybackTest {

    private static final String EBS_FILE = "app/src/main/assets/sequences/左臂挥手右臂掐腰抱胸_20260116_142711.ebs";
    private static final int TARGET_FREQUENCY = 40;  // 目标频率40Hz
    private static final long TARGET_INTERVAL_MS = 1000 / TARGET_FREQUENCY;  // 25ms

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("完整播放测试 - 全部497帧");
        System.out.println("========================================");
        System.out.println("测试时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        System.out.println("目标频率: " + TARGET_FREQUENCY + " Hz");
        System.out.println("目标间隔: " + TARGET_INTERVAL_MS + " ms");
        System.out.println("总帧数: 497");
        System.out.println("理论时长: " + String.format("%.3f", 497.0 / TARGET_FREQUENCY) + " 秒");
        System.out.println("");

        try {
            // 加载序列数据
            SequenceData sequence = loadSequence();
            System.out.println("✅ 序列加载成功: " + sequence.getInfo());
            System.out.println("");

            // 播放并记录
            PlaybackResult result = playFullSequence(sequence);

            // 输出详细报告
            printDetailedReport(result);

            // 保存详细日志
            saveDetailedLog(result);

        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("❌ 测试失败: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
        }
    }

    /**
     * 加载序列数据
     */
    private static SequenceData loadSequence() throws Exception {
        FileInputStream fis = new FileInputStream(EBS_FILE);
        byte[] fileData = new byte[fis.available()];
        fis.read(fileData);
        fis.close();

        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 读取文件头
        buffer.position(96);  // 跳过文件头

        int frameCount = 497;
        int[][] leftArmSequence = new int[frameCount][10];
        int[][] rightArmSequence = new int[frameCount][10];

        // 读取所有帧
        for (int frame = 0; frame < frameCount; frame++) {
            for (int joint = 0; joint < 20; joint++) {
                int pos = buffer.getShort() & 0xFFFF;
                if (pos == 0xFFFF) {
                    pos = -1;
                }

                if (joint < 10) {
                    leftArmSequence[frame][joint] = pos;
                } else {
                    rightArmSequence[frame][joint - 10] = pos;
                }
            }
        }

        SequenceData data = new SequenceData();
        data.name = "左臂挥手右臂掐腰抱胸";
        data.sampleRate = 40.0f;
        data.totalDuration = 497 / 40.0f;
        data.totalFrames = frameCount;
        data.leftArmSequence = leftArmSequence;
        data.rightArmSequence = rightArmSequence;

        return data;
    }

    /**
     * 播放完整序列并记录时间戳
     */
    private static PlaybackResult playFullSequence(SequenceData sequence) throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("开始播放完整序列...");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("");

        PlaybackResult result = new PlaybackResult();
        result.totalFrames = sequence.totalFrames;
        result.frameTimestamps = new ArrayList<>(sequence.totalFrames);
        result.frameIntervals = new ArrayList<>(sequence.totalFrames - 1);

        long startTime = System.currentTimeMillis();
        result.startTime = startTime;

        System.out.println(String.format("[%s] 帧0 开始播放",
            formatTime(startTime)));

        // 播放所有帧
        for (int frame = 0; frame < sequence.totalFrames; frame++) {
            long frameStartTime = System.currentTimeMillis();

            // 记录时间戳
            result.frameTimestamps.add(frameStartTime);

            // 处理帧数据（模拟回调）
            if (frame < 5 || frame >= sequence.totalFrames - 5) {
                // 只打印前5帧和后5帧的详细信息
                int[] leftArm = sequence.leftArmSequence[frame];
                int[] rightArm = sequence.rightArmSequence[frame];
                System.out.println(String.format("[%s] 帧%d 左臂: [%d,%d,%d,%d,%d,%d,%d,%d,%d,%d] 右臂: [%d,%d,%d,%d,%d,%d,%d,%d,%d,%d]",
                    formatTime(frameStartTime),
                    frame,
                    leftArm[0], leftArm[1], leftArm[2], leftArm[3], leftArm[4],
                    leftArm[5], leftArm[6], leftArm[7], leftArm[8], leftArm[9],
                    rightArm[0], rightArm[1], rightArm[2], rightArm[3], rightArm[4],
                    rightArm[5], rightArm[6], rightArm[7], rightArm[8], rightArm[9]
                ));
            } else if (frame == 5) {
                System.out.println("  ... (中间帧省略，只记录时间戳) ...");
            }

            // 计算与上一帧的间隔
            if (frame > 0) {
                long interval = frameStartTime - result.frameTimestamps.get(frame - 1);
                result.frameIntervals.add(interval);
            }

            // 控制帧间隔
            if (frame < sequence.totalFrames - 1) {
                long elapsed = System.currentTimeMillis() - frameStartTime;
                long sleepTime = Math.max(0, TARGET_INTERVAL_MS - elapsed);

                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        result.endTime = endTime;

        System.out.println("");
        System.out.println(String.format("[%s] 帧%d (最后一帧) 播放完成",
            formatTime(endTime), sequence.totalFrames - 1));

        return result;
    }

    /**
     * 打印详细报告
     */
    private static void printDetailedReport(PlaybackResult result) {
        System.out.println("");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("播放统计报告");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("");

        // 基本统计
        long totalDuration = result.endTime - result.startTime;
        double actualDurationSec = totalDuration / 1000.0;
        double theoreticalDurationSec = result.totalFrames / (double) TARGET_FREQUENCY;

        System.out.println("【基本信息】");
        System.out.println("总帧数: " + result.totalFrames);
        System.out.println("开始时间: " + formatTime(result.startTime));
        System.out.println("结束时间: " + formatTime(result.endTime));
        System.out.println("总时长: " + totalDuration + " ms (" + String.format("%.3f", actualDurationSec) + " 秒)");
        System.out.println("理论时长: " + String.format("%.3f", theoreticalDurationSec) + " 秒");
        System.out.println("时长误差: " + String.format("%.3f", actualDurationSec - theoreticalDurationSec) + " 秒");
        System.out.println("");

        // 间隔统计
        if (!result.frameIntervals.isEmpty()) {
            long minInterval = Long.MAX_VALUE;
            long maxInterval = 0;
            long totalInterval = 0;

            for (long interval : result.frameIntervals) {
                minInterval = Math.min(minInterval, interval);
                maxInterval = Math.max(maxInterval, interval);
                totalInterval += interval;
            }

            double avgInterval = (double) totalInterval / result.frameIntervals.size();
            double actualFreq = 1000.0 / avgInterval;
            double freqError = Math.abs(actualFreq - TARGET_FREQUENCY) / TARGET_FREQUENCY * 100;

            System.out.println("【间隔统计】");
            System.out.println("目标间隔: " + TARGET_INTERVAL_MS + " ms");
            System.out.println("平均间隔: " + String.format("%.2f", avgInterval) + " ms");
            System.out.println("最小间隔: " + minInterval + " ms");
            System.out.println("最大间隔: " + maxInterval + " ms");
            System.out.println("间隔范围: " + (maxInterval - minInterval) + " ms");
            System.out.println("");

            System.out.println("【频率统计】");
            System.out.println("目标频率: " + TARGET_FREQUENCY + " Hz");
            System.out.println("实际频率: " + String.format("%.2f", actualFreq) + " Hz");
            System.out.println("频率误差: " + String.format("%.2f", freqError) + "%");
            System.out.println("");

            // 评估
            System.out.println("【性能评估】");
            boolean excellent = freqError < 5;
            boolean good = freqError < 10;
            boolean acceptable = freqError < 20;

            if (excellent) {
                System.out.println("✅ 优秀 - 频率误差 < 5%");
            } else if (good) {
                System.out.println("✅ 良好 - 频率误差 < 10%");
            } else if (acceptable) {
                System.out.println("✅ 可接受 - 频率误差 < 20%");
            } else {
                System.out.println("⚠️  需要优化 - 频率误差 >= 20%");
            }

            // 间隔分布
            System.out.println("");
            System.out.println("【间隔分布】");
            int[] distribution = new int[11];  // 0-5ms, 5-10ms, ..., 45-50ms, >50ms
            for (long interval : result.frameIntervals) {
                int bucket = (int) Math.min(interval / 5, 10);
                distribution[bucket]++;
            }

            for (int i = 0; i < distribution.length; i++) {
                String range;
                if (i < 10) {
                    range = (i * 5) + "-" + ((i + 1) * 5) + "ms";
                } else {
                    range = ">50ms";
                }
                double percentage = distribution[i] * 100.0 / result.frameIntervals.size();
                System.out.println(String.format("  %s: %d 次 (%.1f%%)", range, distribution[i], percentage));
            }
        }

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 保存详细日志到文件
     */
    private static void saveDetailedLog(PlaybackResult result) {
        try {
            String fileName = "playback_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";
            FileOutputStream fos = new FileOutputStream(fileName);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));

            // 写入CSV头
            pw.println("FrameIndex,Timestamp,Interval,LeftArm0,LeftArm1,LeftArm2,LeftArm3,LeftArm4,LeftArm5,LeftArm6,LeftArm7,LeftArm8,LeftArm9,RightArm0,RightArm1,RightArm2,RightArm3,RightArm4,RightArm5,RightArm6,RightArm7,RightArm8,RightArm9");

            // 写入每帧数据
            for (int i = 0; i < result.frameTimestamps.size(); i++) {
                long timestamp = result.frameTimestamps.get(i);
                long interval = (i > 0) ? (timestamp - result.frameTimestamps.get(i - 1)) : 0;

                // 这里为了简化，只写入时间戳和间隔，实际应用中可以写入完整关节数据
                pw.printf("%d,%d,%d%n", i, timestamp, interval);
            }

            pw.close();
            fos.close();

            System.out.println("");
            System.out.println("详细日志已保存: " + fileName);
        } catch (Exception e) {
            System.err.println("保存日志失败: " + e.getMessage());
        }
    }

    /**
     * 格式化时间戳
     */
    private static String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(millis));
    }

    /**
     * 播放结果数据结构
     */
    private static class PlaybackResult {
        int totalFrames;
        long startTime;
        long endTime;
        List<Long> frameTimestamps;      // 每帧的时间戳
        List<Long> frameIntervals;        // 帧间隔（n个帧有n-1个间隔）
    }

    /**
     * 序列数据简化版
     */
    private static class SequenceData {
        String name;
        float sampleRate;
        float totalDuration;
        int totalFrames;
        int[][] leftArmSequence;
        int[][] rightArmSequence;

        String getInfo() {
            return String.format("%s [frames=%d, rate=%.1fHz, duration=%.3fs]",
                name, totalFrames, sampleRate, totalDuration);
        }
    }
}
