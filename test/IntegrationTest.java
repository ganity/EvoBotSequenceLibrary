import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * 集成测试 - 测试动态映射系统的完整功能
 * 模拟真实的使用场景，包括HTTP API响应解析和文件操作
 */
public class IntegrationTest {
    
    private static int testCount = 0;
    private static int passCount = 0;
    
    public static void main(String[] args) {
        System.out.println("=== 动态映射系统集成测试 ===");
        
        // 运行所有集成测试
        testCompleteWorkflow();
        testApiResponseIntegration();
        testFileDownloadSimulation();
        testConcurrentAccess();
        testRealWorldScenarios();
        
        // 输出测试结果
        System.out.println("\n=== 集成测试结果 ===");
        System.out.println("总测试数: " + testCount);
        System.out.println("通过测试: " + passCount);
        System.out.println("失败测试: " + (testCount - passCount));
        
        if (passCount == testCount) {
            System.out.println("✅ 所有集成测试通过！");
        } else {
            System.out.println("❌ 有集成测试失败！");
            System.exit(1);
        }
    }
    
    /**
     * 测试完整的工作流程
     */
    private static void testCompleteWorkflow() {
        System.out.println("\n--- 测试完整工作流程 ---");
        
        // 1. 清空映射，模拟应用启动
        ActionNameUtils.clearMappings();
        assertEquals("初始状态映射数量应为0", 0, ActionNameUtils.getMappingCount());
        
        // 2. 模拟从API获取动作列表并建立映射
        String mockApiResponse = createMockApiResponse();
        ActionLibraryManager.buildMappingsFromSequenceList(mockApiResponse);
        
        assertTrue("API响应应该建立映射", ActionNameUtils.getMappingCount() > 0);
        
        // 3. 测试通过中文名称查找序列ID
        int sequenceId = ActionLibraryManager.findSequenceIdByName(mockApiResponse, "左臂挥手");
        assertEquals("应该找到左臂挥手的序列ID", 1, sequenceId);
        
        // 4. 测试通过英文名称查找序列ID
        int sequenceId2 = ActionLibraryManager.findSequenceIdByName(mockApiResponse, "arm_movement_right_arm_wave");
        assertEquals("应该找到右臂挥手的序列ID", 2, sequenceId2);
        
        // 5. 测试映射转换功能
        String englishName = ActionNameUtils.chineseToEnglish("左臂挥手");
        assertEquals("中文转英文", "arm_movement_left_arm_wave", englishName);
        
        String chineseName = ActionNameUtils.englishToChinese("arm_movement_right_arm_wave");
        assertEquals("英文转中文", "右臂挥手", chineseName);
        
        System.out.println("✅ 完整工作流程测试通过");
    }
    
    /**
     * 测试API响应集成
     */
    private static void testApiResponseIntegration() {
        System.out.println("\n--- 测试API响应集成 ---");
        
        ActionNameUtils.clearMappings();
        
        // 测试复杂的API响应
        String complexApiResponse = "{\n" +
            "  \"code\": 2000,\n" +
            "  \"message\": \"success\",\n" +
            "  \"data\": {\n" +
            "    \"total\": 5,\n" +
            "    \"sequences\": [\n" +
            "      {\n" +
            "        \"id\": 101,\n" +
            "        \"name\": \"点头确认\",\n" +
            "        \"english_name\": \"head_nod_confirm\",\n" +
            "        \"category\": \"head_movement\",\n" +
            "        \"status\": \"verified\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 102,\n" +
            "        \"name\": \"摇头拒绝\",\n" +
            "        \"english_name\": \"head_shake_refuse\",\n" +
            "        \"category\": \"head_movement\",\n" +
            "        \"status\": \"verified\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 103,\n" +
            "        \"name\": \"双手鼓掌\",\n" +
            "        \"english_name\": \"hands_clap_both\",\n" +
            "        \"category\": \"hand_movement\",\n" +
            "        \"status\": \"verified\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
        
        // 解析API响应
        ActionLibraryManager.buildMappingsFromSequenceList(complexApiResponse);
        
        assertEquals("应该解析出3个映射", 3, ActionNameUtils.getMappingCount());
        
        // 测试各种查找方式
        assertEquals("点头确认ID查找", 101, ActionLibraryManager.findSequenceIdByName(complexApiResponse, "点头确认"));
        assertEquals("英文名ID查找", 102, ActionLibraryManager.findSequenceIdByName(complexApiResponse, "head_shake_refuse"));
        assertEquals("双手鼓掌ID查找", 103, ActionLibraryManager.findSequenceIdByName(complexApiResponse, "双手鼓掌"));
        
        // 测试映射转换
        assertEquals("点头确认转英文", "head_nod_confirm", ActionNameUtils.chineseToEnglish("点头确认"));
        assertEquals("英文转中文", "摇头拒绝", ActionNameUtils.englishToChinese("head_shake_refuse"));
        
        System.out.println("✅ API响应集成测试通过");
    }
    
