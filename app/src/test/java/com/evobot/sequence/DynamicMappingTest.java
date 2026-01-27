package com.evobot.sequence;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 动态映射功能测试类
 * 测试中英文名称动态映射机制的正确性
 */
public class DynamicMappingTest {
    
    private static final String TAG = "DynamicMappingTest";
    
    private Context context;
    private File testDir;
    
    public DynamicMappingTest(Context context) {
        this.context = context;
        this.testDir = new File(context.getFilesDir(), "test_actions");
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
    }
    
    /**
     * 运行所有测试
     */
    public void runAllTests() {
        Log.d(TAG, "=== 开始动态映射测试 ===");
        
        try {
            // 清理测试环境
            setupTestEnvironment();
            
            // 测试1: 基本映射功能
            testBasicMapping();
            
            // 测试2: 文件映射功能
            testFileMapping();
            
            // 测试3: 名称匹配功能
            testNameMatching();
            
            // 测试4: 文件查找功能
            testFileSearching();
            
            // 测试5: 映射管理功能
            testMappingManagement();
            
            // 测试6: 边界情况测试
            testEdgeCases();
            
            Log.d(TAG, "=== 所有测试完成 ===");
            
        } catch (Exception e) {
            Log.e(TAG, "测试执行失败", e);
        } finally {
            // 清理测试环境
            cleanupTestEnvironment();
        }
    }
    
    /**
     * 设置测试环境
     */
    private void setupTestEnvironment() {
        Log.d(TAG, "设置测试环境...");
        
        // 清除现有映射
        ActionNameUtils.clearMappings();
        
        // 清理测试目录
        File[] files = testDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        
        Log.d(TAG, "测试环境设置完成");
    }
    
    /**
     * 清理测试环境
     */
    private void cleanupTestEnvironment() {
        Log.d(TAG, "清理测试环境...");
        
        // 清理测试文件
        File[] files = testDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        
        // 清除映射
        ActionNameUtils.clearMappings();
        
        Log.d(TAG, "测试环境清理完成");
    }
    
    /**
     * 测试1: 基本映射功能
     */
    private void testBasicMapping() {
        Log.d(TAG, "--- 测试1: 基本映射功能 ---");
        
        // 初始状态检查
        int initialCount = ActionNameUtils.getMappingCount();
        Log.d(TAG, "初始映射数量: " + initialCount);
        assert initialCount == 0 : "初始映射数量应为0";
        
        // 添加映射
        ActionNameUtils.addMapping("左臂挥手", "arm_movement_left_arm_wave");
        ActionNameUtils.addMapping("右臂挥手", "arm_movement_right_arm_wave");
        ActionNameUtils.addMapping("点头确认", "head_nod_confirm");
        
        // 检查映射数量
        int afterAddCount = ActionNameUtils.getMappingCount();
        Log.d(TAG, "添加映射后数量: " + afterAddCount);
        assert afterAddCount == 3 : "映射数量应为3";
        
        // 测试中文转英文
        String english1 = ActionNameUtils.chineseToEnglish("左臂挥手");
        Log.d(TAG, "中文转英文: 左臂挥手 -> " + english1);
        assert "arm_movement_left_arm_wave".equals(english1) : "中文转英文失败";
        
        // 测试英文转中文
        String chinese1 = ActionNameUtils.englishToChinese("arm_movement_right_arm_wave");
        Log.d(TAG, "英文转中文: arm_movement_right_arm_wave -> " + chinese1);
        assert "右臂挥手".equals(chinese1) : "英文转中文失败";
        
        // 测试不存在的映射
        String notFound = ActionNameUtils.chineseToEnglish("不存在的动作");
        Log.d(TAG, "不存在的映射: " + notFound);
        assert "不存在的动作".equals(notFound) : "不存在的映射应返回原值";
        
        Log.d(TAG, "✅ 基本映射功能测试通过");
    }
    
