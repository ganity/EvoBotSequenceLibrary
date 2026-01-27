import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 签名调试工具
 * 用于调试和验证HMAC-SHA256签名算法
 */
public class SignatureDebugger {
    
    private static final String ROBOT_ID = "EVOBOT-PRD-00000001";
    private static final String API_KEY = "ak_7x9m2n8p4q1r5s6t";
    
    public static void main(String[] args) {
        System.out.println("=== 签名调试工具 ===");
        System.out.println("机器人ID: " + ROBOT_ID);
        System.out.println("API密钥: " + API_KEY);
        System.out.println();
        
        // 测试不同的签名方式
        testSignatureVariations();
        
        // 生成示例请求
        generateExampleRequest();
    }
    
    /**
     * 测试不同的签名方式
     */
    private static void testSignatureVariations() {
        System.out.println("--- 测试不同的签名方式 ---");
        
        String method = "GET";
        String path = "/sequences/list";
        long timestamp = 1769425727L; // 固定时间戳用于调试
        String nonce = "dbf59ca61bf29b63835af3a4c71e7435"; // 固定nonce用于调试
        
        System.out.println("测试参数:");
        System.out.println("  Method: " + method);
        System.out.println("  Path: " + path);
        System.out.println("  Timestamp: " + timestamp);
        System.out.println("  Nonce: " + nonce);
        System.out.println();
        
        // 方式1: robotId + timestamp + nonce + method + path
        String signString1 = ROBOT_ID + timestamp + nonce + method.toUpperCase() + path;
        String signature1 = generateHmacSha256(signString1, API_KEY);
        System.out.println("方式1 - 签名字符串: " + signString1);
        System.out.println("方式1 - 签名结果: " + signature1);
        System.out.println();
        
        // 方式2: method + path + robotId + timestamp + nonce
        String signString2 = method.toUpperCase() + path + ROBOT_ID + timestamp + nonce;
        String signature2 = generateHmacSha256(signString2, API_KEY);
        System.out.println("方式2 - 签名字符串: " + signString2);
        System.out.println("方式2 - 签名结果: " + signature2);
        System.out.println();
        
        // 方式3: timestamp + nonce + method + path + robotId
        String signString3 = timestamp + nonce + method.toUpperCase() + path + ROBOT_ID;
        String signature3 = generateHmacSha256(signString3, API_KEY);
        System.out.println("方式3 - 签名字符串: " + signString3);
        System.out.println("方式3 - 签名结果: " + signature3);
        System.out.println();
        
        // 方式4: 带分隔符
        String signString4 = ROBOT_ID + "|" + timestamp + "|" + nonce + "|" + method.toUpperCase() + "|" + path;
        String signature4 = generateHmacSha256(signString4, API_KEY);
        System.out.println("方式4 - 签名字符串: " + signString4);
        System.out.println("方式4 - 签名结果: " + signature4);
        System.out.println();
        
        // 方式5: 小写method
        String signString5 = ROBOT_ID + timestamp + nonce + method.toLowerCase() + path;
        String signature5 = generateHmacSha256(signString5, API_KEY);
        System.out.println("方式5 - 签名字符串: " + signString5);
        System.out.println("方式5 - 签名结果: " + signature5);
        System.out.println();
    }
    
    /**
     * 生成示例请求
     */
    private static void generateExampleRequest() {
        System.out.println("--- 生成示例请求 ---");
        
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = generateNonce();
        String method = "GET";
        String path = "/sequences/list?limit=1&offset=0";
        
        String signString = ROBOT_ID + timestamp + nonce + method.toUpperCase() + path;
        String signature = generateHmacSha256(signString, API_KEY);
        
        System.out.println("请求信息:");
        System.out.println("  URL: http://localhost:9189/api/v1" + path);
        System.out.println("  Method: " + method);
        System.out.println();
        
        System.out.println("认证头:");
        System.out.println("  X-Robot-ID: " + ROBOT_ID);
        System.out.println("  X-API-Key: " + API_KEY);
        System.out.println("  X-Timestamp: " + timestamp);
        System.out.println("  X-Nonce: " + nonce);
        System.out.println("  X-Signature: " + signature);
        System.out.println();
        
        System.out.println("签名计算:");
        System.out.println("  签名字符串: " + signString);
        System.out.println("  HMAC-SHA256: " + signature);
        System.out.println();
        
        System.out.println("cURL命令:");
        System.out.println("curl -X GET \\");
        System.out.println("  -H \"X-Robot-ID: " + ROBOT_ID + "\" \\");
        System.out.println("  -H \"X-API-Key: " + API_KEY + "\" \\");
        System.out.println("  -H \"X-Timestamp: " + timestamp + "\" \\");
        System.out.println("  -H \"X-Nonce: " + nonce + "\" \\");
        System.out.println("  -H \"X-Signature: " + signature + "\" \\");
        System.out.println("  \"http://localhost:9189/api/v1" + path + "\"");
    }
    
    /**
     * 生成HMAC-SHA256签名
     */
    private static String generateHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(signature);
        } catch (Exception e) {
            throw new RuntimeException("生成签名失败", e);
        }
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
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}