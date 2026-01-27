import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.io.File;

/**
 * ActionNameUtils的简化测试版本（不依赖Android）
 */
class ActionNameUtils {
    
    private static final String TAG = "ActionNameUtils";
    
    // 动态维护的中英文映射表
    private static final Map<String, String> CHINESE_TO_ENGLISH = new ConcurrentHashMap<>();
    private static final Map<String, String> ENGLISH_TO_CHINESE = new ConcurrentHashMap<>();
    
    // 文件名到中文名的映射（从解析的动作文件中获取）
    private static final Map<String, String> FILENAME_TO_CHINESE = new ConcurrentHashMap<>();
    
    /**
     * 判断是否为英文名称
     */
    public static boolean isEnglishName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        // 检查是否包含中文字符
        for (char c : name.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return false;
            }
        }
        
        // 英文名称通常包含下划线
        return name.contains("_") && name.matches("^[a-z0-9_]+$");
    }
    
    /**
     * 判断是否为中文名称
     */
    public static boolean isChineseName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        // 检查是否包含中文字符
        for (char c : name.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 中文名称转英文名称
     */
    public static String chineseToEnglish(String chineseName) {
        if (chineseName == null || chineseName.isEmpty()) {
            return chineseName;
        }
        
        return CHINESE_TO_ENGLISH.getOrDefault(chineseName, chineseName);
    }
    
    /**
     * 英文名称转中文名称
     */
    public static String englishToChinese(String englishName) {
        if (englishName == null || englishName.isEmpty()) {
            return englishName;
        }
        
        return ENGLISH_TO_CHINESE.getOrDefault(englishName, englishName);
    }
    
    /**
     * 动态添加映射关系
     */
    public static void addMapping(String chineseName, String englishName) {
        if (chineseName != null && !chineseName.isEmpty() && 
            englishName != null && !englishName.isEmpty()) {
            
            CHINESE_TO_ENGLISH.put(chineseName, englishName);
            ENGLISH_TO_CHINESE.put(englishName, chineseName);
            
            System.out.println(String.format("添加映射: '%s' <-> '%s'", chineseName, englishName));
        }
    }
    
    /**
     * 清除所有映射
     */
    public static void clearMappings() {
        CHINESE_TO_ENGLISH.clear();
        ENGLISH_TO_CHINESE.clear();
        FILENAME_TO_CHINESE.clear();
        System.out.println("清除所有映射");
    }
    
    /**
     * 获取当前映射数量
     */
    public static int getMappingCount() {
        return CHINESE_TO_ENGLISH.size();
    }
    
    /**
     * 检查两个动作名称是否匹配
     */
    public static boolean isNameMatch(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }
        
        if (name1.equals(name2)) {
            return true;
        }
        
        // 尝试转换后比较
        String english1 = chineseToEnglish(name1);
        String english2 = chineseToEnglish(name2);
        if (english1.equals(english2)) {
            return true;
        }
        
        String chinese1 = englishToChinese(name1);
        String chinese2 = englishToChinese(name2);
        if (chinese1.equals(chinese2)) {
            return true;
        }
        
        // 交叉匹配
        if (english1.equals(name2) || chinese1.equals(name2) ||
            english2.equals(name1) || chinese2.equals(name1)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 从文件名提取动作名称
     */
    public static String extractActionNameFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }
        
        if (fileName.endsWith(".ebs")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        
        return fileName;
    }
}

/**
 * 测试类
 */
public class ActionNameUtilsTest {
    
    private static int testCount = 0;
    private static int passCount = 0;
    
    public static void main(String[] args) {
        System.out.println("=== ActionNameUtils 功能测试 ===");
        
        // 运行所有测试
        testBasicMapping();
        testNameMatching();
        testNameTypeDetection();
        testEdgeCases();
        testPerformance();
        
        // 输出测试结果
        System.out.println("\n=== 测试结果 ===");
        System.out.println("总测试数: " + testCount);
        System.out.println("通过测试: " + passCount);
        System.out.println("失败测试: " + (testCount - passCount));
        
        if (passCount == testCount) {
            System.out.println("✅ 所有测试通过！");
        } else {
            System.out.println("❌ 有测试失败！");
            System.exit(1);
        }
    }
    
    /**
     * 测试基本映射功能
     */
    private static void testBasicMapping() {
        System.out.println("\n--- 测试基本映射功能 ---");
        
        // 清除现有映射
        ActionNameUtils.clearMappings();
        
        // 测试初始状态
        assertEquals("初始映射数量应为0", 0, ActionNameUtils.getMappingCount());
        
        // 添加映射
        ActionNameUtils.addMapping("左臂挥手", "arm_movement_left_arm_wave");
        ActionNameUtils.addMapping("右臂挥手", "arm_movement_right_arm_wave");
        ActionNameUtils.addMapping("点头确认", "head_nod_confirm");
        
        // 测试映射数量
        assertEquals("映射数量应为3", 3, ActionNameUtils.getMappingCount());
        
        // 测试中文转英文
        assertEquals("中文转英文", "arm_movement_left_arm_wave", 
                    ActionNameUtils.chineseToEnglish("左臂挥手"));
        
        // 测试英文转中文
        assertEquals("英文转中文", "右臂挥手", 
                    ActionNameUtils.englishToChinese("arm_movement_right_arm_wave"));
        
        // 测试不存在的映射
        assertEquals("不存在的映射应返回原值", "不存在的动作", 
                    ActionNameUtils.chineseToEnglish("不存在的动作"));
    }
    
