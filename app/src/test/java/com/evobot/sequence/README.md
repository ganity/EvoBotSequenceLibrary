# EvoBot Sequence Library - Test & Example Files

这个目录包含了EvoBot序列库的测试文件和示例代码。

## 目录结构

### Example Files (示例文件)
- `ApplicationIntegrationExample.java` - 应用集成示例
- `EmergencyStopExample.java` - 急停功能示例
- `EnglishNameExample.java` - 英文名称使用示例
- `IntegrationTestExample.java` - 集成测试示例
- `OfflineFirstActionLibraryExample.java` - 离线优先动作库示例

### Test Files (测试文件)
- `DynamicMappingTest.java` - 动态映射测试
- `FullPlaybackTest.java` - 完整播放测试
- `HttpActionLibraryTestRunner.java` - HTTP动作库测试运行器
- `HttpTestActivity.java` - HTTP测试Activity
- `HttpTestRunner.java` - HTTP测试运行器
- `RealHttpTest.java` - 真实HTTP测试
- `SequencePlayerTest.java` - 序列播放器测试
- `SimpleSequenceTest.java` - 简单序列测试
- `TestMain.java` - 测试主入口
- `TestRunner.java` - 测试运行器

## 使用说明

这些文件已从主源码目录 (`app/src/main/java`) 移动到测试目录 (`app/src/test/java`)，以保持代码结构的清晰性：

- **主源码目录** 只包含核心库文件
- **测试目录** 包含所有测试和示例代码

这样的结构有助于：
1. 保持发布包的精简
2. 明确区分核心代码和测试代码
3. 便于维护和管理

## 运行测试

要运行这些测试，可以直接在Android项目中引用这些类，或者使用相应的测试框架。