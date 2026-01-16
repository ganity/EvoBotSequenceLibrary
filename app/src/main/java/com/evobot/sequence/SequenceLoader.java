package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * 序列加载器
 * 负责从assets目录加载并解析.ebs二进制序列文件
 */
public class SequenceLoader {

    private static final String TAG = "SequenceLoader";

    // 文件格式常量
    private static final String MAGIC_NUMBER = "EBS1";
    private static final int HEADER_SIZE = 96;
    private static final int FRAME_SIZE = 40;  // 20关节 × 2字节
    private static final int NAME_SIZE = 64;

    private final Context context;

    public SequenceLoader(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context不能为null");
        }
        this.context = context.getApplicationContext();
    }

    /**
     * 从assets加载序列数据
     *
     * @param assetPath assets中的文件路径，例如 "sequences/左臂挥手右臂掐腰抱胸_20260116_142711.ebs"
     * @return 解析后的序列数据
     * @throws IOException 文件读取或解析失败
     */
    public SequenceData loadFromAssets(String assetPath) throws IOException {
        if (assetPath == null || assetPath.isEmpty()) {
            throw new IllegalArgumentException("assetPath不能为空");
        }

        Log.d(TAG, "开始加载序列: " + assetPath);

        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(assetPath);
            return parseEbsFile(inputStream);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭输入流失败", e);
                }
            }
        }
    }

    /**
     * 解析.ebs二进制文件
     *
     * @param inputStream 输入流
     * @return 解析后的序列数据
     * @throws IOException 解析失败
     */
    private SequenceData parseEbsFile(InputStream inputStream) throws IOException {
        // 读取整个���件到内存
        byte[] fileData = readAllBytes(inputStream);
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 验证文件大小
        if (fileData.length < HEADER_SIZE) {
            throw new IOException("文件过小，不是有效的.ebs文件");
        }

        // ========== 解析文件头 (96 bytes) ==========

        // 魔数 (4 bytes)
        byte[] magicBytes = new byte[4];
        buffer.get(magicBytes);
        String magic = new String(magicBytes, StandardCharsets.US_ASCII);
        if (!MAGIC_NUMBER.equals(magic)) {
            throw new IOException("无效的.ebs文件格式，魔数应为: " + MAGIC_NUMBER + "，实际为: " + magic);
        }

        // 帧数 (4 bytes, uint32)
        int frameCount = (int)(buffer.getInt() & 0xFFFFFFFFL);
        if (frameCount <= 0 || frameCount > 100000) {
            throw new IOException("无效的帧数: " + frameCount);
        }

        // 采样率 (4 bytes, float)
        float sampleRate = buffer.getFloat();
        if (sampleRate <= 0 || sampleRate > 1000) {
            throw new IOException("无效的采样率: " + sampleRate);
        }

        // 总时长 (4 bytes, float)
        float totalDuration = buffer.getFloat();
        if (totalDuration <= 0 || totalDuration > 3600) {
            throw new IOException("无效的总时长: " + totalDuration);
        }

        // 编译时间 (4 bytes, uint32)
        int compiledAt = (int)(buffer.getInt() & 0xFFFFFFFFL);

        // 保留字段 (12 bytes) - 跳过
        buffer.position(buffer.position() + 12);

        // 序列名称 (64 bytes, UTF-8)
        byte[] nameBytes = new byte[NAME_SIZE];
        buffer.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8).trim();

        Log.d(TAG, String.format("文件头解析完成: name=%s, frames=%d, rate=%.1fHz, duration=%.3fs",
            name, frameCount, sampleRate, totalDuration));

        // ========== 验证数据区大小 ==========
        int expectedDataSize = frameCount * FRAME_SIZE;
        int remainingBytes = fileData.length - HEADER_SIZE;
        if (remainingBytes < expectedDataSize) {
            throw new IOException(String.format(
                "文件数据区不完整，预期%d字节，实际%d字节",
                expectedDataSize, remainingBytes));
        }

        // ========== 解析数据区 ==========
        int[][] leftArmSequence = new int[frameCount][SequenceData.JOINTS_PER_ARM];
        int[][] rightArmSequence = new int[frameCount][SequenceData.JOINTS_PER_ARM];

        for (int frame = 0; frame < frameCount; frame++) {
            for (int joint = 0; joint < 20; joint++) {
                // 读取2字节无符号整数
                int pos = buffer.getShort() & 0xFFFF;

                // 0xFFFF转换为-1（保持当前位置）
                if (pos == SequenceData.HOLD_SENTINEL) {
                    pos = -1;
                }

                // 分配到左右臂
                if (joint < SequenceData.JOINTS_PER_ARM) {
                    // 左臂关节 0-9
                    leftArmSequence[frame][joint] = pos;
                } else {
                    // 右臂关节 0-9
                    rightArmSequence[frame][joint - SequenceData.JOINTS_PER_ARM] = pos;
                }
            }
        }

        // ========== 构建SequenceData对象 ==========
        SequenceData data = new SequenceData();
        data.name = name;
        data.sampleRate = sampleRate;
        data.totalDuration = totalDuration;
        data.totalFrames = frameCount;
        data.compiledAt = compiledAt;
        data.leftArmSequence = leftArmSequence;
        data.rightArmSequence = rightArmSequence;

        // 验证数据完整性
        if (!data.validate()) {
            throw new IOException("序列数据验证失败");
        }

        Log.d(TAG, "序列解析完成: " + data.getInfo());
        return data;
    }

    /**
     * 读取输入流中的所有字节
     *
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException 读取失败
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    /**
     * 获取序列信息（不加载完整数据）
     * 只读取文件头，用于快速查询序列元数据
     *
     * @param assetPath assets中的文件路径
     * @return 序列信息数组 [名称, 帧数, 采样率, 总时长, 编译时间]
     * @throws IOException 读取失败
     */
    public Object[] getSequenceInfo(String assetPath) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(assetPath);
            byte[] header = new byte[HEADER_SIZE];
            int bytesRead = inputStream.read(header);
            if (bytesRead < HEADER_SIZE) {
                throw new IOException("文件头不完整");
            }

            ByteBuffer buffer = ByteBuffer.wrap(header);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // 跳过魔数
            buffer.position(4);

            // 读取关键字段
            int frameCount = (int)(buffer.getInt() & 0xFFFFFFFFL);
            float sampleRate = buffer.getFloat();
            float totalDuration = buffer.getFloat();
            int compiledAt = (int)(buffer.getInt() & 0xFFFFFFFFL);

            // 跳过保留字段
            buffer.position(buffer.position() + 12);

            // 读取名称
            byte[] nameBytes = new byte[NAME_SIZE];
            buffer.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8).trim();

            return new Object[]{name, frameCount, sampleRate, totalDuration, compiledAt};
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭输入流失败", e);
                }
            }
        }
    }
}