    /**
     * 测试名称匹配功能
     */
    private static void testNameMatching() {
        System.out.println("\n--- 测试名称匹配功能 ---");
        
        // 添加测试映射
        ActionNameUtils.addMapping("测试动作", "test_action");
        
        // 测试各种匹配情况
        assertTrue("中英文应该匹配", 
                  ActionNameUtils.isNameMatch("测试动作", "test_action"));
        
        assertTrue("英中文应该匹配", 
                  ActionNameUtils.isNameMatch("test_action", "测试动作"));
        
        assertTrue("相同名称应该匹配", 
                  ActionNameUtils.isNameMatch("测试动作", "测试动作"));
        
        assertTrue("相同英文应该匹配", 
                  ActionNameUtils.isNameMatch("test_action", "test_action"));
        
        assertFalse("不存在映射不应该匹配", 
                   ActionNameUtils.isNameMatch("不存在", "unknown"));
    }
    
    /**
     * 测试名称类型检测
     */
    private static void testNameTypeDetection() {
        System.out.println("\n--- 测试名称类型检测 ---");
        
        // 测试中文名称检测
        assertTrue("应该识别中文名称", ActionNameUtils.isChineseName("左臂挥手"));
        assertTrue("应该识别中文名称", ActionNameUtils.isChineseName("测试动作"));
        assertFalse("不应该识别英文为中文", ActionNameUtils.isChineseName("test_action"));
        
        // 测试英文名称检测
        assertTrue("应该识别英文名称", ActionNameUtils.isEnglishName("arm_movement_left_arm_wave"));
        assertTrue("应该识别英文名称", ActionNameUtils.isEnglishName("test_action"));
        assertFalse("不应该识别中文为英文", ActionNameUtils.isEnglishName("左臂挥手"));
        
        // 测试文件名提取
        assertEquals("文件名提取", "test_action", 
                    ActionNameUtils.extractActionNameFromFileName("test_action.ebs"));
        assertEquals("无扩展名文件名", "test_action", 
                    ActionNameUtils.extractActionNameFromFileName("test_action"));
    }
    
    /**
     * 测试边界情况
     */
    private static void testEdgeCases() {
        System.out.println("\n--- 测试边界情况 ---");
        
        // 测试null值处理
        ActionNameUtils.addMapping(null, "test");
        ActionNameUtils.addMapping("test", null);
        ActionNameUtils.addMapping(null, null);
        
        assertEquals("null转换应返回null", null, ActionNameUtils.chineseToEnglish(null));
        assertEquals("null转换应返回null", null, ActionNameUtils.englishToChinese(null));
        assertFalse("null匹配应返回false", ActionNameUtils.isNameMatch(null, "test"));
        assertFalse("null匹配应返回false", ActionNameUtils.isNameMatch("test", null));
        
        // 测试空字符串处理
        ActionNameUtils.addMapping("", "test");
        ActionNameUtils.addMapping("test", "");
        
        assertEquals("空字符串转换", "", ActionNameUtils.chineseToEnglish(""));
        assertEquals("空字符串转换", "", ActionNameUtils.englishToChinese(""));
        
        // 测试特殊字符
        ActionNameUtils.addMapping("特殊字符!@#", "special_chars");
        assertEquals("特殊字符处理", "special_chars", 
                    ActionNameUtils.chineseToEnglish("特殊字符!@#"));
    }
    
    /**
     * 测试性能
     */
    private static void testPerformance() {
        System.out.println("\n--- 测试性能 ---");
        
        ActionNameUtils.clearMappings();
        
        // 测试大量映射添加
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            ActionNameUtils.addMapping("动作" + i, "action_" + i);
        }
        long addTime = System.currentTimeMillis() - startTime;
        System.out.println("添加1000个映射耗时: " + addTime + "ms");
        
        // 测试查找性能
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            ActionNameUtils.chineseToEnglish("动作" + (i % 1000));
        }
        long searchTime = System.currentTimeMillis() - startTime;
        System.out.println("查找1000次耗时: " + searchTime + "ms");
        
        // 测试匹配性能
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            ActionNameUtils.isNameMatch("动作" + (i % 1000), "action_" + (i % 1000));
        }
        long matchTime = System.currentTimeMillis() - startTime;
        System.out.println("匹配1000次耗时: " + matchTime + "ms");
        
        // 性能断言
        assertTrue("添加性能应该合理", addTime < 1000); // 1秒内
        assertTrue("查找性能应该合理", searchTime < 100); // 100ms内
        assertTrue("匹配性能应该合理", matchTime < 200); // 200ms内
    }
    
    // 测试辅助方法
    private static void assertEquals(String message, Object expected, Object actual) {
        testCount++;
        if ((expected == null && actual == null) || 
            (expected != null && expected.equals(actual))) {
            System.out.println("✅ " + message);
            passCount++;
        } else {
            System.out.println("❌ " + message + " - 期望: " + expected + ", 实际: " + actual);
        }
    }
    
    private static void assertTrue(String message, boolean condition) {
        testCount++;
        if (condition) {
            System.out.println("✅ " + message);
            passCount++;
        } else {
            System.out.println("❌ " + message);
        }
    }
    
    private static void assertFalse(String message, boolean condition) {
        testCount++;
        if (!condition) {
            System.out.println("✅ " + message);
            passCount++;
        } else {
            System.out.println("❌ " + message);
        }
    }
}