package com.evobot.sequence;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 动作库HTTP客户端
 * 负责与服务器通信，包括认证、下载、更新检查等功能
 */
public class ActionLibraryClient {
    
    private static final String TAG = "ActionLibraryClient";
    private static final int DEFAULT_TIMEOUT_MS = 30000;  // 默认超时30秒
    
    private final ActionLibraryConfig config;
    private final ExecutorService executor;
    private final SecureRandom random;
    
    // 类型别名，方便使用
    public static class UpdateCheckResult extends ActionLibraryUpdater.UpdateCheckResult {
        public UpdateCheckResult(boolean hasUpdates, int updateCount, long totalSize, 
                               String libraryVersion, List<Integer> updateIds) {
            super(hasUpdates, updateCount, totalSize, libraryVersion, updateIds);
        }
    }
    
    /**
     * 构造函数
     */
    public ActionLibraryClient(ActionLibraryConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config不能为null");
        }
        this.config = config;
        this.executor = Executors.newCachedThreadPool();
        this.random = new SecureRandom();
    }
    
    /**
     * 异步检查更新
     */
    public void checkUpdatesAsync(String currentVersion, UpdateCheckCallback callback) {
        executor.execute(() -> {
            try {
                UpdateCheckResult result = checkUpdates(currentVersion);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "检查更新失败", e);
                if (callback != null) {
                    callback.onError("检查更新失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 同步检查更新
     */
    public UpdateCheckResult checkUpdates(String currentVersion) throws IOException {
        ActionLibraryUpdater.UpdateCheckResult result = checkUpdatesWithTimeout(currentVersion, null, DEFAULT_TIMEOUT_MS);
        return new UpdateCheckResult(result.hasUpdates, result.updateCount, result.totalSize, result.libraryVersion, result.updateIds);
    }
    
    /**
     * 检查更新（支持本地动作列表和超时）
     */
    public ActionLibraryUpdater.UpdateCheckResult checkUpdatesWithTimeout(String currentVersion, 
            List<ActionLibraryUpdater.LocalSequenceInfo> localSequences, int timeoutMs) throws IOException {
        
        String path = "/updates/check";
        String requestBody = buildUpdateCheckRequest(currentVersion, localSequences);
        
        String response = makeAuthenticatedRequestWithTimeout("POST", path, requestBody, timeoutMs);
        return parseUpdateCheckResponse(response);
    }
    
    /**
     * 批量下载动作序列（带超时）
     */
    public byte[] batchDownloadWithTimeout(List<Integer> sequenceIds, int timeoutMs) throws IOException {
        String path = "/updates/batch-download";
        String requestBody = buildBatchDownloadRequest(sequenceIds);
        
        return makeAuthenticatedBinaryRequestWithTimeout("POST", path, requestBody, timeoutMs);
    }
    
    /**
     * 异步下载动作序列
     */
    public void downloadSequenceAsync(int sequenceId, DownloadCallback callback) {
        executor.execute(() -> {
            try {
                byte[] data = downloadSequence(sequenceId);
                if (callback != null) {
                    callback.onSuccess(data);
                }
            } catch (Exception e) {
                Log.e(TAG, "下载动作失败", e);
                if (callback != null) {
                    callback.onError("下载失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 同步下载动作序列
     */
    public byte[] downloadSequence(int sequenceId) throws IOException {
        String path = String.format("/sequences/%d/download?robot_id=%s&compensation=%s&safety_check=%s",
            sequenceId, config.getRobotId(), 
            config.isEnableCompensation(), config.isEnableSafetyCheck());
        
        return makeAuthenticatedBinaryRequest("GET", path, null);
    }
    
    /**
     * 获取动作序列列表
     */
    public void getSequenceListAsync(String category, int limit, int offset, SequenceListCallback callback) {
        executor.execute(() -> {
            try {
                String result = getSequenceList(category, limit, offset);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "获取动作列表失败", e);
                if (callback != null) {
                    callback.onError("获取列表失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 同步获取动作序列列表
     */
    public String getSequenceList(String category, int limit, int offset) throws IOException {
        StringBuilder path = new StringBuilder("/sequences/list?");
        path.append("limit=").append(limit);
        path.append("&offset=").append(offset);
        if (category != null && !category.isEmpty()) {
            path.append("&category=").append(category);
        }
        
        return makeAuthenticatedRequest("GET", path.toString(), null);
    }
    
    /**
     * 发送认证请求（JSON响应）
     */
    private String makeAuthenticatedRequest(String method, String path, String requestBody) throws IOException {
        HttpURLConnection connection = null;
        try {
            // 创建连接
            URL url = new URL(config.getBaseUrl() + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(ActionLibraryConfig.CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(ActionLibraryConfig.READ_TIMEOUT_MS);
            
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
            InputStream inputStream = (responseCode >= 200 && responseCode < 300) 
                ? connection.getInputStream() 
                : connection.getErrorStream();
            
            if (inputStream == null) {
                throw new IOException("无法获取响应流，响应码: " + responseCode);
            }
            
            String response = readStringFromStream(inputStream);
            
            if (responseCode >= 200 && responseCode < 300) {
                Log.d(TAG, String.format("请求成功: %s %s -> %d", method, path, responseCode));
                return response;
            } else {
                Log.e(TAG, String.format("请求失败: %s %s -> %d, 响应: %s", method, path, responseCode, response));
                throw new IOException("HTTP请求失败，响应码: " + responseCode + ", 响应: " + response);
            }
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 发送认证请求（JSON响应，带超时）
     */
    private String makeAuthenticatedRequestWithTimeout(String method, String path, String requestBody, int timeoutMs) throws IOException {
        HttpURLConnection connection = null;
        try {
            // 创建连接
            URL url = new URL(config.getBaseUrl() + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            
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
            InputStream inputStream = (responseCode >= 200 && responseCode < 300) 
                ? connection.getInputStream() 
                : connection.getErrorStream();
            
            if (inputStream == null) {
                throw new IOException("无法获取响应流，响应码: " + responseCode);
            }
            
            String response = readStringFromStream(inputStream);
            
            if (responseCode >= 200 && responseCode < 300) {
                Log.d(TAG, String.format("请求成功: %s %s -> %d (超时: %dms)", method, path, responseCode, timeoutMs));
                return response;
            } else {
                Log.e(TAG, String.format("请求失败: %s %s -> %d, 响应: %s", method, path, responseCode, response));
                throw new IOException("HTTP请求失败，响应码: " + responseCode + ", 响应: " + response);
            }
            
        } catch (java.net.SocketTimeoutException e) {
            throw new IOException("网络请求超时 (" + timeoutMs + "ms): " + e.getMessage(), e);
        } catch (java.net.ConnectException e) {
            throw new IOException("网络连接失败: " + e.getMessage(), e);
        } catch (java.net.UnknownHostException e) {
            throw new IOException("无法解析服务器地址: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 发送认证请求（二进制响应，带超时）
     */
    private byte[] makeAuthenticatedBinaryRequestWithTimeout(String method, String path, String requestBody, int timeoutMs) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(config.getBaseUrl() + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            
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
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return readBytesFromStream(connection.getInputStream());
            } else {
                String errorResponse = readStringFromStream(connection.getErrorStream());
                throw new IOException("HTTP请求失败，响应码: " + responseCode + ", 响应: " + errorResponse);
            }
            
        } catch (java.net.SocketTimeoutException e) {
            throw new IOException("网络请求超时 (" + timeoutMs + "ms): " + e.getMessage(), e);
        } catch (java.net.ConnectException e) {
            throw new IOException("网络连接失败: " + e.getMessage(), e);
        } catch (java.net.UnknownHostException e) {
            throw new IOException("无法解析服务器地址: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    private byte[] makeAuthenticatedBinaryRequest(String method, String path, String requestBody) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(config.getBaseUrl() + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(ActionLibraryConfig.CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(ActionLibraryConfig.READ_TIMEOUT_MS);
            
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
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return readBytesFromStream(connection.getInputStream());
            } else {
                String errorResponse = readStringFromStream(connection.getErrorStream());
                throw new IOException("HTTP请求失败，响应码: " + responseCode + ", 响应: " + errorResponse);
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
    private void setAuthHeaders(HttpURLConnection connection, String method, String path) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String nonce = generateNonce();
            String signature = generateSignature(method, path, timestamp, nonce);
            
            connection.setRequestProperty("X-Robot-ID", config.getRobotId());
            connection.setRequestProperty("X-API-Key", config.getApiKey());
            connection.setRequestProperty("X-Timestamp", String.valueOf(timestamp));
            connection.setRequestProperty("X-Nonce", nonce);
            connection.setRequestProperty("X-Signature", signature);
            
            Log.d(TAG, String.format("认证头: robotId=%s, timestamp=%d, nonce=%s", 
                config.getRobotId(), timestamp, nonce));
                
        } catch (Exception e) {
            Log.e(TAG, "生成认证头失败", e);
            throw new RuntimeException("认证失败", e);
        }
    }
    
    /**
     * 生成随机nonce
     */
    private String generateNonce() {
        // 使用字母和数字生成16位随机字符串，与Python版本保持一致
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder nonce = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            nonce.append(chars.charAt(random.nextInt(chars.length())));
        }
        return nonce.toString();
    }
    
    /**
     * 生成HMAC-SHA256签名
     */
    private String generateSignature(String method, String path, long timestamp, String nonce) throws Exception {
        // 移除查询参数，只使用路径部分进行签名
        String pathForSignature = path;
        int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            pathForSignature = path.substring(0, queryIndex);
        }
        
        String signatureString = config.getRobotId() + timestamp + nonce + method.toUpperCase() + pathForSignature;
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(config.getApiKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        
        byte[] signature = mac.doFinal(signatureString.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(signature);
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * 从输入流读取字符串
     */
    private String readStringFromStream(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }
        }
        return result.toString();
    }
    
    /**
     * 从输入流读取字节数组
     */
    private byte[] readBytesFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
    
    /**
     * 构建更新检查请求体（支持本地动作列表）
     */
    private String buildUpdateCheckRequest(String currentVersion, 
            List<ActionLibraryUpdater.LocalSequenceInfo> localSequences) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"robot_id\":\"").append(config.getRobotId()).append("\",");
        sb.append("\"library_version\":\"").append(currentVersion != null ? currentVersion : "1.0.0").append("\",");
        sb.append("\"last_sync_time\":\"2026-01-01T00:00:00Z\",");
        sb.append("\"sequences\":[");
        
        if (localSequences != null && !localSequences.isEmpty()) {
            for (int i = 0; i < localSequences.size(); i++) {
                ActionLibraryUpdater.LocalSequenceInfo seq = localSequences.get(i);
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"name\":\"").append(seq.name != null ? seq.name : "").append("\",");
                sb.append("\"category\":\"arm_movement\",");
                sb.append("\"file_hash\":\"").append(seq.fileHash != null ? seq.fileHash : "").append("\",");
                sb.append("\"version\":\"").append(seq.version != null ? seq.version : "1.0.0").append("\",");
                sb.append("\"file_path\":\"").append(seq.filePath != null ? seq.filePath : "").append("\"");
                sb.append("}");
            }
        }
        
        sb.append("]}");
        return sb.toString();
    }
    
    /**
     * 构建批量下载请求体
     */
    private String buildBatchDownloadRequest(List<Integer> sequenceIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"robot_id\":\"").append(config.getRobotId()).append("\",");
        sb.append("\"sequence_ids\":[");
        
        if (sequenceIds != null && !sequenceIds.isEmpty()) {
            for (int i = 0; i < sequenceIds.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(sequenceIds.get(i));
            }
        }
        
        sb.append("],");
        sb.append("\"compensation\":").append(config.isEnableCompensation()).append(",");
        sb.append("\"safety_check\":").append(config.isEnableSafetyCheck());
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 解析更新检查响应
     */
    private ActionLibraryUpdater.UpdateCheckResult parseUpdateCheckResponse(String response) {
        // 简化的解析，实际使用时应该使用JSON解析库
        boolean hasUpdates = response.contains("\"has_updates\":true");
        int updateCount = 0;
        long totalSize = 0;
        String libraryVersion = "1.0.0";
        List<Integer> updateIds = new ArrayList<>();
        
        // 简单提取update_count
        String[] parts = response.split("\"update_count\":");
        if (parts.length > 1) {
            String countStr = parts[1].split(",")[0].trim();
            try {
                updateCount = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "解析update_count失败: " + countStr);
            }
        }
        
        // 简单提取total_size
        parts = response.split("\"total_size\":");
        if (parts.length > 1) {
            String sizeStr = parts[1].split(",")[0].trim();
            try {
                totalSize = Long.parseLong(sizeStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "解析total_size失败: " + sizeStr);
            }
        }
        
        // 简单提取library_version
        parts = response.split("\"library_version\":\"");
        if (parts.length > 1) {
            String versionStr = parts[1].split("\"")[0];
            libraryVersion = versionStr;
        }
        
        // 简单提取更新ID列表（从updates数组中提取id字段）
        if (hasUpdates && response.contains("\"updates\":[")) {
            String updatesSection = response.split("\"updates\":\\[")[1].split("\\]")[0];
            String[] updateItems = updatesSection.split("\\{");
            
            for (String item : updateItems) {
                if (item.contains("\"id\":")) {
                    String idStr = item.split("\"id\":")[1].split(",")[0].trim();
                    try {
                        updateIds.add(Integer.parseInt(idStr));
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "解析更新ID失败: " + idStr);
                    }
                }
            }
        }
        
        return new ActionLibraryUpdater.UpdateCheckResult(hasUpdates, updateCount, totalSize, libraryVersion, updateIds);
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    
    // 回调接口
    public interface UpdateCheckCallback {
        void onSuccess(UpdateCheckResult result);
        void onError(String error);
    }
    
    public interface DownloadCallback {
        void onSuccess(byte[] data);
        void onError(String error);
    }
    
    public interface SequenceListCallback {
        void onSuccess(String jsonResponse);
        void onError(String error);
    }
}