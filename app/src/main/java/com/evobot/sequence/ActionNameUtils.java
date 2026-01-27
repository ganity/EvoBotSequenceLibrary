package com.evobot.sequence;

import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.io.File;

/**
 * 动作名称工具类
 * 动态维护中文名称和英文名称之间的映射关系
 * 映射关系通过解析下载的动作文件动态建立
 */
public class ActionNameUtils {
    
    private static final String TAG = "ActionNameUtils";
    
    // 动态维护的中英文映射表
    private static final Map<String, String> CHINESE_TO_ENGLISH = new ConcurrentHashMap<>();
    private static final Map<String, String> ENGLISH_TO_CHINESE = new ConcurrentHashMap<>();
    
    // 文件名到中文名的映射（从解析的动作文件中获取）
    private static final Map<String, String> FILENAME_TO_CHINESE = new ConcurrentHashMap<>();
    
    /**
     * 判断是否为英文名称
     * 英文名称通常包含下划线且不含中文字符
     */
    public static boolean isEnglishName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        // 检查是否包含中文字符
        for (char c : name.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return false;
            }
        }
        
        // 英文名称通常包含下划线
        return name.contains("_") && name.matches("^[a-z0-9_]+$");
    }
    
    /**
     * 判断是否为中文名称
     */
    public static boolean isChineseName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        // 检查是否包含中文字符
        for (char c : name.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 中文名称转英文名称
     */
    public static String chineseToEnglish(String chineseName) {
        if (chineseName == null || chineseName.isEmpty()) {
            return chineseName;
        }
        
        return CHINESE_TO_ENGLISH.getOrDefault(chineseName, chineseName);
    }
    
    /**
     * 英文名称转中文名称
     */
    public static String englishToChinese(String englishName) {
        if (englishName == null || englishName.isEmpty()) {
            return englishName;
        }
        
        return ENGLISH_TO_CHINESE.getOrDefault(englishName, englishName);
    }
    
    /**
     * 动态添加映射关系
     * 当下载并解析动作文件时调用此方法建立映射
     * 
     * @param chineseName 从动作文件中解析出的中文名称
     * @param englishName 文件名对应的英文名称（去掉.ebs扩展名）
     */
    public static void addMapping(String chineseName, String englishName) {
        if (chineseName != null && !chineseName.isEmpty() && 
            englishName != null && !englishName.isEmpty()) {
            
            CHINESE_TO_ENGLISH.put(chineseName, englishName);
            ENGLISH_TO_CHINESE.put(englishName, chineseName);
            
            Log.d(TAG, String.format("添加映射: '%s' <-> '%s'", chineseName, englishName));
        }
    }
    
    /**
     * 从文件名和解析的动作数据建立映射
     * 
     * @param fileName 动作文件名（如：arm_movement_left_arm_wave.ebs）
     * @param sequenceData 解析后的动作数据，包含中文名称
     */
    public static void addMappingFromFile(String fileName, SequenceData sequenceData) {
        if (fileName == null || sequenceData == null) {
            return;
        }
        
        String englishName = extractActionNameFromFileName(fileName);
        String chineseName = sequenceData.name; // SequenceData.name 字段包含中文名称
        
        if (chineseName != null && !chineseName.isEmpty()) {
            addMapping(chineseName, englishName);
            FILENAME_TO_CHINESE.put(fileName, chineseName);
        }
    }
    
    /**
     * 清除所有映射（用于重新初始化）
     */
    public static void clearMappings() {
        CHINESE_TO_ENGLISH.clear();
        ENGLISH_TO_CHINESE.clear();
        FILENAME_TO_CHINESE.clear();
        Log.d(TAG, "清除所有映射");
    }
    
    /**
     * 获取当前映射数量
     */
    public static int getMappingCount() {
        return CHINESE_TO_ENGLISH.size();
    }
    
    /**
     * 获取标准化的动作名称（优先返回英文名称）
     */
    public static String getStandardName(String actionName) {
        if (actionName == null || actionName.isEmpty()) {
            return actionName;
        }
        
        if (isEnglishName(actionName)) {
            return actionName;
        } else if (isChineseName(actionName)) {
            return chineseToEnglish(actionName);
        }
        
        return actionName;
    }
    
    /**
     * 检查两个动作名称是否匹配（支持中英文混合匹配）
     */
    public static boolean isNameMatch(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }
        
        if (name1.equals(name2)) {
            return true;
        }
        
        // 尝试转换后比较
        String english1 = chineseToEnglish(name1);
        String english2 = chineseToEnglish(name2);
        if (english1.equals(english2)) {
            return true;
        }
        
        String chinese1 = englishToChinese(name1);
        String chinese2 = englishToChinese(name2);
        if (chinese1.equals(chinese2)) {
            return true;
        }
        
        // 交叉匹配
        if (english1.equals(name2) || chinese1.equals(name2) ||
            english2.equals(name1) || chinese2.equals(name1)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 根据输入名称查找对应的文件
     * 支持中文名称和英文名称查找
     * 
     * @param actionName 动作名称（中文或英文）
     * @param availableFiles 可用的文件列表
     * @return 匹配的文件，如果没找到返回null
     */
    public static File findMatchingFile(String actionName, File[] availableFiles) {
        if (actionName == null || availableFiles == null) {
            return null;
        }
        
        for (File file : availableFiles) {
            if (!file.getName().endsWith(".ebs")) {
                continue;
            }
            
            String fileName = file.getName();
            String fileBaseName = extractActionNameFromFileName(fileName);
            
            // 直接匹配文件名
            if (fileBaseName.equals(actionName)) {
                return file;
            }
            
            // 通过映射匹配
            if (isNameMatch(actionName, fileBaseName)) {
                return file;
            }
            
            // 检查是否有从文件名到中文名的映射
            String chineseFromFile = FILENAME_TO_CHINESE.get(fileName);
            if (chineseFromFile != null && isNameMatch(actionName, chineseFromFile)) {
                return file;
            }
        }
        
        return null;
    }
    
    /**
     * 从文件名提取动作名称
     */
    public static String extractActionNameFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }
        
        if (fileName.endsWith(".ebs")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        
        return fileName;
    }
    
    /**
     * 生成文件名（基于动作名称）
     */
    public static String generateFileName(String actionName) {
        if (actionName == null || actionName.isEmpty()) {
            return "unknown.ebs";
        }
        
        // 使用标准化名称（英文）作为文件名
        String standardName = getStandardName(actionName);
        return standardName + ".ebs";
    }
}