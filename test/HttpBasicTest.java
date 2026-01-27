import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HTTP基础测试
 * 不依赖Android SDK，可以在普通Java环境中运行
 * 测试HTTP客户端的核心功能
 */
public class HttpBasicTest {
    
    private static final String TAG = "HttpBasicTest";
    
    // 配置信息
    private static final String BASE_URL = "http://localhost:9189/api/v1";
    private static final String ROBOT_ID = "EVOBOT-PRD-00000001";
    private static final String API_KEY = "ak_7x9m2n8p4q1r5s6t";
    
    private static int testCount = 0;
    private static int passCount = 0;
    
    public static void main(String[] args) {
        System.out.println("=== HTTP基础功能测试 ===");
        System.out.println("测试服务器: " + BASE_URL);
        System.out.println("机器人ID: " + ROBOT_ID);
        System.out.println();
        
        // 运行所有测试
        testHttpConnection();
        testAuthentication();
        testSequenceListApi();
        testJsonParsing();
        testMappingIntegration();
        
        // 输出测试结果
        System.out.println("\n=== 测试结果 ===");
        System.out.println("总测试数: " + testCount);
        System.out.println("通过测试: " + passCount);
        System.out.println("失败测试: " + (testCount - passCount));
        
        if (passCount == testCount) {
            System.out.println("✅ 所有HTTP基础测试通过！");
        } else {
            System.out.println("❌ 有测试失败！");
            System.exit(1);
        }
    }
    
    /**
     * 测试HTTP连接
     */
    private static void testHttpConnection() {
        System.out.println("\n--- 测试HTTP连接 ---");
        
        try {
            // 测试基本连接
            String response = makeRequest("GET", "/sequences/list?limit=1&offset=0", null);
            
            assertNotNull("HTTP响应不应为空", response);
            assertTrue("响应应包含内容", response.length() > 0);
            
            System.out.println("HTTP连接测试通过");
            System.out.println("响应长度: " + response.length() + " 字符");
            
        } catch (Exception e) {
            System.err.println("HTTP连接测试失败: " + e.getMessage());
            assertFalse("HTTP连接应该成功", true);
        }
    }
    
    /**
     * 测试认证功能
     */
    private static void testAuthentication() {
        System.out.println("\n--- 测试认证功能 ---");
        
        try {
            // 测试认证头生成
            long timestamp = System.currentTimeMillis() / 1000;
            String nonce = generateNonce();
            String signature = generateSignature("GET", "/sequences/list", timestamp, nonce);
            
            assertNotNull("时间戳不应为空", timestamp);
            assertNotNull("Nonce不应为空", nonce);
            assertNotNull("签名不应为空", signature);
            assertTrue("Nonce应该是16位字母数字", nonce.length() == 16 && nonce.matches("[a-zA-Z0-9]+"));
            assertTrue("签名应该是64位十六进制", signature.length() == 64);
            
            System.out.println("认证功能测试通过");
            System.out.println("时间戳: " + timestamp);
            System.out.println("Nonce: " + nonce);
            System.out.println("签名: " + signature.substring(0, 16) + "...");
            
        } catch (Exception e) {
            System.err.println("认证功能测试失败: " + e.getMessage());
            assertFalse("认证功能应该正常", true);
        }
    }
    
    /**
     * 测试动作序列列表API
     */
    private static void testSequenceListApi() {
        System.out.println("\n--- 测试动作序列列表API ---");
        
        try {
            // 测试获取动作列表
            String response = makeRequest("GET", "/sequences/list?limit=10&offset=0", null);
            
            assertNotNull("API响应不应为空", response);
            assertTrue("响应应包含JSON内容", response.contains("{") || response.contains("["));
            
            System.out.println("动作序列列表API测试通过");
            System.out.println("响应预览: " + response.substring(0, Math.min(100, response.length())) + "...");
            
            // 测试带分类的请求
            String categoryResponse = makeRequest("GET", "/sequences/list?category=arm_movement&limit=5&offset=0", null);
            assertNotNull("分类API响应不应为空", categoryResponse);
            
            System.out.println("分类查询测试通过");
            
        } catch (Exception e) {
            System.err.println("动作序列列表API测试失败: " + e.getMessage());
            assertFalse("动作序列列表API应该成功", true);
        }
    }
    
