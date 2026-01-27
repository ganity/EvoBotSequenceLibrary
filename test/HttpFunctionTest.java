import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * HTTP相关功能测试
 * 测试ActionLibraryManager中的映射建立功能
 */
public class HttpFunctionTest {
    
    private static int testCount = 0;
    private static int passCount = 0;
    
    public static void main(String[] args) {
        System.out.println("=== HTTP相关功能测试 ===");
        
        // 运行所有测试
        testJsonParsing();
        testSequenceIdFinding();
        testMappingFromApiResponse();
        testEnglishNamePriority();
        
        // 输出测试结果
        System.out.println("\n=== 测试结果 ===");
        System.out.println("总测试数: " + testCount);
        System.out.println("通过测试: " + passCount);
        System.out.println("失败测试: " + (testCount - passCount));
        
        if (passCount == testCount) {
            System.out.println("✅ 所有HTTP功能测试通过！");
        } else {
            System.out.println("❌ 有测试失败！");
            System.exit(1);
        }
    }
    
    /**
     * 测试JSON解析功能
     */
    private static void testJsonParsing() {
        System.out.println("\n--- 测试JSON解析功能 ---");
        
        // 模拟API响应
        String mockApiResponse = "{\n" +
            "  \"code\": 2000,\n" +
            "  \"message\": \"success\",\n" +
            "  \"data\": {\n" +
            "    \"sequences\": [\n" +
            "      {\n" +
            "        \"id\": 1,\n" +
            "        \"name\": \"左臂挥手\",\n" +
            "        \"english_name\": \"arm_movement_left_arm_wave\",\n" +
            "        \"description\": \"左臂挥手动作序列\",\n" +
            "        \"category\": \"arm_movement\",\n" +
            "        \"status\": \"verified\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 2,\n" +
            "        \"name\": \"右臂挥手\",\n" +
            "        \"english_name\": \"arm_movement_right_arm_wave\",\n" +
            "        \"description\": \"右臂挥手动作序列\",\n" +
            "        \"category\": \"arm_movement\",\n" +
            "        \"status\": \"verified\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
        
        // 测试解析映射关系
        Map<String, String> parsedMappings = parseApiResponseForMappings(mockApiResponse);
        
        assertEquals("应该解析出2个映射", 2, parsedMappings.size());
        assertEquals("左臂挥手映射", "arm_movement_left_arm_wave", parsedMappings.get("左臂挥手"));
        assertEquals("右臂挥手映射", "arm_movement_right_arm_wave", parsedMappings.get("右臂挥手"));
    }
    
    /**
     * 测试序列ID查找功能
     */
    private static void testSequenceIdFinding() {
        System.out.println("\n--- 测试序列ID查找功能 ---");
        
        String mockApiResponse = "{\n" +
            "  \"sequences\": [\n" +
            "    {\n" +
            "      \"id\": 15,\n" +
            "      \"name\": \"左臂挥手\",\n" +
            "      \"english_name\": \"arm_movement_left_arm_wave\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 16,\n" +
            "      \"name\": \"右臂挥手\",\n" +
            "      \"english_name\": \"arm_movement_right_arm_wave\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        // 调试：打印JSON内容
        System.out.println("调试JSON内容:");
        String[] lines = mockApiResponse.split("\n");
        for (int i = 0; i < lines.length; i++) {
            System.out.println(i + ": " + lines[i].trim());
        }
        
        // 测试用中文名称查找ID
        int id1 = findSequenceIdByName(mockApiResponse, "左臂挥手");
        assertEquals("中文名称查找ID", 15, id1);
        
        // 测试用英文名称查找ID
        int id2 = findSequenceIdByName(mockApiResponse, "arm_movement_right_arm_wave");
        assertEquals("英文名称查找ID", 16, id2);
        
        // 测试不存在的名称
        int id3 = findSequenceIdByName(mockApiResponse, "不存在的动作");
        assertEquals("不存在的名称应返回-1", -1, id3);
    }
    
    /**
     * 测试从API响应建立映射
     */
    private static void testMappingFromApiResponse() {
        System.out.println("\n--- 测试从API响应建立映射 ---");
        
        String mockApiResponse = "{\n" +
            "  \"sequences\": [\n" +
            "    {\n" +
            "      \"name\": \"点头确认\",\n" +
            "      \"english_name\": \"head_nod_confirm\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"摇头拒绝\",\n" +
            "      \"english_name\": \"head_shake_refuse\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        // 清除现有映射
        ActionNameUtils.clearMappings();
        
        // 从API响应建立映射
        buildMappingsFromSequenceList(mockApiResponse);
        
        // 验证映射是否建立
        assertEquals("映射数量", 2, ActionNameUtils.getMappingCount());
        assertEquals("点头确认映射", "head_nod_confirm", ActionNameUtils.chineseToEnglish("点头确认"));
        assertEquals("摇头拒绝映射", "head_shake_refuse", ActionNameUtils.chineseToEnglish("摇头拒绝"));
    }
    
    /**
     * 测试英文名称优先级
     */
    private static void testEnglishNamePriority() {
        System.out.println("\n--- 测试英文名称优先级 ---");
        
        String mockApiResponse = "{\n" +
            "  \"sequences\": [\n" +
            "    {\n" +
            "      \"id\": 100,\n" +
            "      \"name\": \"测试动作\",\n" +
            "      \"english_name\": \"test_action\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        // 测试英文名称优先匹配
        int id1 = findSequenceIdByName(mockApiResponse, "test_action");
        assertEquals("英文名称优先匹配", 100, id1);
        
        // 测试中文名称也能匹配
        int id2 = findSequenceIdByName(mockApiResponse, "测试动作");
        assertEquals("中文名称也能匹配", 100, id2);
    }
    
