package com.evobot.sequence;

/**
 * 播放器状态枚举
 * 定义序列播放器的所有可能状态
 */
public enum PlayerState {
    /**
     * 空闲状态
     * 播放器已初始化，未加载任何序列
     */
    IDLE("空闲"),

    /**
     * 加载中状态
     * 正在从assets加载序列数据
     */
    LOADING("加载中"),

    /**
     * 就绪状态
     * 序列已加载完成，等待开始播放
     */
    READY("就绪"),

    /**
     * 播放中状态
     * 正在按设定频率播放序列
     */
    PLAYING("播放中"),

    /**
     * 已暂停状态
     * 播放被暂停，可以恢复播放
     */
    PAUSED("已暂停"),

    /**
     * 已停止状态
     * 播放已停止，可以重新开始
     */
    STOPPED("已停止"),

    /**
     * 错误状态
     * 发生错误，需要重新加载序列
     */
    ERROR("错误");

    private final String description;

    PlayerState(String description) {
        this.description = description;
    }

    /**
     * 获取状态的中文描述
     *
     * @return 状态描述字符串
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }

    /**
     * 判断是否可以开始播放
     * 只有READY或PAUSED状态可以开始播放
     *
     * @return true if can play, false otherwise
     */
    public boolean canPlay() {
        return this == READY || this == PAUSED;
    }

    /**
     * 判断是否可以暂停
     * 只有PLAYING状态可以暂停
     *
     * @return true if can pause, false otherwise
     */
    public boolean canPause() {
        return this == PLAYING;
    }

    /**
     * 判断是否可以停止
     * PLAYING或PAUSED状态可以停止
     *
     * @return true if can stop, false otherwise
     */
    public boolean canStop() {
        return this == PLAYING || this == PAUSED;
    }

    /**
     * 判断是否为终止状态
     * STOPPED或ERROR状态为终止状态
     *
     * @return true if is terminal, false otherwise
     */
    public boolean isTerminal() {
        return this == STOPPED || this == ERROR;
    }
}
