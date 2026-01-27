package com.evobot.sequence;

/**
 * 动作库配置类
 * 包含HTTP服务器连接和认证配置
 */
public class ActionLibraryConfig {
    
    // 默认配置
    public static final String DEFAULT_BASE_URL = "http://localhost:9189/api/v1";
    public static final String DEFAULT_ROBOT_ID = "EVOBOT-PRD-00000001";
    public static final String DEFAULT_API_KEY = "ak_7x9m2n8p4q1r5s6t";
    
    // 超时配置
    public static final int CONNECT_TIMEOUT_MS = 10000;  // 10秒
    public static final int READ_TIMEOUT_MS = 30000;     // 30秒
    
    // 重试配置
    public static final int MAX_RETRY_COUNT = 3;
    public static final int RETRY_DELAY_MS = 1000;      // 1秒
    
    // 缓存配置
    public static final String CACHE_DIR_NAME = "action_library";
    public static final long CACHE_MAX_SIZE = 50 * 1024 * 1024; // 50MB
    
    private final String baseUrl;
    private final String robotId;
    private final String apiKey;
    private final boolean enableCache;
    private final boolean enableCompensation;
    private final boolean enableSafetyCheck;
    
    /**
     * 构造函数
     */
    public ActionLibraryConfig(String baseUrl, String robotId, String apiKey) {
        this(baseUrl, robotId, apiKey, true, true, true);
    }
    
    /**
     * 完整构造函数
     */
    public ActionLibraryConfig(String baseUrl, String robotId, String apiKey, 
                              boolean enableCache, boolean enableCompensation, 
                              boolean enableSafetyCheck) {
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.robotId = robotId != null ? robotId : DEFAULT_ROBOT_ID;
        this.apiKey = apiKey != null ? apiKey : DEFAULT_API_KEY;
        this.enableCache = enableCache;
        this.enableCompensation = enableCompensation;
        this.enableSafetyCheck = enableSafetyCheck;
    }
    
    /**
     * 创建默认配置
     */
    public static ActionLibraryConfig createDefault() {
        return new ActionLibraryConfig(DEFAULT_BASE_URL, DEFAULT_ROBOT_ID, DEFAULT_API_KEY);
    }
    
    // Getters
    public String getBaseUrl() { return baseUrl; }
    public String getRobotId() { return robotId; }
    public String getApiKey() { return apiKey; }
    public boolean isEnableCache() { return enableCache; }
    public boolean isEnableCompensation() { return enableCompensation; }
    public boolean isEnableSafetyCheck() { return enableSafetyCheck; }
    
    @Override
    public String toString() {
        return String.format("ActionLibraryConfig{baseUrl='%s', robotId='%s', cache=%s, compensation=%s, safety=%s}",
            baseUrl, robotId, enableCache, enableCompensation, enableSafetyCheck);
    }
    
    /**
     * Builder模式构建器
     */
    public static class Builder {
        private String baseUrl = DEFAULT_BASE_URL;
        private String robotId = DEFAULT_ROBOT_ID;
        private String apiKey = DEFAULT_API_KEY;
        private boolean enableCache = true;
        private boolean enableCompensation = true;
        private boolean enableSafetyCheck = true;
        
        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        
        public Builder setRobotId(String robotId) {
            this.robotId = robotId;
            return this;
        }
        
        public Builder setApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        public Builder setEnableCache(boolean enableCache) {
            this.enableCache = enableCache;
            return this;
        }
        
        public Builder setEnableCompensation(boolean enableCompensation) {
            this.enableCompensation = enableCompensation;
            return this;
        }
        
        public Builder setEnableSafetyCheck(boolean enableSafetyCheck) {
            this.enableSafetyCheck = enableSafetyCheck;
            return this;
        }
        
        public ActionLibraryConfig build() {
            return new ActionLibraryConfig(baseUrl, robotId, apiKey, enableCache, enableCompensation, enableSafetyCheck);
        }
    }
}