    /**
     * 测试2: 文件映射功能
     */
    private void testFileMapping() {
        Log.d(TAG, "--- 测试2: 文件映射功能 ---");
        
        try {
            // 创建模拟的SequenceData
            SequenceData mockData = createMockSequenceData("左臂挥手动作");
            
            // 测试从文件建立映射
            ActionNameUtils.addMappingFromFile("arm_movement_left_arm_wave.ebs", mockData);
            
            // 验证映射是否建立
            String englishName = ActionNameUtils.chineseToEnglish("左臂挥手动作");
            Log.d(TAG, "从文件建立的映射: 左臂挥手动作 -> " + englishName);
            assert "arm_movement_left_arm_wave".equals(englishName) : "文件映射建立失败";
            
            // 测试文件名提取
            String extractedName = ActionNameUtils.extractActionNameFromFileName("test_action.ebs");
            Log.d(TAG, "提取的文件名: " + extractedName);
            assert "test_action".equals(extractedName) : "文件名提取失败";
            
            Log.d(TAG, "✅ 文件映射功能测试通过");
            
        } catch (Exception e) {
            Log.e(TAG, "文件映射测试失败", e);
            throw new RuntimeException("文件映射测试失败", e);
        }
    }
    
    /**
     * 测试3: 名称匹配功能
     */
    private void testNameMatching() {
        Log.d(TAG, "--- 测试3: 名称匹配功能 ---");
        
        // 添加测试映射
        ActionNameUtils.addMapping("测试动作", "test_action");
        
        // 测试各种匹配情况
        boolean match1 = ActionNameUtils.isNameMatch("测试动作", "test_action");
        Log.d(TAG, "中英文匹配: " + match1);
        assert match1 : "中英文匹配失败";
        
        boolean match2 = ActionNameUtils.isNameMatch("test_action", "测试动作");
        Log.d(TAG, "英中文匹配: " + match2);
        assert match2 : "英中文匹配失败";
        
        boolean match3 = ActionNameUtils.isNameMatch("测试动作", "测试动作");
        Log.d(TAG, "相同名称匹配: " + match3);
        assert match3 : "相同名称匹配失败";
        
        boolean match4 = ActionNameUtils.isNameMatch("test_action", "test_action");
        Log.d(TAG, "相同英文匹配: " + match4);
        assert match4 : "相同英文匹配失败";
        
        boolean match5 = ActionNameUtils.isNameMatch("不存在", "unknown");
        Log.d(TAG, "不存在映射匹配: " + match5);
        assert !match5 : "不存在映射不应匹配";
        
        // 测试名称类型判断
        boolean isChinese = ActionNameUtils.isChineseName("测试动作");
        boolean isEnglish = ActionNameUtils.isEnglishName("test_action");
        Log.d(TAG, "名称类型判断: 中文=" + isChinese + ", 英文=" + isEnglish);
        assert isChinese && isEnglish : "名称类型判断失败";
        
        Log.d(TAG, "✅ 名称匹配功能测试通过");
    }
    
    /**
     * 测试4: 文件查找功能
     */
    private void testFileSearching() {
        Log.d(TAG, "--- 测试4: 文件查找功能 ---");
        
        try {
            // 创建测试文件
            File testFile1 = new File(testDir, "arm_movement_left_arm_wave.ebs");
            File testFile2 = new File(testDir, "head_nod_confirm.ebs");
            File testFile3 = new File(testDir, "smile_greeting.ebs");
            
            createTestFile(testFile1, "左臂挥手");
            createTestFile(testFile2, "点头确认");
            createTestFile(testFile3, "微笑打招呼");
            
            // 建立映射
            ActionNameUtils.addMapping("左臂挥手", "arm_movement_left_arm_wave");
            ActionNameUtils.addMapping("点头确认", "head_nod_confirm");
            ActionNameUtils.addMapping("微笑打招呼", "smile_greeting");
            
            File[] availableFiles = testDir.listFiles();
            
            // 测试用中文名查找
            File found1 = ActionNameUtils.findMatchingFile("左臂挥手", availableFiles);
            Log.d(TAG, "中文名查找结果: " + (found1 != null ? found1.getName() : "null"));
            assert found1 != null && found1.getName().equals("arm_movement_left_arm_wave.ebs") : "中文名查找失败";
            
            // 测试用英文名查找
            File found2 = ActionNameUtils.findMatchingFile("head_nod_confirm", availableFiles);
            Log.d(TAG, "英文名查找结果: " + (found2 != null ? found2.getName() : "null"));
            assert found2 != null && found2.getName().equals("head_nod_confirm.ebs") : "英文名查找失败";
            
            // 测试查找不存在的文件
            File found3 = ActionNameUtils.findMatchingFile("不存在的动作", availableFiles);
            Log.d(TAG, "不存在文件查找结果: " + (found3 != null ? found3.getName() : "null"));
            assert found3 == null : "不存在的文件不应被找到";
            
            Log.d(TAG, "✅ 文件查找功能测试通过");
            
        } catch (Exception e) {
            Log.e(TAG, "文件查找测试失败", e);
            throw new RuntimeException("文件查找测试失败", e);
        }
    }
    
