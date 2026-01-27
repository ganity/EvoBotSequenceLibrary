# 动态映射实现说明

## 概述

本文档说明了动作名称动态映射机制的实现方案，解决了中英文名称对应关系的维护问题。

## 问题背景

1. **API变化**：动作序列接口新增了 `english_name` 字段
2. **文件命名**：下载的文件使用英文名称，但文件内容包含中文名称
3. **映射需求**：需要建立中文名称和英文名称的对应关系
4. **动态特性**：映射关系不能写死，需要根据实际的动作数据动态建立

## 解决方案

### 1. 动态映射机制

#### 核心思想
- **运行时建立**：映射关系在运行时通过解析实际数据建立
- **多源建立**：从文件解析、API响应、本地扫描等多个来源建立映射
- **内存存储**：映射关系存储在内存中，确保快速访问
- **线程安全**：使用 `ConcurrentHashMap` 保证多线程安全

#### 映射建立时机
1. **应用初始化**：扫描本地已有文件，解析并建立映射
2. **文件下载**：下载新文件后，解析文件内容建立映射
3. **API调用**：获取动作列表时，从响应中提取映射关系
4. **按需建立**：在需要时动态建立映射关系

### 2. 核心类设计

#### ActionNameUtils（工具类）
```java
public class ActionNameUtils {
    // 动态维护的映射表
    private static final Map<String, String> CHINESE_TO_ENGLISH = new ConcurrentHashMap<>();
    private static final Map<String, String> ENGLISH_TO_CHINESE = new ConcurrentHashMap<>();
    private static final Map<String, String> FILENAME_TO_CHINESE = new ConcurrentHashMap<>();
    
    // 核心方法
    public static void addMapping(String chineseName, String englishName);
    public static void addMappingFromFile(String fileName, SequenceData sequenceData);
    public static boolean isNameMatch(String name1, String name2);
    public static File findMatchingFile(String actionName, File[] availableFiles);
}
```

#### ActionInfo（数据类）
```java
public class ActionInfo {
    public String name;           // 中文名称
    public String englishName;    // 英文名称
    // 其他字段...
}
```

### 3. 集成点

#### EvoBotSequencePlayer
- **初始化时**：调用 `initializeActionMappings()` 扫描本地文件
- **后台执行**：在后台线程中建立映射，不阻塞主线程

#### ActionLibraryUpdater
- **文件下载后**：调用 `buildMappingForDownloadedFile()` 解析文件建立映射
- **批量初始化**：提供 `initializeMappings()` 方法扫描所有本地文件

#### ActionLibraryManager
- **API调用时**：调用 `buildMappingsFromSequenceList()` 从API响应建立映射
- **自动集成**：在下载动作时自动建立映射关系

### 4. 文件查找逻辑

#### 智能匹配算法
```java
public static File findMatchingFile(String actionName, File[] availableFiles) {
    for (File file : availableFiles) {
        String fileName = file.getName();
        String fileBaseName = extractActionNameFromFileName(fileName);
        
        // 1. 直接匹配文件名
        if (fileBaseName.equals(actionName)) return file;
        
        // 2. 通过映射匹配
        if (isNameMatch(actionName, fileBaseName)) return file;
        
        // 3. 检查文件名到中文名的映射
        String chineseFromFile = FILENAME_TO_CHINESE.get(fileName);
        if (chineseFromFile != null && isNameMatch(actionName, chineseFromFile)) {
            return file;
        }
    }
    return null;
}
```

#### 匹配优先级
1. **精确匹配**：直接匹配文件名
2. **映射匹配**：通过中英文映射匹配
3. **交叉匹配**：中文名匹配英文名，英文名匹配中文名
4. **文件映射**：通过文件名到中文名的映射匹配

## 实现细节

### 1. 线程安全
- 使用 `ConcurrentHashMap` 确保多线程环境下的安全性
- 映射建立在后台线程中进行，不阻塞主线程
- 读取操作无锁，写入操作线程安全

### 2. 性能优化
- **内存存储**：映射关系存储在内存中，访问速度快
- **懒加载**：按需建立映射关系，避免不必要的开销
- **批量处理**：支持批量建立映射关系

### 3. 错误处理
- **容错机制**：映射建立失败不影响主要功能
- **日志记录**：详细记录映射建立过程和结果
- **降级处理**：映射失败时使用原始名称

### 4. 扩展性
- **插件化**：支持从不同数据源建立映射
- **可配置**：映射策略可以根据需要调整
- **向后兼容**：保持对现有代码的兼容性

## 使用示例

### 1. 基本使用
```java
// 系统会自动建立映射，开发者只需正常调用
player.play("左臂挥手", listener);  // 中文名称
player.play("arm_movement_left_arm_wave", listener);  // 英文名称
```

### 2. 映射状态检查
```java
// 检查映射数量
int count = ActionNameUtils.getMappingCount();
Log.d(TAG, "当前映射数量: " + count);

// 检查特定映射
boolean hasMapping = ActionNameUtils.isNameMatch("左臂挥手", "arm_movement_left_arm_wave");
Log.d(TAG, "映射存在: " + hasMapping);
```

### 3. 手动映射管理
```java
// 手动添加映射（通常不需要）
ActionNameUtils.addMapping("自定义动作", "custom_action");

// 清除映射（谨慎使用）
ActionNameUtils.clearMappings();
```

## 优势

### 1. 灵活性
- **动态适应**：自动适应服务器上的动作定义变化
- **无需维护**：不需要手动维护映射表
- **实时更新**：随着新动作下载自动更新映射

### 2. 可靠性
- **数据驱动**：基于实际数据建立映射，准确性高
- **容错能力**：映射失败不影响基本功能
- **向后兼容**：保持对现有代码的完全兼容

### 3. 性能
- **内存存储**：快速访问映射关系
- **智能匹配**：多种匹配策略确保找到正确文件
- **后台处理**：不阻塞主线程

## 注意事项

### 1. 内存管理
- 映射关系存储在内存中，应用重启后需要重新建立
- 大量动作可能占用较多内存，但通常可以接受

### 2. 初始化时间
- 首次启动时需要扫描本地文件建立映射
- 在后台线程中执行，不影响用户体验

### 3. 数据一致性
- 确保文件名和文件内容的一致性
- 服务器端需要保证 `name` 和 `english_name` 的对应关系

## 总结

动态映射机制成功解决了中英文名称对应关系的维护问题，具有以下特点：

1. **自动化**：无需手动维护映射关系
2. **准确性**：基于实际数据建立映射
3. **灵活性**：适应动作定义的变化
4. **兼容性**：保持向后兼容
5. **性能**：高效的查找和匹配机制

这种设计确保了系统在支持英文名称的同时，保持了良好的用户体验和开发体验。