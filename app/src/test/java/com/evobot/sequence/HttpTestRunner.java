package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

/**
 * HTTP测试运行器
 * 用于在Android环境中运行真实的HTTP测试
 */
public class HttpTestRunner {
    
    private static final String TAG = "HttpTestRunner";
    
    /**
     * 运行HTTP测试
     */
    public static void runHttpTests(Context context) {
        Log.d(TAG, "=== 开始HTTP测试 ===");
        
        try {
            // 创建并运行真实HTTP测试
            RealHttpTest httpTest = new RealHttpTest(context);
            httpTest.runAllTests();
            
        } catch (Exception e) {
            Log.e(TAG, "HTTP测试运行失败", e);
        }
        
        Log.d(TAG, "=== HTTP测试完成 ===");
    }
    
    /**
     * 快速连接测试
     */
    public static boolean quickConnectionTest(Context context) {
        Log.d(TAG, "执行快速连接测试...");
        
        try {
            ActionLibraryConfig config = ActionLibraryConfig.createDefault();
            ActionLibraryClient client = new ActionLibraryClient(config);
            
            // 尝试获取动作列表
            String response = client.getSequenceList(null, 1, 0);
            
            boolean success = response != null && !response.isEmpty();
            Log.d(TAG, "快速连接测试结果: " + (success ? "成功" : "失败"));
            
            client.release();
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "快速连接测试失败", e);
            return false;
        }
    }
    
    /**
     * 测试映射建立
     */
    public static int testMappingCreation(Context context) {
        Log.d(TAG, "测试映射建立...");
        
        try {
            ActionLibraryConfig config = ActionLibraryConfig.createDefault();
            ActionLibraryManager manager = new ActionLibraryManager(context, config);
            
            // 清空现有映射
            ActionNameUtils.clearMappings();
            
            // 获取动作列表并建立映射
            String response = manager.getSequenceList(null, 20, 0);
            manager.buildMappingsFromSequenceList(response);
            
            int mappingCount = ActionNameUtils.getMappingCount();
            Log.d(TAG, "成功建立 " + mappingCount + " 个映射");
            
            manager.release();
            return mappingCount;
            
        } catch (Exception e) {
            Log.e(TAG, "映射建立测试失败", e);
            return -1;
        }
    }
    
    /**
     * 测试单个动作下载
     */
    public static boolean testSingleActionDownload(Context context) {
        Log.d(TAG, "测试单个动作下载...");
        
        try {
            ActionLibraryConfig config = ActionLibraryConfig.createDefault();
            ActionLibraryClient client = new ActionLibraryClient(config);
            
            // 获取第一个可用的序列ID
            String listResponse = client.getSequenceList(null, 5, 0);
            int sequenceId = extractSequenceId(listResponse);
            
            if (sequenceId > 0) {
                // 下载动作
                byte[] data = client.downloadSequence(sequenceId);
                boolean success = data != null && data.length > 0;
                
                Log.d(TAG, "动作下载测试结果: " + (success ? "成功" : "失败") + 
                          ", 序列ID: " + sequenceId + 
                          ", 数据大小: " + (data != null ? data.length : 0) + " 字节");
                
                client.release();
                return success;
            } else {
                Log.w(TAG, "未找到可下载的序列ID");
                client.release();
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "单个动作下载测试失败", e);
            return false;
        }
    }
    
    /**
     * 从响应中提取序列ID
     */
    private static int extractSequenceId(String jsonResponse) {
        try {
            String[] lines = jsonResponse.split("\n");
            for (String line : lines) {
                if (line.contains("\"id\":")) {
                    String[] parts = line.split("\"id\":\\s*");
                    if (parts.length > 1) {
                        String idStr = parts[1].split("[,}]")[0].trim();
                        return Integer.parseInt(idStr);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "提取序列ID失败", e);
        }
        return -1;
    }
}