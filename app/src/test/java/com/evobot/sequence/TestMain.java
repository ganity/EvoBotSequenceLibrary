package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

/**
 * 测试入口类
 * 提供简单的测试接口
 */
public class TestMain {
    
    private static final String TAG = "TestMain";
    
    /**
     * 运行所有测试
     * 
     * @param context Android上下文
     */
    public static void runAllTests(Context context) {
        Log.d(TAG, "开始执行所有测试...");
        
        // 运行基础测试
        TestRunner runner = new TestRunner(context);
        runner.runAllTests();
        
        // 运行HTTP测试
        Log.d(TAG, "\n=== 开始HTTP测试 ===");
        HttpTestRunner.runHttpTests(context);
        
        Log.d(TAG, "所有测试执行完成");
    }
    
    /**
     * 快速验证测试
     * 
     * @param context Android上下文
     */
    public static void quickValidation(Context context) {
        Log.d(TAG, "开始快速验证...");
        
        TestRunner runner = new TestRunner(context);
        runner.runQuickValidation();
        
        // 快速HTTP测试
        Log.d(TAG, "\n=== 快速HTTP验证 ===");
        runQuickHttpTests(context);
        
        Log.d(TAG, "快速验证完成");
    }
    
    /**
     * 运行快速HTTP测试
     */
    public static void runQuickHttpTests(Context context) {
        Log.d(TAG, "=== 开始快速HTTP测试 ===");
        
        try {
            // 快速连接测试
            boolean connectionOk = HttpTestRunner.quickConnectionTest(context);
            Log.d(TAG, "连接测试: " + (connectionOk ? "✅ 通过" : "❌ 失败"));
            
            // 映射建立测试
            int mappingCount = HttpTestRunner.testMappingCreation(context);
            Log.d(TAG, "映射建立测试: " + (mappingCount > 0 ? "✅ 通过 (" + mappingCount + "个映射)" : "❌ 失败"));
            
            // 单个动作下载测试
            boolean downloadOk = HttpTestRunner.testSingleActionDownload(context);
            Log.d(TAG, "动作下载测试: " + (downloadOk ? "✅ 通过" : "❌ 失败"));
            
            Log.d(TAG, "=== 快速HTTP测试完成 ===");
            
        } catch (Exception e) {
            Log.e(TAG, "快速HTTP测试失败", e);
        }
    }
    
    /**
     * 运行完整HTTP测试
     */
    public static void runFullHttpTests(Context context) {
        Log.d(TAG, "=== 开始完整HTTP测试 ===");
        HttpTestRunner.runHttpTests(context);
        Log.d(TAG, "=== 完整HTTP测试完成 ===");
    }
    
    /**
     * 实际使用场景测试
     * 
     * @param context Android上下文
     */
    public static void testRealWorldScenario(Context context) {
        Log.d(TAG, "开始实际场景测试...");
        
        TestRunner runner = new TestRunner(context);
        runner.testRealWorldScenario();
        
        Log.d(TAG, "实际场景测试完成");
    }
    
    /**
     * 演示动态映射功能
     * 
     * @param context Android上下文
     */
    public static void demonstrateDynamicMapping(Context context) {
        Log.d(TAG, "=== 动态映射功能演示 ===");
        
        try {
            // 1. 清除现有映射
            ActionNameUtils.clearMappings();
            Log.d(TAG, "1. 清除映射，当前数量: " + ActionNameUtils.getMappingCount());
            
            // 2. 模拟从API获取动作列表
            Log.d(TAG, "2. 模拟从API获取动作列表...");
            ActionNameUtils.addMapping("左臂挥手", "arm_movement_left_arm_wave");
            ActionNameUtils.addMapping("右臂挥手", "arm_movement_right_arm_wave");
            ActionNameUtils.addMapping("点头确认", "head_nod_confirm");
            Log.d(TAG, "   API映射建立完成，当前数量: " + ActionNameUtils.getMappingCount());
            
            // 3. 模拟下载文件并解析
            Log.d(TAG, "3. 模拟下载文件并解析...");
            SequenceData data1 = createMockSequenceData("微笑打招呼");
            SequenceData data2 = createMockSequenceData("摇头拒绝");
            ActionNameUtils.addMappingFromFile("smile_greeting.ebs", data1);
            ActionNameUtils.addMappingFromFile("head_shake_refuse.ebs", data2);
            Log.d(TAG, "   文件映射建立完成，当前数量: " + ActionNameUtils.getMappingCount());
            
            // 4. 演示各种使用方式
            Log.d(TAG, "4. 演示各种使用方式:");
            
            // 中文转英文
            String english1 = ActionNameUtils.chineseToEnglish("左臂挥手");
            Log.d(TAG, "   中文转英文: 左臂挥手 -> " + english1);
            
            // 英文转中文
            String chinese1 = ActionNameUtils.englishToChinese("head_nod_confirm");
            Log.d(TAG, "   英文转中文: head_nod_confirm -> " + chinese1);
            
            // 名称匹配
            boolean match1 = ActionNameUtils.isNameMatch("微笑打招呼", "smile_greeting");
            Log.d(TAG, "   名称匹配: 微笑打招呼 <-> smile_greeting = " + match1);
            
            // 类型判断
            boolean isChinese = ActionNameUtils.isChineseName("左臂挥手");
            boolean isEnglish = ActionNameUtils.isEnglishName("arm_movement_left_arm_wave");
            Log.d(TAG, "   类型判断: '左臂挥手'是中文=" + isChinese + ", 'arm_movement_left_arm_wave'是英文=" + isEnglish);
            
            // 5. 演示在EvoBotSequencePlayer中的使用
            Log.d(TAG, "5. 演示在播放器中的使用:");
            Log.d(TAG, "   player.play(\"左臂挥手\", listener);  // 使用中文名称");
            Log.d(TAG, "   player.play(\"arm_movement_left_arm_wave\", listener);  // 使用英文名称");
            Log.d(TAG, "   两种方式都能正确工作，系统会自动处理名称匹配");
            
            Log.d(TAG, "=== 动态映射功能演示完成 ===");
            
        } catch (Exception e) {
            Log.e(TAG, "动态映射演示失败", e);
        }
    }
    
