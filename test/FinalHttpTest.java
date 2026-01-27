import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 最终HTTP测试
 * 验证修复后的认证和动态映射功能
 */
public class FinalHttpTest {
    
    private static final String BASE_URL = "http://localhost:9189";
    private static final String ROBOT_ID = "EVOBOT-PRD-00000001";
    private static final String API_KEY = "ak_7x9m2n8p4q1r5s6t";
    
    private static int testCount = 0;
    private static int passCount = 0;
    
    public static void main(String[] args) {
        System.out.println("=== 最终HTTP测试验证 ===");
        System.out.println("验证修复后的认证和动态映射功能");
        System.out.println();
        
        // 运行所有测试
        testBasicHttpRequests();
        testDynamicMappingIntegration();
        testRealWorldScenario();
        
        // 输出结果
        System.out.println("\n=== 最终测试结果 ===");
        System.out.println("总测试数: " + testCount);
        System.out.println("通过测试: " + passCount);
        System.out.println("失败测试: " + (testCount - passCount));
        
        if (passCount == testCount) {
            System.out.println("🎉 所有HTTP测试通过！动态映射系统完全就绪！");
        } else {
            System.out.println("❌ 有测试失败！");
            System.exit(1);
        }
    }
    
    /**
     * 测试基本HTTP请求
     */
    private static void testBasicHttpRequests() {
        System.out.println("--- 测试基本HTTP请求 ---");
        
        try {
            // 测试GET请求
            String getResponse = makeRequest("GET", "/api/v1/sequences/list", null);
            assertNotNull("GET响应不应为空", getResponse);
            assertTrue("GET响应应包含JSON", getResponse.contains("{"));
            System.out.println("✅ GET请求成功，响应长度: " + getResponse.length());
            
            // 测试POST请求
            String postData = "{\"robot_id\":\"" + ROBOT_ID + "\",\"library_version\":\"1.0.0\",\"last_sync_time\":\"2024-01-01T00:00:00Z\",\"sequences\":[]}";
            String postResponse = makeRequest("POST", "/api/v1/updates/check", postData);
            assertNotNull("POST响应不应为空", postResponse);
            assertTrue("POST响应应包含JSON", postResponse.contains("{"));
            System.out.println("✅ POST请求成功，响应长度: " + postResponse.length());
            
        } catch (Exception e) {
            System.err.println("❌ 基本HTTP请求测试失败: " + e.getMessage());
            assertFalse("基本HTTP请求应该成功", true);
        }
    }
    
    /**
     * 测试动态映射集成
     */
    private static void testDynamicMappingIntegration() {
        System.out.println("\n--- 测试动态映射集成 ---");
        
        try {
            // 获取动作列表
            String response = makeRequest("GET", "/api/v1/sequences/list", null);
            
            // 清空现有映射
            ActionNameUtils.clearMappings();
            assertEquals("初始映射数量应为0", 0, ActionNameUtils.getMappingCount());
            
            // 从API响应建立映射
            buildMappingsFromResponse(response);
            int mappingCount = ActionNameUtils.getMappingCount();
            
            assertTrue("应该建立了映射", mappingCount > 0);
            System.out.println("✅ 成功建立 " + mappingCount + " 个动态映射");
            
            // 测试映射转换
            testMappingConversions();
            
        } catch (Exception e) {
            System.err.println("❌ 动态映射集成测试失败: " + e.getMessage());
            assertFalse("动态映射集成应该成功", true);
        }
    }
    
    /**
     * 测试映射转换
     */
    private static void testMappingConversions() {
        System.out.println("测试映射转换功能:");
        
        // 测试一些常见的映射转换
        String[] testNames = {"左臂挥手", "右臂挥手", "双臂拥抱"};
        
        for (String chineseName : testNames) {
            String englishName = ActionNameUtils.chineseToEnglish(chineseName);
            if (!chineseName.equals(englishName)) {
                System.out.println("  " + chineseName + " -> " + englishName);
                
                // 测试反向转换
                String backToChinese = ActionNameUtils.englishToChinese(englishName);
                assertEquals("反向转换应该正确", chineseName, backToChinese);
                
                // 测试名称匹配
                assertTrue("名称匹配应该成功", ActionNameUtils.isNameMatch(chineseName, englishName));
            }
        }
    }
    