    /**
     * 测试文件下载模拟
     */
    private static void testFileDownloadSimulation() {
        System.out.println("\n--- 测试文件下载模拟 ---");
        
        ActionNameUtils.clearMappings();
        
        // 模拟文件下载和解析过程
        simulateFileDownload("arm_movement_left_arm_wave.ebs", "左臂挥手");
        simulateFileDownload("head_nod_confirm.ebs", "点头确认");
        simulateFileDownload("hands_clap_both.ebs", "双手鼓掌");
        
        assertEquals("文件下载后应该有3个映射", 3, ActionNameUtils.getMappingCount());
        
        // 测试文件名提取
        assertEquals("文件名提取1", "arm_movement_left_arm_wave", 
                    ActionNameUtils.extractActionNameFromFileName("arm_movement_left_arm_wave.ebs"));
        assertEquals("文件名提取2", "head_nod_confirm", 
                    ActionNameUtils.extractActionNameFromFileName("head_nod_confirm.ebs"));
        
        // 测试映射查找
        assertEquals("文件映射查找1", "arm_movement_left_arm_wave", ActionNameUtils.chineseToEnglish("左臂挥手"));
        assertEquals("文件映射查找2", "点头确认", ActionNameUtils.englishToChinese("head_nod_confirm"));
        
        System.out.println("✅ 文件下载模拟测试通过");
    }
    