    /**
     * 测试5: 映射管理功能
     */
    private void testMappingManagement() {
        Log.d(TAG, "--- 测试5: 映射管理功能 ---");
        
        // 添加一些映射
        ActionNameUtils.addMapping("动作1", "action1");
        ActionNameUtils.addMapping("动作2", "action2");
        ActionNameUtils.addMapping("动作3", "action3");
        
        int count = ActionNameUtils.getMappingCount();
        Log.d(TAG, "添加映射后数量: " + count);
        assert count >= 3 : "映射数量不正确";
        
        // 清除映射
        ActionNameUtils.clearMappings();
        int afterClearCount = ActionNameUtils.getMappingCount();
        Log.d(TAG, "清除映射后数量: " + afterClearCount);
        assert afterClearCount == 0 : "清除映射后数量应为0";
        
        Log.d(TAG, "✅ 映射管理功能测试通过");
    }
    
    /**
     * 测试6: 边界情况测试
     */
    private void testEdgeCases() {
        Log.d(TAG, "--- 测试6: 边界情况测试 ---");
        
        // 测试null值处理
        ActionNameUtils.addMapping(null, "test");
        ActionNameUtils.addMapping("test", null);
        ActionNameUtils.addMapping(null, null);
        
        String result1 = ActionNameUtils.chineseToEnglish(null);
        String result2 = ActionNameUtils.englishToChinese(null);
        boolean match = ActionNameUtils.isNameMatch(null, "test");
        
        Log.d(TAG, "null值处理结果: " + result1 + ", " + result2 + ", " + match);
        assert result1 == null && result2 == null && !match : "null值处理失败";
        
        // 测试空字符串处理
        ActionNameUtils.addMapping("", "test");
        ActionNameUtils.addMapping("test", "");
        
        String result3 = ActionNameUtils.chineseToEnglish("");
        String result4 = ActionNameUtils.englishToChinese("");
        
        Log.d(TAG, "空字符串处理结果: '" + result3 + "', '" + result4 + "'");
        assert "".equals(result3) && "".equals(result4) : "空字符串处理失败";
        
        // 测试特殊字符
        ActionNameUtils.addMapping("特殊字符!@#", "special_chars");
        String specialResult = ActionNameUtils.chineseToEnglish("特殊字符!@#");
        Log.d(TAG, "特殊字符处理: " + specialResult);
        assert "special_chars".equals(specialResult) : "特殊字符处理失败";
        
        Log.d(TAG, "✅ 边界情况测试通过");
    }
    
    /**
     * 创建模拟的SequenceData
     */
    private SequenceData createMockSequenceData(String name) {
        SequenceData data = new SequenceData();
        data.name = name;
        data.sampleRate = 40.0f;
        data.totalDuration = 5.0f;
        data.totalFrames = 200;
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
     * 创建测试文件
     */
    private void createTestFile(File file, String actionName) throws IOException {
        // 创建简单的测试文件内容
        // 实际应用中这里应该是EBS格式的二进制数据
        String content = "Mock EBS file for: " + actionName;
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes());
        }
        
        Log.d(TAG, "创建测试文件: " + file.getName());
    }
    
    /**
     * 运行性能测试
     */
    public void runPerformanceTest() {
        Log.d(TAG, "=== 性能测试 ===");
        
        // 清除现有映射
        ActionNameUtils.clearMappings();
        
        // 添加大量映射
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            ActionNameUtils.addMapping("动作" + i, "action_" + i);
        }
        long addTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "添加1000个映射耗时: " + addTime + "ms");
        
        // 测试查找性能
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            String result = ActionNameUtils.chineseToEnglish("动作" + (i % 1000));
        }
        long searchTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "查找1000次耗时: " + searchTime + "ms");
        
        // 测试匹配性能
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            boolean match = ActionNameUtils.isNameMatch("动作" + (i % 1000), "action_" + (i % 1000));
        }
        long matchTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "匹配1000次耗时: " + matchTime + "ms");
        
        Log.d(TAG, "✅ 性能测试完成");
    }
}