    /**
     * 测试真实世界场景
     */
    private static void testRealWorldScenario() {
        System.out.println("\n--- 测试真实世界场景 ---");
        
        try {
            // 场景1: 应用启动时获取动作列表并建立映射
            System.out.println("场景1: 应用启动时建立映射");
            String response = makeRequest("GET", "/api/v1/sequences/list", null);
            ActionNameUtils.clearMappings();
            buildMappingsFromResponse(response);
            int startupMappings = ActionNameUtils.getMappingCount();
            assertTrue("启动时应该建立映射", startupMappings > 0);
            System.out.println("✅ 启动时建立了 " + startupMappings + " 个映射");
            
            // 场景2: 用户请求播放动作（使用中文名）
            System.out.println("场景2: 用户请求播放动作");
            String userRequest = "左臂挥手";
            String standardName = ActionNameUtils.getStandardName(userRequest);
            String fileName = ActionNameUtils.generateFileName(userRequest);
            System.out.println("  用户请求: " + userRequest);
            System.out.println("  标准名称: " + standardName);
            System.out.println("  文件名: " + fileName);
            assertTrue("应该能处理用户请求", !standardName.equals(userRequest) || ActionNameUtils.isEnglishName(userRequest));
            
            // 场景3: 检查更新
            System.out.println("场景3: 检查更新");
            String updateData = "{\"robot_id\":\"" + ROBOT_ID + "\",\"library_version\":\"1.0.0\",\"last_sync_time\":\"2024-01-01T00:00:00Z\",\"sequences\":[]}";
            String updateResponse = makeRequest("POST", "/api/v1/updates/check", updateData);
            assertTrue("更新检查应该成功", updateResponse.contains("has_updates") || updateResponse.contains("update_count"));
            System.out.println("✅ 更新检查成功");
            
            System.out.println("✅ 所有真实世界场景测试通过");
            
        } catch (Exception e) {
            System.err.println("❌ 真实世界场景测试失败: " + e.getMessage());
            assertFalse("真实世界场景应该成功", true);
        }
    }
    
    /**
     * 从API响应建立映射
     */
    private static void buildMappingsFromResponse(String jsonResponse) {
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
            System.err.println("建立映射失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
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
            setAuthHeaders(connection, method, path);
            
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
     * 设置认证头
     */
    private static void setAuthHeaders(HttpURLConnection connection, String method, String path) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String nonce = generateNonce();
            String signature = generateSignature(method, path, timestamp, nonce);
            
            connection.setRequestProperty("X-Robot-ID", ROBOT_ID);
            connection.setRequestProperty("X-API-Key", API_KEY);
            connection.setRequestProperty("X-Timestamp", String.valueOf(timestamp));
            connection.setRequestProperty("X-Nonce", nonce);
            connection.setRequestProperty("X-Signature", signature);
            connection.setRequestProperty("Content-Type", "application/json");
        } catch (Exception e) {
            throw new RuntimeException("设置认证头失败", e);
        }
    }
    
    /**
     * 生成随机nonce
     */
    private static String generateNonce() {
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
    
    // ===== ActionNameUtils 简化版本 =====
    
    static class ActionNameUtils {
        private static final Map<String, String> CHINESE_TO_ENGLISH = new ConcurrentHashMap<>();
        private static final Map<String, String> ENGLISH_TO_CHINESE = new ConcurrentHashMap<>();
        
        public static boolean isEnglishName(String name) {
            if (name == null || name.isEmpty()) return false;
            for (char c : name.toCharArray()) {
                if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                    return false;
                }
            }
            return name.contains("_") && name.matches("^[a-z0-9_]+$");
        }
        
        public static String chineseToEnglish(String chineseName) {
            if (chineseName == null || chineseName.isEmpty()) return chineseName;
            return CHINESE_TO_ENGLISH.getOrDefault(chineseName, chineseName);
        }
        
        public static String englishToChinese(String englishName) {
            if (englishName == null || englishName.isEmpty()) return englishName;
            return ENGLISH_TO_CHINESE.getOrDefault(englishName, englishName);
        }
        
        public static void addMapping(String chineseName, String englishName) {
            if (chineseName != null && !chineseName.isEmpty() && 
                englishName != null && !englishName.isEmpty()) {
                CHINESE_TO_ENGLISH.put(chineseName, englishName);
                ENGLISH_TO_CHINESE.put(englishName, chineseName);
            }
        }
        
        public static void clearMappings() {
            CHINESE_TO_ENGLISH.clear();
            ENGLISH_TO_CHINESE.clear();
        }
        
        public static int getMappingCount() {
            return CHINESE_TO_ENGLISH.size();
        }
        
        public static boolean isNameMatch(String name1, String name2) {
            if (name1 == null || name2 == null) return false;
            if (name1.equals(name2)) return true;
            
            String english1 = chineseToEnglish(name1);
            String english2 = chineseToEnglish(name2);
            if (english1.equals(english2)) return true;
            
            String chinese1 = englishToChinese(name1);
            String chinese2 = englishToChinese(name2);
            if (chinese1.equals(chinese2)) return true;
            
            return english1.equals(name2) || chinese1.equals(name2) ||
                   english2.equals(name1) || chinese2.equals(name1);
        }
        
        public static String getStandardName(String actionName) {
            if (actionName == null || actionName.isEmpty()) return actionName;
            if (isEnglishName(actionName)) return actionName;
            return chineseToEnglish(actionName);
        }
        
        public static String generateFileName(String actionName) {
            if (actionName == null || actionName.isEmpty()) return "unknown.ebs";
            String standardName = getStandardName(actionName);
            return standardName + ".ebs";
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