package com.evobot.sequence;

/**
 * 序列播放监听器接口
 * 用于接收序列播放过程中的回调事件
 */
public interface SequenceListener {

    /**
     * 帧数据回调
     * 按照设定的频率（如40Hz）定时回调
     *
     * @param leftArm     左臂10个关节的位置数据，每个关节范围0-4095，-1表示保持当前位置
     * @param rightArm    右臂10个关节的位置数据，每个关节范围0-4095，-1表示保持当前位置
     * @param frameIndex  当前帧索引（从0开始）
     */
    void onFrameData(int[] leftArm, int[] rightArm, int frameIndex);

    /**
     * 播放完成回调
     * 当所有帧都播放完毕时调用
     */
    void onComplete();

    /**
     * 错误回调
     * 当加载或播放过程中发生错误时调用
     *
     * @param errorMessage 错误信息描述
     */
    void onError(String errorMessage);
}