    /**
     * 测试JSON解析
     */
    private static void testJsonParsing() {
        System.out.println("\n--- 测试JSON解析 ---");
        
        try {
            // 模拟API响应
            String mockResponse = "{\n" +
                "  \"code\": 2000,\n" +
                "  \"message\": \"success\",\n" +
                "  \"data\": {\n" +
                "    \"sequences\": [\n" +
                "      {\n" +
                "        \"id\": 1,\n" +
                "        \"name\": \"左臂挥手\",\n" +
                "        \"english_name\": \"arm_movement_left_arm_wave\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 2,\n" +
                "        \"name\": \"右臂挥手\",\n" +
                "        \"english_name\": \"arm_movement_right_arm_wave\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
            
            // 解析映射关系
            Map<String, String> mappings = parseApiResponseForMappings(mockResponse);
            
            assertEquals("应该解析出2个映射", 2, mappings.size());
            assertEquals("左臂挥手映射", "arm_movement_left_arm_wave", mappings.get("左臂挥手"));
            assertEquals("右臂挥手映射", "arm_movement_right_arm_wave", mappings.get("右臂挥手"));
            
            // 测试序列ID查找
            int sequenceId = findSequenceIdByName(mockResponse, "左臂挥手");
            assertEquals("应该找到序列ID", 1, sequenceId);
            
            int englishId = findSequenceIdByName(mockResponse, "arm_movement_right_arm_wave");
            assertEquals("英文名称应该找到序列ID", 2, englishId);
            
            System.out.println("JSON解析测试通过");
            System.out.println("解析出映射数量: " + mappings.size());
            
        } catch (Exception e) {
            System.err.println("JSON解析测试失败: " + e.getMessage());
            assertFalse("JSON解析应该成功", true);
        }
    }
    
    /**
     * 测试映射集成
     */
    private static void testMappingIntegration() {
        System.out.println("\n--- 测试映射集成 ---");
        
        try {
            // 获取真实的API响应
            String response = makeRequest("GET", "/sequences/list?limit=20&offset=0", null);
            
            // 清空现有映射
            ActionNameUtils.clearMappings();
            int initialCount = ActionNameUtils.getMappingCount();
            assertEquals("初始映射数量应为0", 0, initialCount);
            
            // 从API响应建立映射
            buildMappingsFromSequenceList(response);
            int finalCount = ActionNameUtils.getMappingCount();
            
            assertTrue("应该建立了映射关系", finalCount > 0);
            
            System.out.println("映射集成测试通过");
            System.out.println("建立了 " + finalCount + " 个映射关系");
            
            // 显示一些映射示例
            showMappingExamples();
            
        } catch (Exception e) {
            System.err.println("映射集成测试失败: " + e.getMessage());
            assertFalse("映射集成应该成功", true);
        }
    }
    
    // ===== HTTP工具方法 =====
    
    /**
     * 发送HTTP请求
     */
    private static String makeRequest(String method, String path, String requestBody) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            
            // 设置认证头
            try {
                setAuthHeaders(connection, method, path);
            } catch (Exception e) {
                throw new IOException("设置认证头失败: " + e.getMessage(), e);
            }
            
