package com.evobot.sequence;

/**
 * 序列数据模型
 * 存储从.ebs二进制文件解析出来的完整序列数据
 */
public class SequenceData {

    // 常量定义
    public static final int JOINTS_PER_ARM = 10;      // 每臂关节数
    public static final int HOLD_SENTINEL = 0xFFFF;   // 二进制文件中表示-1的值
    public static final int POSITION_MAX = 4095;      // 关节位置最大值
    public static final int POSITION_MIN = 0;         // 关节位置最小值

    // 元数据
    public String name;                // 序列名称
    public float sampleRate;           // 采样率 (Hz)，例如40.0f
    public float totalDuration;        // 总时长 (秒)
    public int totalFrames;            // 总帧数
    public int compiledAt;             // 编译时间戳 (Unix时间戳，秒)

    // 序列数据
    public int[][] leftArmSequence;    // 左臂序列 [帧号][关节号]，大小[totalFrames][10]
    public int[][] rightArmSequence;   // 右臂序列 [帧号][关节号]，大小[totalFrames][10]

    /**
     * 验证序列数据完整性
     *
     * @return true if data is valid, false otherwise
     */
    public boolean validate() {
        // 检查必需字段
        if (name == null || name.isEmpty()) {
            return false;
        }

        if (sampleRate <= 0 || totalDuration <= 0 || totalFrames <= 0) {
            return false;
        }

        // 检查序列数组
        if (leftArmSequence == null || rightArmSequence == null) {
            return false;
        }

        if (leftArmSequence.length != totalFrames || rightArmSequence.length != totalFrames) {
            return false;
        }

        // 检查每帧数据
        for (int i = 0; i < totalFrames; i++) {
            if (leftArmSequence[i] == null || leftArmSequence[i].length != JOINTS_PER_ARM) {
                return false;
            }
            if (rightArmSequence[i] == null || rightArmSequence[i].length != JOINTS_PER_ARM) {
                return false;
            }

            // 验证关节位置范围
            for (int j = 0; j < JOINTS_PER_ARM; j++) {
                int leftPos = leftArmSequence[i][j];
                int rightPos = rightArmSequence[i][j];

                // -1表示保持，其他值必须在0-4095范围内
                if (leftPos != -1 && (leftPos < POSITION_MIN || leftPos > POSITION_MAX)) {
                    return false;
                }
                if (rightPos != -1 && (rightPos < POSITION_MIN || rightPos > POSITION_MAX)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 获取指定帧的左臂位置数据
     *
     * @param frameIndex 帧索引
     * @return 左臂10个关节的位置数组，如果索引无效返回null
     */
    public int[] getLeftArmFrame(int frameIndex) {
        if (frameIndex < 0 || frameIndex >= totalFrames) {
            return null;
        }
        return leftArmSequence[frameIndex];
    }

    /**
     * 获取指定帧的右臂位置数据
     *
     * @param frameIndex 帧索引
     * @return 右臂10个关节的位置数组，如果索引无效返回null
     */
    public int[] getRightArmFrame(int frameIndex) {
        if (frameIndex < 0 || frameIndex >= totalFrames) {
            return null;
        }
        return rightArmSequence[frameIndex];
    }

    /**
     * 获取序列信息摘要
     *
     * @return 格式化的序列信息字符串
     */
    public String getInfo() {
        return String.format(
            "Sequence[name=%s, frames=%d, rate=%.1fHz, duration=%.3fs]",
            name, totalFrames, sampleRate, totalDuration
        );
    }

    @Override
    public String toString() {
        return getInfo();
    }
}
