# EvoBot序列播放器库 ProGuard规则

# 保持所有公共API
-keep public class com.evobot.sequence.** {
    public *;
}

# 保持回调接口
-keep interface com.evobot.sequence.SequenceListener {
    public *;
}

# 保持枚举
-keep enum com.evobot.sequence.PlayerState {
    public *;
}

# 保持数据模型
-keep class com.evobot.sequence.SequenceData {
    public *;
}
