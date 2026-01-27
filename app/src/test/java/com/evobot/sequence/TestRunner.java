package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

/**
 * 测试运行器
 * 用于执行各种功能测试
 */
public class TestRunner {
    
    private static final String TAG = "TestRunner";
    
    private Context context;
    
    public TestRunner(Context context) {
        this.context = context;
    }
    
    /**
     * 运行所有测试
     */
    public void runAllTests() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "开始执行动态映射功能测试");
        Log.d(TAG, "========================================");
        
        try {
            // 1. 动态映射测试
            runDynamicMappingTests();
            
            // 2. 集成测试
            runIntegrationTests();
            
            // 3. 性能测试
            runPerformanceTests();
            
            Log.d(TAG, "========================================");
            Log.d(TAG, "所有测试执行完成 ✅");
            Log.d(TAG, "========================================");
            
        } catch (Exception e) {
            Log.e(TAG, "========================================");
            Log.e(TAG, "测试执行失败 ❌", e);
            Log.e(TAG, "========================================");
        }
    }
    
    /**
     * 运行动态映射测试
     */
    private void runDynamicMappingTests() {
        Log.d(TAG, "--- 执行动态映射测试 ---");
        
        DynamicMappingTest mappingTest = new DynamicMappingTest(context);
        mappingTest.runAllTests();
        
        Log.d(TAG, "动态映射测试完成 ✅");
    }
    
    /**
     * 运行集成测试
     */
    private void runIntegrationTests() {
        Log.d(TAG, "--- 执行集成测试 ---");
        
        try {
            // 测试ActionLibraryUpdater集成
            testActionLibraryUpdaterIntegration();
            
            // 测试EvoBotSequencePlayer集成
            testEvoBotSequencePlayerIntegration();
            
            Log.d(TAG, "集成测试完成 ✅");
            
        } catch (Exception e) {
            Log.e(TAG, "集成测试失败", e);
            throw e;
        }
    }
    
    /**
     * 测试ActionLibraryUpdater集成
     */
    private void testActionLibraryUpdaterIntegration() {
        Log.d(TAG, "测试ActionLibraryUpdater集成...");
        
        // 清除现有映射
        ActionNameUtils.clearMappings();
        int initialCount = ActionNameUtils.getMappingCount();
        Log.d(TAG, "初始映射数量: " + initialCount);
        
        // 创建ActionLibraryUpdater实例
        ActionLibraryConfig config = new ActionLibraryConfig.Builder()
            .setBaseUrl("http://test.example.com")
            .setRobotId("TEST-ROBOT-001")
            .setApiKey("test-key")
            .build();
            
        ActionLibraryUpdater updater = new ActionLibraryUpdater(
            context, config, ActionLibraryUpdater.StorageLocation.INTERNAL_FILES);
        
        // 测试初始化映射（这会在后台线程中执行）
        updater.initializeMappings();
        
        // 等待一段时间让后台线程完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Log.d(TAG, "ActionLibraryUpdater集成测试完成");
    }
    
    /**
     * 测试EvoBotSequencePlayer集成
     */
    private void testEvoBotSequencePlayerIntegration() {
        Log.d(TAG, "测试EvoBotSequencePlayer集成...");
        
        // 创建配置
        ActionLibraryConfig config = new ActionLibraryConfig.Builder()
            .setBaseUrl("http://test.example.com")
            .setRobotId("TEST-ROBOT-001")
            .setApiKey("test-key")
            .build();
        
        // 创建播放器实例（这会触发映射初始化）
        EvoBotSequencePlayer player = new EvoBotSequencePlayer(
            context, config, ActionLibraryUpdater.StorageLocation.INTERNAL_FILES);
        
        // 等待初始化完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Log.d(TAG, "当前映射数量: " + ActionNameUtils.getMappingCount());
        
        // 清理资源
        player.release();
        
        Log.d(TAG, "EvoBotSequencePlayer集成测试完成");
    }
    
    /**
     * 运行性能测试
     */
    private void runPerformanceTests() {
        Log.d(TAG, "--- 执行性能测试 ---");
        
        DynamicMappingTest mappingTest = new DynamicMappingTest(context);
        mappingTest.runPerformanceTest();
        
        Log.d(TAG, "性能测试完成 ✅");
    }
    
    /**
     * 快速验证测试
     * 用于快速验证核心功能是否正常
     */
    public void runQuickValidation() {
        Log.d(TAG, "=== 快速验证测试 ===");
        
        try {
            // 清除映射
            ActionNameUtils.clearMappings();
            
            // 添加测试映射
            ActionNameUtils.addMapping("左臂挥手", "arm_movement_left_arm_wave");
            ActionNameUtils.addMapping("右臂挥手", "arm_movement_right_arm_wave");
            
            // 验证映射数量
            int count = ActionNameUtils.getMappingCount();
            Log.d(TAG, "映射数量: " + count);
            assert count == 2 : "映射数量不正确";
            
            // 验证转换功能
            String english = ActionNameUtils.chineseToEnglish("左臂挥手");
            String chinese = ActionNameUtils.englishToChinese("arm_movement_right_arm_wave");
            
            Log.d(TAG, "转换测试: 左臂挥手 -> " + english);
            Log.d(TAG, "转换测试: arm_movement_right_arm_wave -> " + chinese);
            
            assert "arm_movement_left_arm_wave".equals(english) : "中文转英文失败";
            assert "右臂挥手".equals(chinese) : "英文转中文失败";
            
            // 验证匹配功能
            boolean match = ActionNameUtils.isNameMatch("左臂挥手", "arm_movement_left_arm_wave");
            Log.d(TAG, "匹配测试: " + match);
            assert match : "名称匹配失败";
            
            Log.d(TAG, "✅ 快速验证测试通过");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 快速验证测试失败", e);
            throw e;
        }
    }
    
    /**
     * 测试实际使用场景
     */
    public void testRealWorldScenario() {
        Log.d(TAG, "=== 实际使用场景测试 ===");
        
        try {
            // 模拟从API获取动作列表并建立映射
            simulateApiResponse();
            
            // 模拟文件下载并建立映射
            simulateFileDownload();
            
            // 模拟用户调用
            simulateUserCalls();
            
            Log.d(TAG, "✅ 实际使用场景测试通过");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 实际使用场景测试失败", e);
            throw e;
        }
    }
    
    /**
     * 模拟API响应处理
     */
    private void simulateApiResponse() {
        Log.d(TAG, "模拟API响应处理...");
        
        // 模拟从API响应中解析动作信息
        ActionInfo action1 = new ActionInfo(1, "左臂挥手", "arm_movement_left_arm_wave");
        ActionInfo action2 = new ActionInfo(2, "右臂挥手", "arm_movement_right_arm_wave");
        ActionInfo action3 = new ActionInfo(3, "点头确认", "head_nod_confirm");
        
        // 建立映射
        ActionNameUtils.addMapping(action1.name, action1.englishName);
        ActionNameUtils.addMapping(action2.name, action2.englishName);
        ActionNameUtils.addMapping(action3.name, action3.englishName);
        
        Log.d(TAG, "API响应处理完成，映射数量: " + ActionNameUtils.getMappingCount());
    }
    
    /**
     * 模拟文件下载处理
     */
    private void simulateFileDownload() {
        Log.d(TAG, "模拟文件下载处理...");
        
        // 模拟解析下载的文件
        SequenceData data1 = createMockSequenceData("微笑打招呼");
        SequenceData data2 = createMockSequenceData("摇头拒绝");
        
        // 从文件建立映射
        ActionNameUtils.addMappingFromFile("smile_greeting.ebs", data1);
        ActionNameUtils.addMappingFromFile("head_shake_refuse.ebs", data2);
        
        Log.d(TAG, "文件下载处理完成，映射数量: " + ActionNameUtils.getMappingCount());
    }
    
    /**
     * 模拟用户调用
     */
    private void simulateUserCalls() {
        Log.d(TAG, "模拟用户调用...");
        
        // 用户使用中文名称
        String english1 = ActionNameUtils.chineseToEnglish("左臂挥手");
        Log.d(TAG, "用户调用: 左臂挥手 -> " + english1);
        
        // 用户使用英文名称
        String chinese1 = ActionNameUtils.englishToChinese("head_nod_confirm");
        Log.d(TAG, "用户调用: head_nod_confirm -> " + chinese1);
        
        // 用户进行名称匹配
        boolean match = ActionNameUtils.isNameMatch("微笑打招呼", "smile_greeting");
        Log.d(TAG, "用户匹配: 微笑打招呼 <-> smile_greeting = " + match);
        
        Log.d(TAG, "用户调用模拟完成");
    }
    
    /**
     * 创建模拟的SequenceData
     */
    private SequenceData createMockSequenceData(String name) {
        SequenceData data = new SequenceData();
        data.name = name;
        data.sampleRate = 40.0f;
        data.totalDuration = 3.0f;
        data.totalFrames = 120;
        data.compiledAt = (int) (System.currentTimeMillis() / 1000);
        
        // 创建简单的序列数据
        data.leftArmSequence = new int[data.totalFrames][SequenceData.JOINTS_PER_ARM];
        data.rightArmSequence = new int[data.totalFrames][SequenceData.JOINTS_PER_ARM];
        
        return data;
    }
}