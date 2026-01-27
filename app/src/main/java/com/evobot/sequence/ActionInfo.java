package com.evobot.sequence;

/**
 * 动作信息类
 * 用于存储从API获取的动作基本信息
 */
public class ActionInfo {
    public int id;
    public String name;           // 中文名称
    public String englishName;    // 英文名称
    public String description;
    public String category;
    public String subCategory;
    public String version;
    public String fileHash;
    public long fileSize;
    public String fileName;
    public boolean isPublic;
    public String status;
    public long lastModified;
    
    public ActionInfo() {
    }
    
    public ActionInfo(int id, String name, String englishName) {
        this.id = id;
        this.name = name;
        this.englishName = englishName;
    }
    
    @Override
    public String toString() {
        return String.format("ActionInfo{id=%d, name='%s', englishName='%s', category='%s'}", 
            id, name, englishName, category);
    }
}