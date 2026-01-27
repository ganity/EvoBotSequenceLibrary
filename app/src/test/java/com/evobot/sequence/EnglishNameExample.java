package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

/**
 * 英文名称使用示例
 * 展示如何在应用中正确使用英文动作名称
 */
public class EnglishNameExample {
    
    private static final String TAG = "EnglishNameExample";
    
    private EvoBotSequencePlayer player;
    
    public EnglishNameExample(Context context) {
        player = new EvoBotSequencePlayer(context);
    }
    
    /**
     * 推荐的使用方式：使用英文名称
     */
    public void playActionWithEnglishName() {
        // 使用英文名称调用动作，确保跨平台兼容性
        player.play("arm_movement_left_arm_wave", new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                // 处理帧数据
            }
            
            @Override
            public void onComplete() {
                Log.d(TAG, "左臂挥手动作完成");
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "动作播放失败: " + errorMessage);
            }
            
            @Override
            public void onEmergencyStop() {
                Log.d(TAG, "急停触发");
            }
        });
    }
    
    /**
     * 兼容的使用方式：使用中文名称（会自动转换）
     */
    public void playActionWithChineseName() {
        // 使用中文名称，系统会自动转换为英文名称
        player.play("左臂挥手", new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                // 处理帧数据
            }
            
            @Override
            public void onComplete() {
                Log.d(TAG, "动作播放完成");
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "动作播放失败: " + errorMessage);
            }
            
            @Override
            public void onEmergencyStop() {
                Log.d(TAG, "急停触发");
            }
        });
    }
    
    /**
     * 批量播放动作序列
     */
    public void playActionSequence() {
        String[] actions = {
            "arm_movement_left_arm_wave",    // 左臂挥手
            "arm_movement_right_arm_wave",   // 右臂挥手
            "head_nod_confirm",              // 点头确认
            "smile_greeting"                 // 微笑打招呼
        };
        
        playActionsSequentially(actions, 0);
    }
    
    private void playActionsSequentially(String[] actions, int index) {
        if (index >= actions.length) {
            Log.d(TAG, "所有动作播放完成");
            return;
        }
        
        String currentAction = actions[index];
        Log.d(TAG, "播放动作: " + currentAction);
        
        player.play(currentAction, new SequenceListener() {
            @Override
            public void onFrameData(int[] leftArm, int[] rightArm, int frameIndex) {
                // 处理帧数据
            }
            
            @Override
            public void onComplete() {
                Log.d(TAG, "完成播放: " + currentAction);
                // 播放下一个动作
                playActionsSequentially(actions, index + 1);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "动作播放失败: " + currentAction + ", 错误: " + errorMessage);
                // 继续播放下一个动作
                playActionsSequentially(actions, index + 1);
            }
            
            @Override
            public void onEmergencyStop() {
                Log.d(TAG, "急停触发");
            }
        });
    }
    
    /**
     * 检查动作名称类型和映射状态
     */
    public void checkActionNameTypes() {
        String[] testNames = {
            "左臂挥手",                        // 中文名称
            "arm_movement_left_arm_wave",     // 英文名称
            "右臂拥抱",                        // 中文名称
            "arm_movement_right_arm_hug",     // 英文名称
            "unknown_action"                  // 未知名称
        };
        
        Log.d(TAG, "当前映射数量: " + ActionNameUtils.getMappingCount());
        
        for (String name : testNames) {
            Log.d(TAG, String.format("名称: %s, 是否中文: %s, 是否英文: %s, 转换结果: 中->英=%s, 英->中=%s",
                name,
                ActionNameUtils.isChineseName(name),
                ActionNameUtils.isEnglishName(name),
                ActionNameUtils.chineseToEnglish(name),
                ActionNameUtils.englishToChinese(name)
            ));
        }
    }
    
    /**
     * 名称匹配测试
     */
    public void testNameMatching() {
        // 注意：这些映射关系是动态建立的，需要先下载或解析相应的动作文件
        String[][] testPairs = {
            {"左臂挥手", "arm_movement_left_arm_wave"},
            {"右臂拥抱", "arm_movement_right_arm_hug"},
            {"点头确认", "head_nod_confirm"},
            {"微笑打招呼", "smile_greeting"}
        };
        
        Log.d(TAG, "开始名称匹配测试...");
        
        for (String[] pair : testPairs) {
            boolean isMatch = ActionNameUtils.isNameMatch(pair[0], pair[1]);
            Log.d(TAG, String.format("匹配测试: '%s' <-> '%s' = %s", 
                pair[0], pair[1], isMatch));
        }
    }
    
    /**
     * 演示动态映射的建立过程
     */
    public void demonstrateDynamicMapping() {
        Log.d(TAG, "=== 动态映射演示 ===");
        
        // 1. 清除现有映射
        ActionNameUtils.clearMappings();
        Log.d(TAG, "清除映射后，映射数量: " + ActionNameUtils.getMappingCount());
        
        // 2. 手动添加一些映射（模拟从API或文件解析获得）
        ActionNameUtils.addMapping("左臂挥手", "arm_movement_left_arm_wave");
        ActionNameUtils.addMapping("右臂挥手", "arm_movement_right_arm_wave");
        ActionNameUtils.addMapping("点头确认", "head_nod_confirm");
        
        Log.d(TAG, "添加映射后，映射数量: " + ActionNameUtils.getMappingCount());
        
        // 3. 测试转换
        String chineseName = "左臂挥手";
        String englishName = ActionNameUtils.chineseToEnglish(chineseName);
        Log.d(TAG, String.format("转换测试: '%s' -> '%s'", chineseName, englishName));
        
        // 4. 测试反向转换
        String backToChinese = ActionNameUtils.englishToChinese(englishName);
        Log.d(TAG, String.format("反向转换: '%s' -> '%s'", englishName, backToChinese));
        
        // 5. 测试匹配
        boolean isMatch = ActionNameUtils.isNameMatch("左臂挥手", "arm_movement_left_arm_wave");
        Log.d(TAG, String.format("匹配测试: %s", isMatch));
    }
}