    /**
     * 创建模拟的SequenceData
     */
    private static SequenceData createMockSequenceData(String name) {
        SequenceData data = new SequenceData();
        data.name = name;
        data.sampleRate = 40.0f;
        data.totalDuration = 3.0f;
        data.totalFrames = 120;
        data.compiledAt = (int) (System.currentTimeMillis() / 1000);
        
        // 创建简单的序列数据
        data.leftArmSequence = new int[data.totalFrames][SequenceData.JOINTS_PER_ARM];
        data.rightArmSequence = new int[data.totalFrames][SequenceData.JOINTS_PER_ARM];
        
        // 填充测试数据
        for (int frame = 0; frame < data.totalFrames; frame++) {
            for (int joint = 0; joint < SequenceData.JOINTS_PER_ARM; joint++) {
                data.leftArmSequence[frame][joint] = 2048;  // 中位值
                data.rightArmSequence[frame][joint] = 2048;
            }
        }
        
        return data;
    }
    
    /**
     * 测试特定功能
     * 
     * @param context Android上下文
     * @param testName 测试名称
     */
    public static void runSpecificTest(Context context, String testName) {
        Log.d(TAG, "运行特定测试: " + testName);
        
        switch (testName.toLowerCase()) {
            case "mapping":
                testMappingFunctionality();
                break;
            case "matching":
                testMatchingFunctionality();
                break;
            case "file":
                testFileFunctionality(context);
                break;
            case "performance":
                testPerformance();
                break;
            case "http":
                runFullHttpTests(context);
                break;
            case "http_quick":
                runQuickHttpTests(context);
                break;
            case "connection":
                boolean connected = HttpTestRunner.quickConnectionTest(context);
                Log.d(TAG, "连接测试结果: " + (connected ? "成功" : "失败"));
                break;
            default:
                Log.w(TAG, "未知的测试名称: " + testName);
                Log.d(TAG, "可用的测试: mapping, matching, file, performance, http, http_quick, connection");
                break;
        }
    }
    
    private static void testMappingFunctionality() {
        Log.d(TAG, "--- 测试映射功能 ---");
        
        ActionNameUtils.clearMappings();
        ActionNameUtils.addMapping("测试动作", "test_action");
        
        String result = ActionNameUtils.chineseToEnglish("测试动作");
        Log.d(TAG, "映射结果: " + result);
        
        assert "test_action".equals(result) : "映射功能测试失败";
        Log.d(TAG, "✅ 映射功能测试通过");
    }
    
    private static void testMatchingFunctionality() {
        Log.d(TAG, "--- 测试匹配功能 ---");
        
        ActionNameUtils.clearMappings();
        ActionNameUtils.addMapping("测试匹配", "test_matching");
        
        boolean match = ActionNameUtils.isNameMatch("测试匹配", "test_matching");
        Log.d(TAG, "匹配结果: " + match);
        
        assert match : "匹配功能测试失败";
        Log.d(TAG, "✅ 匹配功能测试通过");
    }
    
    private static void testFileFunctionality(Context context) {
        Log.d(TAG, "--- 测试文件功能 ---");
        
        SequenceData data = createMockSequenceData("文件测试");
        ActionNameUtils.addMappingFromFile("file_test.ebs", data);
        
        String result = ActionNameUtils.chineseToEnglish("文件测试");
        Log.d(TAG, "文件映射结果: " + result);
        
        assert "file_test".equals(result) : "文件功能测试失败";
        Log.d(TAG, "✅ 文件功能测试通过");
    }
    
    private static void testPerformance() {
        Log.d(TAG, "--- 测试性能 ---");
        
        ActionNameUtils.clearMappings();
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            ActionNameUtils.addMapping("动作" + i, "action_" + i);
        }
        long addTime = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            ActionNameUtils.chineseToEnglish("动作" + i);
        }
        long searchTime = System.currentTimeMillis() - startTime;
        
        Log.d(TAG, "添加100个映射耗时: " + addTime + "ms");
        Log.d(TAG, "查找100次耗时: " + searchTime + "ms");
        Log.d(TAG, "✅ 性能测试完成");
    }
}