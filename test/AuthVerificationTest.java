import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 认证验证测试
 * 使用修复后的认证算法测试实际的HTTP请求
 */
public class AuthVerificationTest {
    
    private static final String BASE_URL = "http://localhost:9189";
    private static final String ROBOT_ID = "EVOBOT-PRD-00000001";
    private static final String API_KEY = "ak_7x9m2n8p4q1r5s6t";
    
    private static int testCount = 0;
    private static int passCount = 0;
    
    public static void main(String[] args) {
        System.out.println("=== 认证验证测试 ===");
        System.out.println("使用修复后的认证算法测试HTTP请求");
        System.out.println();
        
        // 运行测试
        testGetRequest();
        testPostRequest();
        testAuthenticationComponents();
        
        // 输出结果
        System.out.println("\n=== 测试结果 ===");
        System.out.println("总测试数: " + testCount);
        System.out.println("通过测试: " + passCount);
        System.out.println("失败测试: " + (testCount - passCount));
        
        if (passCount == testCount) {
            System.out.println("✅ 所有认证测试通过！");
        } else {
            System.out.println("❌ 有测试失败！");
            System.exit(1);
        }
    }
    
    /**
     * 测试GET请求
     */
    private static void testGetRequest() {
        System.out.println("--- 测试GET请求 ---");
        
        try {
            String path = "/api/v1/sequences/list?limit=5&offset=0";
            String response = makeAuthenticatedRequest("GET", path, null);
            
            assertNotNull("GET响应不应为空", response);
            assertTrue("GET响应应包含JSON内容", response.contains("{") || response.contains("["));
            
            System.out.println("GET请求测试通过");
            System.out.println("响应长度: " + response.length() + " 字符");
            
            // 如果响应包含sequences，尝试解析
            if (response.contains("sequences")) {
                System.out.println("响应包含动作序列数据");
            }
            
        } catch (Exception e) {
            System.err.println("GET请求测试失败: " + e.getMessage());
            assertFalse("GET请求应该成功", true);
        }
    }
    
    /**
     * 测试POST请求
     */
    private static void testPostRequest() {
        System.out.println("\n--- 测试POST请求 ---");
        
        try {
            String path = "/api/v1/updates/check";
            String requestBody = "{" +
                "\"robot_id\":\"" + ROBOT_ID + "\"," +
                "\"library_version\":\"1.0.0\"," +
                "\"last_sync_time\":\"2024-01-01T00:00:00Z\"," +
                "\"sequences\":[]" +
                "}";
            
            String response = makeAuthenticatedRequest("POST", path, requestBody);
            
            assertNotNull("POST响应不应为空", response);
            assertTrue("POST响应应包含JSON内容", response.contains("{"));
            
            System.out.println("POST请求测试通过");
            System.out.println("响应长度: " + response.length() + " 字符");
            
            // 检查是否是更新检查的响应
            if (response.contains("has_updates") || response.contains("update_count")) {
                System.out.println("响应包含更新检查数据");
            }
            
        } catch (Exception e) {
            System.err.println("POST请求测试失败: " + e.getMessage());
            assertFalse("POST请求应该成功", true);
        }
    }
    
    /**
     * 测试认证组件
     */
    private static void testAuthenticationComponents() {
        System.out.println("\n--- 测试认证组件 ---");
        
        try {
            // 测试nonce生成
            String nonce1 = generateNonce();
            String nonce2 = generateNonce();
            
            assertNotNull("Nonce不应为空", nonce1);
            assertEquals("Nonce长度应为16", 16, nonce1.length());
            assertFalse("两次生成的nonce应该不同", nonce1.equals(nonce2));
            assertTrue("Nonce应该只包含字母和数字", nonce1.matches("[a-zA-Z0-9]+"));
            
            System.out.println("Nonce生成测试通过");
            System.out.println("示例nonce: " + nonce1);
            
            // 测试签名生成
            long timestamp = System.currentTimeMillis() / 1000;
            String method = "GET";
            String path = "/api/v1/test";
            String signature = generateSignature(method, path, timestamp, nonce1);
            
            assertNotNull("签名不应为空", signature);
            assertEquals("签名长度应为64", 64, signature.length());
            assertTrue("签名应该是十六进制", signature.matches("[0-9a-f]+"));
            
            System.out.println("签名生成测试通过");
            System.out.println("示例签名: " + signature.substring(0, 16) + "...");
            
            // 测试签名一致性
            String signature2 = generateSignature(method, path, timestamp, nonce1);
            assertEquals("相同参数应生成相同签名", signature, signature2);
            
            System.out.println("签名一致性测试通过");
            
        } catch (Exception e) {
            System.err.println("认证组件测试失败: " + e.getMessage());
            assertFalse("认证组件应该正常", true);
        }
    }
    
    /**
     * 发送认证请求
     */
    private static String makeAuthenticatedRequest(String method, String path, String requestBody) throws IOException {
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
            
            System.out.println("HTTP " + method + " " + path + " -> " + responseCode);
            
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
        connection.setRequestProperty("Content-Type", "application/json");
        
        System.out.println("认证头设置完成:");
        System.out.println("  Timestamp: " + timestamp);
        System.out.println("  Nonce: " + nonce);
        System.out.println("  Signature: " + signature.substring(0, 16) + "...");
    }
    
    /**
     * 生成随机nonce（修复版本）
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
     * 生成HMAC-SHA256签名（与Python版本一致）
     */
    private static String generateSignature(String method, String path, long timestamp, String nonce) throws Exception {
        // 移除查询参数，只使用路径部分进行签名
        String pathForSignature = path;
        int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            pathForSignature = path.substring(0, queryIndex);
        }
        
        // 构建签名字符串: robotId + timestamp + nonce + method + path
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