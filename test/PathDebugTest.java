import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 路径调试测试
 * 测试不同路径格式的签名生成
 */
public class PathDebugTest {
    
    private static final String ROBOT_ID = "EVOBOT-PRD-00000001";
    private static final String API_KEY = "ak_7x9m2n8p4q1r5s6t";
    
    public static void main(String[] args) {
        System.out.println("=== 路径调试测试 ===");
        
        // 固定参数用于调试
        long timestamp = 1769478361L;
        String nonce = "GbkCCLGVtERcYtlj";
        String method = "GET";
        
        // 测试不同的路径格式
        testPathVariations(method, timestamp, nonce);
        
        // 生成当前时间的签名
        generateCurrentSignature();
    }
    
    /**
     * 测试不同的路径格式
     */
    private static void testPathVariations(String method, long timestamp, String nonce) {
        System.out.println("--- 测试不同路径格式 ---");
        System.out.println("固定参数:");
        System.out.println("  Method: " + method);
        System.out.println("  Timestamp: " + timestamp);
        System.out.println("  Nonce: " + nonce);
        System.out.println();
        
        String[] paths = {
            "/api/v1/sequences/list",
            "/api/v1/sequences/list?limit=5&offset=0",
            "/api/v1/sequences/list?limit=10&offset=0",
            "sequences/list",
            "sequences/list?limit=5&offset=0"
        };
        
        for (String path : paths) {
            String signature = generateSignature(method, path, timestamp, nonce);
            System.out.println("路径: " + path);
            System.out.println("签名: " + signature);
            
            // 检查是否匹配Python生成的签名
            if (signature.equals("a91ce793b9d985bb855122ba609cc48db67b9d96492eada552157af97ed9974f")) {
                System.out.println("✅ 匹配Python生成的签名！");
            }
            System.out.println();
        }
    }
    
    /**
     * 生成当前时间的签名
     */
    private static void generateCurrentSignature() {
        System.out.println("--- 生成当前时间签名 ---");
        
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = generateNonce();
        String method = "GET";
        String path = "/api/v1/sequences/list?limit=5&offset=0";
        
        String signature = generateSignature(method, path, timestamp, nonce);
        
        System.out.println("当前参数:");
        System.out.println("  Method: " + method);
        System.out.println("  Path: " + path);
        System.out.println("  Timestamp: " + timestamp);
        System.out.println("  Nonce: " + nonce);
        System.out.println("  Signature: " + signature);
        System.out.println();
        
        System.out.println("cURL命令:");
        System.out.println("curl -X GET \\");
        System.out.println("  -H \"X-Robot-ID: " + ROBOT_ID + "\" \\");
        System.out.println("  -H \"X-API-Key: " + API_KEY + "\" \\");
        System.out.println("  -H \"X-Timestamp: " + timestamp + "\" \\");
        System.out.println("  -H \"X-Nonce: " + nonce + "\" \\");
        System.out.println("  -H \"X-Signature: " + signature + "\" \\");
        System.out.println("  -H \"Content-Type: application/json\" \\");
        System.out.println("  \"http://localhost:9189" + path + "\"");
    }
    
    /**
     * 生成HMAC-SHA256签名
     */
    private static String generateSignature(String method, String path, long timestamp, String nonce) {
        try {
            // 构建签名字符串: robotId + timestamp + nonce + method + path
            String signatureString = ROBOT_ID + timestamp + nonce + method.toUpperCase() + path;
            
            System.out.println("签名字符串: " + signatureString);
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(API_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            
            byte[] signature = mac.doFinal(signatureString.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(signature);
        } catch (Exception e) {
            throw new RuntimeException("生成签名失败", e);
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