    // ===== 模拟的HTTP相关方法 =====
    
    /**
     * 从API响应中解析映射关系（简化版本）
     */
    private static Map<String, String> parseApiResponseForMappings(String jsonResponse) {
        Map<String, String> mappings = new ConcurrentHashMap<>();
        
        try {
            String[] lines = jsonResponse.split("\n");
            String currentName = null;
            String currentEnglishName = null;
            
            for (String line : lines) {
                line = line.trim();
                
                if (line.contains("\"name\":")) {
                    // 提取中文名称
                    String[] parts = line.split("\"name\":\\s*\"");
                    if (parts.length > 1) {
                        currentName = parts[1].split("\"")[0];
                    }
                } else if (line.contains("\"english_name\":")) {
                    // 提取英文名称
                    String[] parts = line.split("\"english_name\":\\s*\"");
                    if (parts.length > 1) {
                        currentEnglishName = parts[1].split("\"")[0];
                    }
                }
                
                // 如果两个名称都获取到了，建立映射
                if (currentName != null && currentEnglishName != null) {
                    mappings.put(currentName, currentEnglishName);
                    System.out.println("解析映射: " + currentName + " -> " + currentEnglishName);
                    currentName = null;
                    currentEnglishName = null;
                }
            }
        } catch (Exception e) {
            System.err.println("解析API响应失败: " + e.getMessage());
        }
        
        return mappings;
    }
    
    /**
     * 从JSON响应中查找序列ID（支持中英文名称）
     */
    private static int findSequenceIdByName(String jsonResponse, String actionName) {
        try {
            String[] lines = jsonResponse.split("\n");
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                
                // 找到包含ID的行
                if (line.contains("\"id\":")) {
                    // 提取ID
                    String[] parts = line.split("\"id\":\\s*");
                    if (parts.length > 1) {
                        String idStr = parts[1].split("[,}]")[0].trim();
                        int id = Integer.parseInt(idStr);
                        
                        // 在接下来的几行中查找匹配的名称
                        for (int j = i + 1; j < Math.min(i + 10, lines.length); j++) {
                            String nextLine = lines[j].trim();
                            
                            // 如果遇到下一个对象的开始，停止搜索
                            if (nextLine.equals("{") || nextLine.contains("\"id\":")) {
                                break;
                            }
                            
                            // 检查英文名称匹配（考虑空格）
                            if (nextLine.contains("\"english_name\"") && nextLine.contains("\"" + actionName + "\"")) {
                                return id;
                            }
                            
                            // 检查中文名称匹配（考虑空格）
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
    
    /**
     * 从动作列表JSON中解析并建立映射关系
     */
    private static void buildMappingsFromSequenceList(String jsonResponse) {
        try {
            String[] lines = jsonResponse.split("\n");
            String currentName = null;
            String currentEnglishName = null;
            
            for (String line : lines) {
                line = line.trim();
                
                if (line.contains("\"name\":")) {
                    // 提取中文名称
                    String[] parts = line.split("\"name\":\\s*\"");
                    if (parts.length > 1) {
                        currentName = parts[1].split("\"")[0];
                    }
                } else if (line.contains("\"english_name\":")) {
                    // 提取英文名称
                    String[] parts = line.split("\"english_name\":\\s*\"");
                    if (parts.length > 1) {
                        currentEnglishName = parts[1].split("\"")[0];
                    }
                }
                
                // 如果两个名称都获取到了，建立映射
                if (currentName != null && currentEnglishName != null) {
                    ActionNameUtils.addMapping(currentName, currentEnglishName);
                    currentName = null;
                    currentEnglishName = null;
                }
            }
            
            System.out.println("从动作列表建立映射完成，当前映射数量: " + ActionNameUtils.getMappingCount());
            
        } catch (Exception e) {
            System.err.println("从动作列表建立映射失败: " + e.getMessage());
        }
    }
    
    // ===== ActionNameUtils 简化版本 =====
    
    static class ActionNameUtils {
        private static final Map<String, String> CHINESE_TO_ENGLISH = new ConcurrentHashMap<>();
        private static final Map<String, String> ENGLISH_TO_CHINESE = new ConcurrentHashMap<>();
        
        public static void addMapping(String chineseName, String englishName) {
            if (chineseName != null && !chineseName.isEmpty() && 
                englishName != null && !englishName.isEmpty()) {
                
                CHINESE_TO_ENGLISH.put(chineseName, englishName);
                ENGLISH_TO_CHINESE.put(englishName, chineseName);
                
                System.out.println("添加映射: '" + chineseName + "' <-> '" + englishName + "'");
            }
        }
        
        public static String chineseToEnglish(String chineseName) {
            if (chineseName == null || chineseName.isEmpty()) {
                return chineseName;
            }
            return CHINESE_TO_ENGLISH.getOrDefault(chineseName, chineseName);
        }
        
        public static void clearMappings() {
            CHINESE_TO_ENGLISH.clear();
            ENGLISH_TO_CHINESE.clear();
            System.out.println("清除所有映射");
        }
        
        public static int getMappingCount() {
            return CHINESE_TO_ENGLISH.size();
        }
    }
    
    // ===== 测试辅助方法 =====
    
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
}