            // 发送请求体
            if (requestBody != null && ("POST".equals(method) || "PUT".equals(method))) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }
            }
            
            // 读取响应
            int responseCode = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                (responseCode >= 200 && responseCode < 300) 
                    ? connection.getInputStream() 
                    : connection.getErrorStream(),
                StandardCharsets.UTF_8));
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append('\n');
            }
            reader.close();
            
            if (responseCode >= 200 && responseCode < 300) {
                return response.toString();
            } else {
                throw new IOException("HTTP请求失败，响应码: " + responseCode + ", 响应: " + response.toString());
            }
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 设置认证请求头
     */
    private static void setAuthHeaders(HttpURLConnection connection, String method, String path) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = generateNonce();
        String signature = generateSignature(method, path, timestamp, nonce);
        
        connection.setRequestProperty("X-Robot-ID", ROBOT_ID);
        connection.setRequestProperty("X-API-Key", API_KEY);
        connection.setRequestProperty("X-Timestamp", String.valueOf(timestamp));
        connection.setRequestProperty("X-Nonce", nonce);
        connection.setRequestProperty("X-Signature", signature);
    }
    
    /**
     * 生成随机nonce
     */
    private static String generateNonce() {
        // 使用字母和数字生成16位随机字符串，与Python版本保持一致
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder nonce = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 16; i++) {
            nonce.append(chars.charAt(random.nextInt(chars.length())));
        }
        return nonce.toString();
    }
    
    /**
     * 生成HMAC-SHA256签名
     */
    private static String generateSignature(String method, String path, long timestamp, String nonce) throws Exception {
        // 移除查询参数，只使用路径部分进行签名
        String pathForSignature = path;
        int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            pathForSignature = path.substring(0, queryIndex);
        }
        
        // 确保路径包含完整的API路径
        if (!pathForSignature.startsWith("/api/v1")) {
            pathForSignature = "/api/v1" + pathForSignature;
        }
        
        String signatureString = ROBOT_ID + timestamp + nonce + method.toUpperCase() + pathForSignature;
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(API_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        
        byte[] signature = mac.doFinal(signatureString.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(signature);
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    // ===== JSON解析方法 =====
    
    /**
     * 从API响应中解析映射关系
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
                    mappings.put(currentName, currentEnglishName);
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
     * 从JSON响应中查找序列ID
     */
    private static int findSequenceIdByName(String jsonResponse, String actionName) {
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
    
    /**
     * 从动作列表JSON中解析并建立映射关系
     */
    private static void buildMappingsFromSequenceList(String jsonResponse) {
        try {
            System.out.println("调试: 开始解析API响应");
            
            // 响应是一行JSON，需要用正则表达式或简单字符串匹配来解析
            // 格式: {"id":3,"name":"双臂拥抱","english_name":"interaction_both_arms_hug",...}
            
            // 使用正则表达式匹配所有的name和english_name对
            String pattern = "\"name\":\"([^\"]+)\",\"english_name\":\"([^\"]+)\"";
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regex.matcher(jsonResponse);
            
            int mappingCount = 0;
            while (matcher.find()) {
                String chineseName = matcher.group(1);
                String englishName = matcher.group(2);
                
                ActionNameUtils.addMapping(chineseName, englishName);
                System.out.println("建立映射: " + chineseName + " -> " + englishName);
                mappingCount++;
            }
            
            System.out.println("解析完成: 建立了 " + mappingCount + " 个映射");
            
        } catch (Exception e) {
            System.err.println("从动作列表建立映射失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 显示映射示例
     */
    private static void showMappingExamples() {
        System.out.println("映射转换示例:");
        
        String[] testNames = {"左臂挥手", "右臂挥手", "点头确认", "摇头拒绝", "双手鼓掌"};
        
        for (String chineseName : testNames) {
            String englishName = ActionNameUtils.chineseToEnglish(chineseName);
            if (!chineseName.equals(englishName)) {
                System.out.println("  " + chineseName + " -> " + englishName);
            }
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
        }
        
        public static int getMappingCount() {
            return CHINESE_TO_ENGLISH.size();
        }
    }
    
    // ===== 测试断言方法 =====
    
    private static void assertTrue(String message, boolean condition) {
        testCount++;
        if (condition) {
            passCount++;
            System.out.println("✅ " + message);
        } else {
            System.err.println("❌ " + message);
        }
    }
    
    private static void assertFalse(String message, boolean condition) {
        testCount++;
        if (!condition) {
            passCount++;
            System.out.println("✅ " + message);
        } else {
            System.err.println("❌ " + message);
        }
    }
    
    private static void assertNotNull(String message, Object object) {
        testCount++;
        if (object != null) {
            passCount++;
            System.out.println("✅ " + message);
        } else {
            System.err.println("❌ " + message);
        }
    }
    
    private static void assertEquals(String message, Object expected, Object actual) {
        testCount++;
        if ((expected == null && actual == null) || 
            (expected != null && expected.equals(actual))) {
            passCount++;
            System.out.println("✅ " + message);
        } else {
            System.err.println("❌ " + message + " - 期望: " + expected + ", 实际: " + actual);
        }
    }
}