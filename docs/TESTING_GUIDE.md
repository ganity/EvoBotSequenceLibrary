# 动态映射功能测试指南

## 概述

本文档说明如何测试动态映射功能的正确性，包括各种测试方法和验证步骤。

## 测试文件说明

### 1. DynamicMappingTest.java
- **功能**：核心映射功能的单元测试
- **测试内容**：
  - 基本映射功能（添加、查找、转换）
  - 文件映射功能（从文件建立映射）
  - 名称匹配功能（中英文匹配）
  - 文件查找功能（智能文件查找）
  - 映射管理功能（清除、计数）
  - 边界情况测试（null值、空字符串、特殊字符）

### 2. TestRunner.java
- **功能**：测试运行器，执行各种测试套件
- **测试内容**：
  - 动态映射测试
  - 集成测试（与其他组件的集成）
  - 性能测试
  - 实际使用场景测试

### 3. TestMain.java
- **功能**：测试入口，提供简单的测试接口
- **测试内容**：
  - 快速验证测试
  - 功能演示
  - 特定功能测试

## 如何运行测试

### 1. 在Android应用中运行

```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 运行快速验证
        TestMain.quickValidation(this);
        
        // 运行完整测试
        TestMain.runAllTests(this);
        
        // 演示动态映射功能
        TestMain.demonstrateDynamicMapping(this);
    }
}
```

### 2. 运行特定测试

```java
// 测试映射功能
TestMain.runSpecificTest(context, "mapping");

// 测试匹配功能
TestMain.runSpecificTest(context, "matching");

// 测试文件功能
TestMain.runSpecificTest(context, "file");

// 测试性能
TestMain.runSpecificTest(context, "performance");
```

### 3. 手动验证

```java
// 清除现有映射
ActionNameUtils.clearMappings();

// 添加测试映射
ActionNameUtils.addMapping("左臂挥手", "arm_movement_left_arm_wave");

// 验证转换
String english = ActionNameUtils.chineseToEnglish("左臂挥手");
Log.d("Test", "转换结果: " + english); // 应该输出: arm_movement_left_arm_wave

// 验证匹配
boolean match = ActionNameUtils.isNameMatch("左臂挥手", "arm_movement_left_arm_wave");
Log.d("Test", "匹配结果: " + match); // 应该输出: true
```

## 测试验证点

### 1. 基本功能验证

- [ ] 映射添加功能正常
- [ ] 中文转英文功能正常
- [ ] 英文转中文功能正常
- [ ] 映射数量统计正确
- [ ] 映射清除功能正常

### 2. 文件处理验证

- [ ] 从文件名提取动作名称正确
- [ ] 从SequenceData建立映射正常
- [ ] 文件查找功能正确
- [ ] 支持多种匹配策略

### 3. 名称匹配验证

- [ ] 中英文名称匹配正确
- [ ] 相同名称匹配正确
- [ ] 不存在映射处理正确
- [ ] 名称类型判断正确

### 4. 集成功能验证

- [ ] EvoBotSequencePlayer集成正常
- [ ] ActionLibraryUpdater集成正常
- [ ] ActionLibraryManager集成正常
- [ ] 后台线程处理正常

### 5. 边界情况验证

- [ ] null值处理正确
- [ ] 空字符串处理正确
- [ ] 特殊字符处理正确
- [ ] 大量数据处理正常

## 预期测试结果

### 1. 快速验证测试
```
=== 快速验证测试 ===
映射数量: 2
转换测试: 左臂挥手 -> arm_movement_left_arm_wave
转换测试: arm_movement_right_arm_wave -> 右臂挥手
匹配测试: true
✅ 快速验证测试通过
```

### 2. 动态映射演示
```
=== 动态映射功能演示 ===
1. 清除映射，当前数量: 0
2. 模拟从API获取动作列表...
   API映射建立完成，当前数量: 3
3. 模拟下载文件并解析...
   文件映射建立完成，当前数量: 5
4. 演示各种使用方式:
   中文转英文: 左臂挥手 -> arm_movement_left_arm_wave
   英文转中文: head_nod_confirm -> 点头确认
   名称匹配: 微笑打招呼 <-> smile_greeting = true
   类型判断: '左臂挥手'是中文=true, 'arm_movement_left_arm_wave'是英文=true
5. 演示在播放器中的使用:
   player.play("左臂挥手", listener);  // 使用中文名称
   player.play("arm_movement_left_arm_wave", listener);  // 使用英文名称
   两种方式都能正确工作，系统会自动处理名称匹配
=== 动态映射功能演示完成 ===
```

### 3. 性能测试结果
```
=== 性能测试 ===
添加1000个映射耗时: <50ms
查找1000次耗时: <10ms
匹配1000次耗时: <20ms
✅ 性能测试完成
```

## 常见问题排查

### 1. 映射建立失败
- 检查输入参数是否为null或空字符串
- 确认SequenceData对象的name字段不为空
- 检查文件名格式是否正确

### 2. 名称匹配失败
- 确认映射关系已经建立
- 检查名称拼写是否正确
- 验证映射数量是否符合预期

### 3. 文件查找失败
- 确认文件存在且可读
- 检查文件名格式（.ebs扩展名）
- 验证映射关系是否正确建立

### 4. 性能问题
- 检查映射数量是否过大
- 确认没有重复建立映射
- 考虑在后台线程中处理大量映射

## 调试技巧

### 1. 启用详细日志
```java
// 在测试前启用详细日志
Log.d("ActionNameUtils", "当前映射数量: " + ActionNameUtils.getMappingCount());
```

### 2. 检查映射状态
```java
// 检查特定映射是否存在
boolean hasMapping = ActionNameUtils.isNameMatch("中文名", "english_name");
Log.d("Debug", "映射存在: " + hasMapping);
```

### 3. 验证转换结果
```java
// 验证双向转换
String english = ActionNameUtils.chineseToEnglish("中文名");
String backToChinese = ActionNameUtils.englishToChinese(english);
Log.d("Debug", "双向转换: 中文名 -> " + english + " -> " + backToChinese);
```

## 总结

通过这些测试，可以全面验证动态映射功能的正确性：

1. **功能完整性**：所有核心功能都能正常工作
2. **数据准确性**：映射关系建立和查找准确
3. **性能表现**：在合理的时间内完成操作
4. **边界处理**：正确处理各种边界情况
5. **集成兼容**：与其他组件良好集成

建议在每次修改相关代码后都运行这些测试，确保功能的稳定性和可靠性。