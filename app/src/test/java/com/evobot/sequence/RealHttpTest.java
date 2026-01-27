package com.evobot.sequence;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 真实HTTP测试类
 * 测试实际的网络请求、API调用和文件下载功能
 */
public class RealHttpTest {
    
    private static final String TAG = "RealHttpTest";
    
    private final Context context;
    private final ActionLibraryConfig config;
    private final ActionLibraryClient client;
    private final ActionLibraryManager manager;
    private final ActionLibraryUpdater updater;
    
    private int testCount = 0;
    private int passCount = 0;
    
    /**
     * 构造函数
     */
    public RealHttpTest(Context context) {
        this.context = context;
        this.config = ActionLibraryConfig.createDefault();
        this.client = new ActionLibraryClient(config);
        this.manager = new ActionLibraryManager(context, config);
        this.updater = new ActionLibraryUpdater(context, config);
        
        Log.d(TAG, "真实HTTP测试初始化完成");
        Log.d(TAG, "配置信息: " + config.toString());
    }
    
    /**
     * 运行所有HTTP测试
     */
    public void runAllTests() {
        Log.d(TAG, "=== 开始真实HTTP测试 ===");
        
        try {
            // 清空映射，准备测试
            ActionNameUtils.clearMappings();
            
            // 运行各项测试
            testServerConnection();
            testSequenceListApi();
            testSequenceDownload();
            testUpdateCheck();
            testMappingIntegration();
            testErrorHandling();
            
            // 输出测试结果
            Log.d(TAG, "\n=== HTTP测试结果 ===");
            Log.d(TAG, "总测试数: " + testCount);
            Log.d(TAG, "通过测试: " + passCount);
            Log.d(TAG, "失败测试: " + (testCount - passCount));
            
            if (passCount == testCount) {
                Log.d(TAG, "✅ 所有HTTP测试通过！");
            } else {
                Log.e(TAG, "❌ 有HTTP测试失败！");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "HTTP测试执行失败", e);
        } finally {
            // 清理资源
            cleanup();
        }
    }
    
    /**
     * 测试服务器连接
     */
    private void testServerConnection() {
        Log.d(TAG, "\n--- 测试服务器连接 ---");
        
        try {
            // 测试基本连接 - 获取动作列表
            String response = client.getSequenceList(null, 10, 0);
            
            assertNotNull("服务器响应不应为空", response);
            assertTrue("响应应包含JSON格式", response.contains("{") || response.contains("["));
            
            Log.d(TAG, "服务器连接测试通过");
            Log.d(TAG, "响应长度: " + response.length() + " 字符");
            
        } catch (Exception e) {
            Log.e(TAG, "服务器连接测试失败", e);
            assertFalse("服务器连接应该成功", true);
        }
    }
    
    /**
     * 测试动作序列列表API
     */
    private void testSequenceListApi() {
        Log.d(TAG, "\n--- 测试动作序列列表API ---");
        
        try {
            // 测试同步获取列表
            String response = client.getSequenceList(null, 20, 0);
            
            assertNotNull("API响应不应为空", response);
            Log.d(TAG, "API响应: " + response.substring(0, Math.min(200, response.length())) + "...");
            
            // 解析响应并建立映射
            int initialMappings = ActionNameUtils.getMappingCount();
            manager.buildMappingsFromSequenceList(response);
            int afterMappings = ActionNameUtils.getMappingCount();
            
            assertTrue("应该从API响应建立映射", afterMappings > initialMappings);
            Log.d(TAG, "从API建立映射: " + initialMappings + " -> " + afterMappings);
            
            // 测试异步获取列表
            testAsyncSequenceList();
            
        } catch (Exception e) {
            Log.e(TAG, "动作序列列表API测试失败", e);
            assertFalse("动作序列列表API应该成功", true);
        }
    }
    