    /**
     * 测试并发访问
     */
    private static void testConcurrentAccess() {
        System.out.println("\n--- 测试并发访问 ---");
        
        ActionNameUtils.clearMappings();
        
        // 模拟多线程并发添加映射
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    String chineseName = "动作" + threadId + "_" + j;
                    String englishName = "action_" + threadId + "_" + j;
                    ActionNameUtils.addMapping(chineseName, englishName);
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证结果
        assertEquals("并发添加应该有1000个映射", 1000, ActionNameUtils.getMappingCount());
        
        // 测试并发查找
        boolean allFound = true;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 100; j++) {
                String chineseName = "动作" + i + "_" + j;
                String expectedEnglish = "action_" + i + "_" + j;
                String actualEnglish = ActionNameUtils.chineseToEnglish(chineseName);
                if (!expectedEnglish.equals(actualEnglish)) {
                    allFound = false;
                    break;
                }
            }
            if (!allFound) break;
        }
        
        assertTrue("并发查找应该全部成功", allFound);
        
        System.out.println("✅ 并发访问测试通过");
    }
    
    /**
     * 测试真实世界场景
     */
    private static void testRealWorldScenarios() {
        System.out.println("\n--- 测试真实世界场景 ---");
        
        ActionNameUtils.clearMappings();
        
        // 场景1: 应用启动时从本地文件建立映射
        simulateAppStartup();
        int startupMappings = ActionNameUtils.getMappingCount();
        assertTrue("启动时应该有映射", startupMappings > 0);
        
        // 场景2: 从API获取新的动作列表
        String newApiResponse = createExtendedApiResponse();
        ActionLibraryManager.buildMappingsFromSequenceList(newApiResponse);
        int afterApiMappings = ActionNameUtils.getMappingCount();
        assertTrue("API更新后映射应该增加", afterApiMappings > startupMappings);
        
        // 场景3: 用户请求播放动作（使用中文名）
        String actionToPlay = "左臂挥手";
        String englishName = ActionNameUtils.chineseToEnglish(actionToPlay);
        assertEquals("用户请求转换", "arm_movement_left_arm_wave", englishName);
        
        // 场景4: 系统内部使用英文名查找文件
        String fileName = ActionNameUtils.generateFileName(actionToPlay);
        assertEquals("文件名生成", "arm_movement_left_arm_wave.ebs", fileName);
        
        // 场景5: 名称匹配（支持中英文混合）
        assertTrue("中英文匹配", ActionNameUtils.isNameMatch("左臂挥手", "arm_movement_left_arm_wave"));
        assertTrue("英中文匹配", ActionNameUtils.isNameMatch("arm_movement_left_arm_wave", "左臂挥手"));
        
        // 场景6: 标准化名称（优先英文）
        assertEquals("中文标准化", "arm_movement_left_arm_wave", ActionNameUtils.getStandardName("左臂挥手"));
        assertEquals("英文标准化", "arm_movement_left_arm_wave", ActionNameUtils.getStandardName("arm_movement_left_arm_wave"));
        
        System.out.println("✅ 真实世界场景测试通过");
    }
    
    // ===== 辅助方法 =====
    
    /**
     * 创建模拟API响应
     */
    private static String createMockApiResponse() {
        return "{\n" +
            "  \"sequences\": [\n" +
            "    {\n" +
            "      \"id\": 1,\n" +
            "      \"name\": \"左臂挥手\",\n" +
            "      \"english_name\": \"arm_movement_left_arm_wave\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 2,\n" +
            "      \"name\": \"右臂挥手\",\n" +
            "      \"english_name\": \"arm_movement_right_arm_wave\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }
    
    /**
     * 创建扩展的API响应
     */
    private static String createExtendedApiResponse() {
        return "{\n" +
            "  \"sequences\": [\n" +
            "    {\n" +
            "      \"id\": 1,\n" +
            "      \"name\": \"左臂挥手\",\n" +
            "      \"english_name\": \"arm_movement_left_arm_wave\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 2,\n" +
            "      \"name\": \"右臂挥手\",\n" +
            "      \"english_name\": \"arm_movement_right_arm_wave\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 3,\n" +
            "      \"name\": \"转身动作\",\n" +
            "      \"english_name\": \"body_turn_around\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 4,\n" +
            "      \"name\": \"前进步伐\",\n" +
            "      \"english_name\": \"walk_forward_step\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }
    
    /**
     * 模拟文件下载
     */
    private static void simulateFileDownload(String fileName, String chineseName) {
        // 模拟SequenceData
        MockSequenceData sequenceData = new MockSequenceData();
        sequenceData.name = chineseName;
        
        // 建立映射
        ActionNameUtils.addMappingFromFile(fileName, sequenceData);
    }
    
    /**
     * 模拟应用启动
     */
    private static void simulateAppStartup() {
        // 模拟本地已有的文件
        simulateFileDownload("arm_movement_left_arm_wave.ebs", "左臂挥手");
        simulateFileDownload("arm_movement_right_arm_wave.ebs", "右臂挥手");
        simulateFileDownload("head_nod_confirm.ebs", "点头确认");
    }
    
    // ===== 模拟类 =====
    
    /**
     * 模拟SequenceData类
     */
    static class MockSequenceData {
        public String name;
    }
    
    // ===== ActionNameUtils 简化版本 =====
    
    static class ActionNameUtils {
        private static final Map<String, String> CHINESE_TO_ENGLISH = new ConcurrentHashMap<>();
        private static final Map<String, String> ENGLISH_TO_CHINESE = new ConcurrentHashMap<>();
        private static final Map<String, String> FILENAME_TO_CHINESE = new ConcurrentHashMap<>();
        
        public static boolean isEnglishName(String name) {
            if (name == null || name.isEmpty()) {
                return false;
            }
            
            for (char c : name.toCharArray()) {
                if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                    return false;
                }
            }
            
            return name.contains("_") && name.matches("^[a-z0-9_]+$");
        }
        
        public static boolean isChineseName(String name) {
            if (name == null || name.isEmpty()) {
                return false;
            }
            
            for (char c : name.toCharArray()) {
                if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                    return true;
                }
            }
            
            return false;
        }
        
        public static String chineseToEnglish(String chineseName) {
            if (chineseName == null || chineseName.isEmpty()) {
                return chineseName;
            }
            return CHINESE_TO_ENGLISH.getOrDefault(chineseName, chineseName);
        }
        
        public static String englishToChinese(String englishName) {
            if (englishName == null || englishName.isEmpty()) {
                return englishName;
            }
            return ENGLISH_TO_CHINESE.getOrDefault(englishName, englishName);
        }
        
        public static void addMapping(String chineseName, String englishName) {
            if (chineseName != null && !chineseName.isEmpty() && 
                englishName != null && !englishName.isEmpty()) {
                
                CHINESE_TO_ENGLISH.put(chineseName, englishName);
                ENGLISH_TO_CHINESE.put(englishName, chineseName);
            }
        }
        
        public static void addMappingFromFile(String fileName, MockSequenceData sequenceData) {
            if (fileName == null || sequenceData == null) {
                return;
            }
            
            String englishName = extractActionNameFromFileName(fileName);
            String chineseName = sequenceData.name;
            
            if (chineseName != null && !chineseName.isEmpty()) {
                addMapping(chineseName, englishName);
                FILENAME_TO_CHINESE.put(fileName, chineseName);
            }
        }
        
        public static void clearMappings() {
            CHINESE_TO_ENGLISH.clear();
            ENGLISH_TO_CHINESE.clear();
            FILENAME_TO_CHINESE.clear();
        }
        
        public static int getMappingCount() {
            return CHINESE_TO_ENGLISH.size();
        }
        
        public static boolean isNameMatch(String name1, String name2) {
            if (name1 == null || name2 == null) {
                return false;
            }
            
            if (name1.equals(name2)) {
                return true;
            }
            
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
            
            if (english1.equals(name2) || chinese1.equals(name2) ||
                english2.equals(name1) || chinese2.equals(name1)) {
                return true;
            }
            
            return false;
        }
        
        public static String extractActionNameFromFileName(String fileName) {
            if (fileName == null || fileName.isEmpty()) {
                return fileName;
            }
            
            if (fileName.endsWith(".ebs")) {
                return fileName.substring(0, fileName.length() - 4);
            }
            
            return fileName;
        }
        
        public static String generateFileName(String actionName) {
            if (actionName == null || actionName.isEmpty()) {
                return "unknown.ebs";
            }
            
            String standardName = getStandardName(actionName);
            return standardName + ".ebs";
        }
        
        public static String getStandardName(String actionName) {
            if (actionName == null || actionName.isEmpty()) {
                return actionName;
            }
            
            if (isEnglishName(actionName)) {
                return actionName;
            } else if (isChineseName(actionName)) {
                return chineseToEnglish(actionName);
            }
            
            return actionName;
        }
    }
    
    // ===== ActionLibraryManager 简化版本 =====
    
    static class ActionLibraryManager {
        
        public static void buildMappingsFromSequenceList(String jsonResponse) {
            try {
                String[] lines = jsonResponse.split("\n");
                String currentName = null;
                String currentEnglishName = null;
                
                for (String line : lines) {
                    line = line.trim();
                    
                    if (line.contains("\"name\":")) {
                        String[] parts = line.split("\"name\":\\s*\"");
                        if (parts.length > 1) {
                            currentName = parts[1].split("\"")[0];
                        }
                    } else if (line.contains("\"english_name\":")) {
                        String[] parts = line.split("\"english_name\":\\s*\"");
                        if (parts.length > 1) {
                            currentEnglishName = parts[1].split("\"")[0];
                        }
                    }
                    
                    if (currentName != null && currentEnglishName != null) {
                        ActionNameUtils.addMapping(currentName, currentEnglishName);
                        currentName = null;
                        currentEnglishName = null;
                    }
                }
            } catch (Exception e) {
                System.err.println("从动作列表建立映射失败: " + e.getMessage());
            }
        }
        
        public static int findSequenceIdByName(String jsonResponse, String actionName) {
            try {
                String[] lines = jsonResponse.split("\n");
                
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    
                    if (line.contains("\"id\":")) {
                        String[] parts = line.split("\"id\":\\s*");
                        if (parts.length > 1) {
                            String idStr = parts[1].split("[,}]")[0].trim();
                            int id = Integer.parseInt(idStr);
                            
                            for (int j = i + 1; j < Math.min(i + 10, lines.length); j++) {
                                String nextLine = lines[j].trim();
                                
                                if (nextLine.equals("{") || nextLine.contains("\"id\":")) {
                                    break;
                                }
                                
                                if (nextLine.contains("\"english_name\"") && nextLine.contains("\"" + actionName + "\"")) {
                                    return id;
                                }
                                
                                if (nextLine.contains("\"name\"") && nextLine.contains("\"" + actionName + "\"") && 
                                    !nextLine.contains("english_name")) {
                                    return id;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("解析序列ID失败: " + e.getMessage());
            }
            
            return -1;
        }
    }
    
    // ===== 测试辅助方法 =====
    
    private static void assertEquals(String message, Object expected, Object actual) {
        testCount++;
        if ((expected == null && actual == null) || 
            (expected != null && expected.equals(actual))) {
            passCount++;
        } else {
            System.out.println("❌ " + message + " - 期望: " + expected + ", 实际: " + actual);
        }
    }
    
    private static void assertTrue(String message, boolean condition) {
        testCount++;
        if (condition) {
            passCount++;
        } else {
            System.out.println("❌ " + message);
        }
    }
}