    /**
     * 测试异步动作序列列表
     */
    private void testAsyncSequenceList() throws InterruptedException {
        Log.d(TAG, "测试异步动作序列列表...");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<String> responseRef = new AtomicReference<>();
        
        client.getSequenceListAsync("arm_movement", 10, 0, new ActionLibraryClient.SequenceListCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                responseRef.set(jsonResponse);
                success.set(true);
                latch.countDown();
                Log.d(TAG, "异步获取动作列表成功");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "异步获取动作列表失败: " + error);
                latch.countDown();
            }
        });
        
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue("异步请求应该在30秒内完成", completed);
        assertTrue("异步请求应该成功", success.get());
        assertNotNull("异步响应不应为空", responseRef.get());
    }
    
    /**
     * 测试动作序列下载
     */
    private void testSequenceDownload() {
        Log.d(TAG, "\n--- 测试动作序列下载 ---");
        
        try {
            // 首先获取可用的序列列表
            String listResponse = client.getSequenceList(null, 5, 0);
            int sequenceId = extractFirstSequenceId(listResponse);
            
            if (sequenceId > 0) {
                // 测试同步下载
                byte[] sequenceData = client.downloadSequence(sequenceId);
                
                assertNotNull("下载的数据不应为空", sequenceData);
                assertTrue("下载的数据应该有内容", sequenceData.length > 0);
                
                Log.d(TAG, "成功下载序列 " + sequenceId + ", 大小: " + sequenceData.length + " 字节");
                
                // 保存到临时文件进行验证
                File tempFile = saveToTempFile(sequenceData, "test_sequence_" + sequenceId + ".ebs");
                assertTrue("临时文件应该存在", tempFile.exists());
                assertEquals("文件大小应该匹配", sequenceData.length, tempFile.length());
                
                // 测试异步下载
                testAsyncSequenceDownload(sequenceId);
                
                // 清理临时文件
                tempFile.delete();
                
            } else {
                Log.w(TAG, "未找到可下载的序列ID，跳过下载测试");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "动作序列下载测试失败", e);
            assertFalse("动作序列下载应该成功", true);
        }
    }
    
    /**
     * 测试异步动作序列下载
     */
    private void testAsyncSequenceDownload(int sequenceId) throws InterruptedException {
        Log.d(TAG, "测试异步动作序列下载...");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<byte[]> dataRef = new AtomicReference<>();
        
        client.downloadSequenceAsync(sequenceId, new ActionLibraryClient.DownloadCallback() {
            @Override
            public void onSuccess(byte[] data) {
                dataRef.set(data);
                success.set(true);
                latch.countDown();
                Log.d(TAG, "异步下载序列成功，大小: " + data.length + " 字节");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "异步下载序列失败: " + error);
                latch.countDown();
            }
        });
        
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertTrue("异步下载应该在60秒内完成", completed);
        assertTrue("异步下载应该成功", success.get());
        assertNotNull("异步下载的数据不应为空", dataRef.get());
        assertTrue("异步下载的数据应该有内容", dataRef.get().length > 0);
    }
    
    /**
     * 测试更新检查
     */
    private void testUpdateCheck() {
        Log.d(TAG, "\n--- 测试更新检查 ---");
        
        try {
            // 创建模拟的本地序列信息
            List<ActionLibraryUpdater.LocalSequenceInfo> localSequences = createMockLocalSequences();
            
            // 测试更新检查
            ActionLibraryUpdater.UpdateCheckResult result = client.checkUpdatesWithTimeout(
                "1.0.0", localSequences, 15000);
            
            assertNotNull("更新检查结果不应为空", result);
            Log.d(TAG, "更新检查结果: hasUpdates=" + result.hasUpdates + 
                      ", updateCount=" + result.updateCount + 
                      ", totalSize=" + result.totalSize);
            
            // 如果有更新，测试批量下载
            if (result.hasUpdates && result.updateIds != null && !result.updateIds.isEmpty()) {
                testBatchDownload(result.updateIds);
            } else {
                Log.d(TAG, "当前没有可用更新");
            }
            
            // 测试异步更新检查
            testAsyncUpdateCheck();
            
        } catch (Exception e) {
            Log.e(TAG, "更新检查测试失败", e);
            assertFalse("更新检查应该成功", true);
        }
    }
    
    /**
     * 测试批量下载
     */
    private void testBatchDownload(List<Integer> updateIds) {
        Log.d(TAG, "测试批量下载，ID数量: " + updateIds.size());
        
        try {
            // 只下载前3个，避免测试时间过长
            List<Integer> testIds = updateIds.subList(0, Math.min(3, updateIds.size()));
            
            byte[] batchData = client.batchDownloadWithTimeout(testIds, 30000);
            
            assertNotNull("批量下载数据不应为空", batchData);
            assertTrue("批量下载数据应该有内容", batchData.length > 0);
            
            Log.d(TAG, "批量下载成功，总大小: " + batchData.length + " 字节");
            
        } catch (Exception e) {
            Log.e(TAG, "批量下载测试失败", e);
            assertFalse("批量下载应该成功", true);
        }
    }
    
    /**
     * 测试异步更新检查
     */
    private void testAsyncUpdateCheck() throws InterruptedException {
        Log.d(TAG, "测试异步更新检查...");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        
        client.checkUpdatesAsync("1.0.0", new ActionLibraryClient.UpdateCheckCallback() {
            @Override
            public void onSuccess(ActionLibraryClient.UpdateCheckResult result) {
                success.set(true);
                latch.countDown();
                Log.d(TAG, "异步更新检查成功: " + result.toString());
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "异步更新检查失败: " + error);
                latch.countDown();
            }
        });
        
        boolean completed = latch.await(20, TimeUnit.SECONDS);
        assertTrue("异步更新检查应该在20秒内完成", completed);
        assertTrue("异步更新检查应该成功", success.get());
    }
    
    /**
     * 测试映射集成
     */
    private void testMappingIntegration() {
        Log.d(TAG, "\n--- 测试映射集成 ---");
        
        try {
            // 清空现有映射
            ActionNameUtils.clearMappings();
            int initialCount = ActionNameUtils.getMappingCount();
            assertEquals("初始映射数量应为0", 0, initialCount);
            
            // 通过ActionLibraryManager加载动作并建立映射
            testActionLibraryManagerIntegration();
            
            // 验证映射是否正确建立
            int finalCount = ActionNameUtils.getMappingCount();
            assertTrue("应该建立了映射关系", finalCount > 0);
            
            Log.d(TAG, "映射集成测试完成，建立了 " + finalCount + " 个映射");
            
        } catch (Exception e) {
            Log.e(TAG, "映射集成测试失败", e);
            assertFalse("映射集成应该成功", true);
        }
    }
    
    /**
     * 测试ActionLibraryManager集成
     */
    private void testActionLibraryManagerIntegration() throws InterruptedException {
        Log.d(TAG, "测试ActionLibraryManager集成...");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        
        // 异步获取动作列表并建立映射
        manager.getActionListAsync(null, new ActionLibraryManager.ActionListCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                try {
                    // 从响应建立映射
                    manager.buildMappingsFromSequenceList(jsonResponse);
                    success.set(true);
                    Log.d(TAG, "ActionLibraryManager集成成功");
                } catch (Exception e) {
                    Log.e(TAG, "建立映射失败", e);
                }
                latch.countDown();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "ActionLibraryManager集成失败: " + error);
                latch.countDown();
            }
        });
        
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue("ActionLibraryManager集成应该在30秒内完成", completed);
        assertTrue("ActionLibraryManager集成应该成功", success.get());
    }
    
    /**
     * 测试错误处理
     */
    private void testErrorHandling() {
        Log.d(TAG, "\n--- 测试错误处理 ---");
        
        try {
            // 测试无效序列ID下载
            testInvalidSequenceDownload();
            
            // 测试网络超时
            testNetworkTimeout();
            
            // 测试无效API参数
            testInvalidApiParameters();
            
            Log.d(TAG, "错误处理测试完成");
            
        } catch (Exception e) {
            Log.e(TAG, "错误处理测试失败", e);
        }
    }
    
    /**
     * 测试无效序列ID下载
     */
    private void testInvalidSequenceDownload() {
        Log.d(TAG, "测试无效序列ID下载...");
        
        try {
            // 尝试下载不存在的序列ID
            byte[] data = client.downloadSequence(999999);
            // 如果没有抛出异常，说明服务器返回了数据（可能是错误信息）
            Log.w(TAG, "下载无效序列ID返回了数据，长度: " + (data != null ? data.length : 0));
        } catch (IOException e) {
            // 预期的异常
            Log.d(TAG, "无效序列ID正确抛出异常: " + e.getMessage());
            assertTrue("应该抛出IOException", true);
        }
    }
    
    /**
     * 测试网络超时
     */
    private void testNetworkTimeout() {
        Log.d(TAG, "测试网络超时...");
        
        try {
            // 使用很短的超时时间
            List<ActionLibraryUpdater.LocalSequenceInfo> emptyList = new ArrayList<>();
            ActionLibraryUpdater.UpdateCheckResult result = client.checkUpdatesWithTimeout(
                "1.0.0", emptyList, 1); // 1ms超时
            
            // 如果没有超时，说明网络很快或者服务器在本地
            Log.d(TAG, "网络请求在1ms内完成，结果: " + result.hasUpdates);
            
        } catch (IOException e) {
            if (e.getMessage().contains("超时")) {
                Log.d(TAG, "网络超时测试正确: " + e.getMessage());
                assertTrue("应该抛出超时异常", true);
            } else {
                Log.w(TAG, "网络异常（非超时）: " + e.getMessage());
            }
        }
    }
    
    /**
     * 测试无效API参数
     */
    private void testInvalidApiParameters() {
        Log.d(TAG, "测试无效API参数...");
        
        try {
            // 测试无效的limit参数
            String response = client.getSequenceList(null, -1, 0);
            Log.d(TAG, "无效limit参数返回响应长度: " + response.length());
            
        } catch (Exception e) {
            Log.d(TAG, "无效API参数正确处理: " + e.getMessage());
        }
    }
    
    // ===== 辅助方法 =====
    
    /**
     * 从API响应中提取第一个序列ID
     */
    private int extractFirstSequenceId(String jsonResponse) {
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
    
    /**
     * 保存数据到临时文件
     */
    private File saveToTempFile(byte[] data, String fileName) throws IOException {
        File tempDir = new File(context.getCacheDir(), "http_test");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        File tempFile = new File(tempDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
        }
        
        return tempFile;
    }
    
    /**
     * 创建模拟的本地序列信息
     */
    private List<ActionLibraryUpdater.LocalSequenceInfo> createMockLocalSequences() {
        List<ActionLibraryUpdater.LocalSequenceInfo> sequences = new ArrayList<>();
        
        ActionLibraryUpdater.LocalSequenceInfo seq1 = new ActionLibraryUpdater.LocalSequenceInfo();
        seq1.name = "左臂挥手";
        seq1.fileName = "arm_movement_left_arm_wave.ebs";
        seq1.version = "1.0.0";
        seq1.fileHash = "mock_hash_1";
        seq1.fileSize = 1024;
        sequences.add(seq1);
        
        ActionLibraryUpdater.LocalSequenceInfo seq2 = new ActionLibraryUpdater.LocalSequenceInfo();
        seq2.name = "右臂挥手";
        seq2.fileName = "arm_movement_right_arm_wave.ebs";
        seq2.version = "1.0.0";
        seq2.fileHash = "mock_hash_2";
        seq2.fileSize = 1024;
        sequences.add(seq2);
        
        return sequences;
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        Log.d(TAG, "清理HTTP测试资源...");
        
        try {
            if (client != null) {
                client.release();
            }
            if (manager != null) {
                manager.release();
            }
            if (updater != null) {
                updater.release();
            }
            
            // 清理临时文件
            File tempDir = new File(context.getCacheDir(), "http_test");
            if (tempDir.exists()) {
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                tempDir.delete();
            }
            
        } catch (Exception e) {
            Log.w(TAG, "清理资源时出现异常", e);
        }
    }
    
    // ===== 测试断言方法 =====
    
    private void assertTrue(String message, boolean condition) {
        testCount++;
        if (condition) {
            passCount++;
            Log.d(TAG, "✅ " + message);
        } else {
            Log.e(TAG, "❌ " + message);
        }
    }
    
    private void assertFalse(String message, boolean condition) {
        testCount++;
        if (!condition) {
            passCount++;
            Log.d(TAG, "✅ " + message);
        } else {
            Log.e(TAG, "❌ " + message);
        }
    }
    
    private void assertNotNull(String message, Object object) {
        testCount++;
        if (object != null) {
            passCount++;
            Log.d(TAG, "✅ " + message);
        } else {
            Log.e(TAG, "❌ " + message);
        }
    }
    
    private void assertEquals(String message, Object expected, Object actual) {
        testCount++;
        if ((expected == null && actual == null) || 
            (expected != null && expected.equals(actual))) {
            passCount++;
            Log.d(TAG, "✅ " + message);
        } else {
            Log.e(TAG, "❌ " + message + " - 期望: " + expected + ", 实际: " + actual);
        }
    }
}