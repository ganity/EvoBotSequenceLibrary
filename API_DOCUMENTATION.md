# EvoBot 动作库管理系统 API Documentation

## 概览

EvoBot 动作库管理系统是一个专门为机器人动作序列提供云端管理服务的RESTful API系统。系统支持动作分类管理、版本控制、自动更新检查等功能。本文档详细描述了所有可用的API接口、数据模型和使用方法。

### 基本信息

- **API版本**: v1
- **基础URL**: `http://your-server:9189/api/v1`
- **协议**: HTTP/HTTPS
- **数据格式**: JSON
- **字符编码**: UTF-8

### 认证方式

系统使用基于HMAC-SHA256的API Key认证机制：

#### API Key认证（推荐）
- **签名算法**：HMAC-SHA256
- **防重放**：时间戳 + 随机字符串（nonce）
- **权限控制**：基于API Key的细粒度权限管理
- **机器人匹配**：API Key与机器人ID模式绑定

#### 认证流程
1. 生成签名：`HMAC-SHA256(robotId + timestamp + nonce + method + path, apiKey)`
2. 设置请求头：
   - `X-Robot-ID`: 机器人ID
   - `X-API-Key`: API密钥
   - `X-Timestamp`: Unix时间戳
   - `X-Nonce`: 随机字符串（16位十六进制）
   - `X-Signature`: 生成的签名

#### 权限级别
- `download`: 下载动作序列
- `sync`: 同步和更新检查
- `upload`: 上传动作序列
- `standard`: 标准机器人权限
- `manage`: 管理分类
- `admin`: 管理员权限（版本管理、关节限位等）

### 通用响应格式

#### 成功响应
```json
{
  "code": 2000,
  "message": "success",
  "data": {
    // 具体数据内容
  }
}
```

#### 错误响应
```json
{
  "code": 4001,
  "message": "错误描述",
  "data": {
    "details": "详细错误信息"
  }
}
```

### 常用错误码

| 错误码 | 描述 |
|--------|------|
| 2000 | 成功 |
| 4000 | 请求参数错误 |
| 4001 | 认证失败（API Key无效或签名错误） |
| 4002 | 零位数据不完整 |
| 4003 | 权限不足 |
| 4004 | 资源不存在 |
| 4005 | 补偿计算失败 |
| 4006 | 安全检查失败 |
| 4007 | 文件格式错误 |
| 4008 | 动作序列未验证 |
| 5001 | 服务器内部错误 |
| 5002 | 数据库错误 |
| 5003 | 存储服务不可用 |
| 5004 | 缓存服务错误 |
## 数据模型

### Sequence (动作序列)
```json
{
  "id": 1,
  "name": "左臂挥手",
  "description": "左臂挥手动作序列",
  "category": "arm_movement",
  "sub_category": "left_arm",
  "tags": ["basic", "greeting"],
  "version": "1.0.0",
  "created_by_robot_id": "EVOBOT-STD-00000001",
  "library_version_id": 1,
  "file_name": "left_arm_wave.ebs",
  "file_size": 1024,
  "file_hash": "abc123def456",
  "oss_path": "sequences/1642857600_left_arm_wave.ebs",
  "duration": 5.5,
  "frame_count": 275,
  "sample_rate": 50.0,
  "status": "verified",
  "download_count": 10,
  "is_public": true,
  "last_modified": "2026-01-22T10:00:00Z",
  "created_at": "2026-01-01T00:00:00Z",
  "updated_at": "2026-01-22T10:00:00Z"
}
```

### CategoryHierarchy (分类层级)
```json
{
  "id": 1,
  "name": "arm_movement",
  "display_name": "手臂动作",
  "parent_id": null,
  "level": 1,
  "sort_order": 1,
  "description": "手臂相关动作分类",
  "is_active": true,
  "sequence_count": 15,
  "created_at": "2026-01-01T00:00:00Z",
  "updated_at": "2026-01-22T10:00:00Z",
  "children": [
    {
      "id": 2,
      "name": "left_arm",
      "display_name": "左臂动作",
      "parent_id": 1,
      "level": 2,
      "sort_order": 1,
      "sequence_count": 8
    }
  ]
}
```
### ActionLibraryVersion (动作库版本)
```json
{
  "id": 1,
  "version": "1.2.0",
  "name": "春季更新版本",
  "description": "包含新的手势动作和优化",
  "is_stable": true,
  "is_default": true,
  "is_published": true,
  "sequence_count": 28,
  "download_count": 150,
  "release_notes": "- 新增5个手势动作\n- 优化动作流畅性\n- 修复已知问题",
  "published_at": "2026-01-22T10:00:00Z",
  "created_by": "admin",
  "created_at": "2026-01-01T00:00:00Z"
}
```

### ZeroPosition (零位数据)
```json
{
  "id": 1,
  "robot_id": "EVOBOT-STD-00000001",
  "type": "standard",
  "version": "v1.0",
  "left_arm_zeros": [2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048],
  "right_arm_zeros": [2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048],
  "left_arm_deviation": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  "right_arm_deviation": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  "max_deviation": 0,
  "deviation_level": "normal",
  "calibration_environment": {
    "temperature": 25.5,
    "humidity": 60.0,
    "technician": "张工程师"
  },
  "verification_status": "verified",
  "created_at": "2026-01-01T00:00:00Z",
  "updated_at": "2026-01-22T10:00:00Z"
}
```
### JointLimitsConfig (关节限位配置)
```json
{
  "id": 1,
  "version": "v1.0",
  "name": "标准关节限位",
  "description": "EvoBot-10DOF标准关节限位配置",
  "is_default": true,
  "limits": [
    {
      "id": 1,
      "config_id": 1,
      "joint_id": 0,
      "joint_name": "左肩关节1",
      "min_position": 0,
      "max_position": 4095,
      "description": "左臂肩部第一关节"
    }
  ],
  "created_by": "admin",
  "created_at": "2026-01-01T00:00:00Z",
  "updated_at": "2026-01-22T10:00:00Z"
}
```

### DownloadLog (下载记录)
```json
{
  "id": 1,
  "sequence_id": 15,
  "robot_id": "EVOBOT-PRD-00000001",
  "compensation": true,
  "safety_check": true,
  "success": true,
  "error_message": "",
  "created_at": "2026-01-22T10:00:00Z"
}
```
## 1. 系统管理 API

### 1.1 健康检查

**GET** `/health`

检查系统运行状态。

#### 响应示例
```json
{
  "status": "ok"
}
```

## 2. 零位管理 API

### 2.1 上传标准零位

**POST** `/api/v1/zero-positions/standard`

上传标准机器人的零位数据。需要标准机器人权限。

#### 请求体
```json
{
  "robot_id": "EVOBOT-STD-00000001",
  "left_arm_zeros": [2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048],
  "right_arm_zeros": [2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048],
  "calibration_environment": {
    "temperature": 25.5,
    "humidity": 60.0,
    "technician": "张工程师"
  },
  "description": "标准零位配置"
}
```

#### 响应示例
```json
{
  "code": 2000,
  "message": "standard zero position uploaded successfully",
  "data": {
    "id": 1,
    "robot_id": "EVOBOT-STD-00000001",
    "type": "standard",
    "left_arm_zeros": [2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048],
    "right_arm_zeros": [2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048],
    "verification_status": "pending",
    "created_at": "2026-01-22T10:00:00Z"
  }
}
```
### 2.2 获取标准零位

**GET** `/api/v1/zero-positions/standard`

获取标准零位数据。

#### 响应示例
```json
{
  "code": 2000,
  "message": "success",
  "data": {
    "id": 1,
    "robot_id": "EVOBOT-STD-00000001",
    "type": "standard",
    "left_arm_zeros": [2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048],
    "right_arm_zeros": [2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048],
    "verification_status": "verified",
    "created_at": "2026-01-22T10:00:00Z"
  }
}
```

### 2.3 上传设备零位

**POST** `/api/v1/zero-positions/{robot_id}`

上传指定设备的零位数据。

#### 路径参数
- `robot_id` (string, required): 机器人ID

#### 请求体
```json
{
  "left_arm_zeros": [2050, 2046, 2048, 2051, 2048, 2047, 2048, 2048, 2048, 2048],
  "right_arm_zeros": [2049, 2047, 2048, 2050, 2048, 2046, 2048, 2048, 2048, 2048],
  "calibration_environment": {
    "temperature": 24.8,
    "humidity": 58.0,
    "technician": "李工程师"
  },
  "description": "设备零位配置"
}
```

### 2.4 获取设备零位

**GET** `/api/v1/zero-positions/{robot_id}`

获取指定设备的零位数据。

### 2.5 获取补偿参数

**GET** `/api/v1/zero-positions/{robot_id}/compensation`

获取设备相对于标准零位的补偿参数。

#### 响应示例
```json
{
  "code": 2000,
  "message": "success",
  "data": {
    "robot_id": "EVOBOT-PRD-00000001",
    "left_arm_deviation": [2, -2, 0, 3, 0, -1, 0, 0, 0, 0],
    "right_arm_deviation": [1, -1, 0, 2, 0, -2, 0, 0, 0, 0],
    "max_deviation": 3,
    "deviation_level": "normal",
    "calculated_at": "2026-01-22T10:00:00Z"
  }
}
```
## 3. 动作序列管理 API

### 3.1 上传动作序列

**POST** `/api/v1/sequences/upload`

上传新的动作序列文件。需要标准机器人权限。

#### 请求体 (multipart/form-data)
- `name` (string, required): 动作名称
- `description` (string): 动作描述
- `category` (string): 主分类
- `sub_category` (string): 子分类
- `tags` (array): 标签数组，JSON格式字符串
- `file` (file, required): EBS格式的动作文件

#### 响应示例
```json
{
  "code": 2000,
  "message": "sequence uploaded successfully",
  "data": {
    "id": 1,
    "name": "左臂挥手",
    "description": "左臂挥手动作序列",
    "category": "arm_movement",
    "sub_category": "left_arm",
    "file_name": "left_arm_wave.ebs",
    "file_size": 1024,
    "file_hash": "abc123def456",
    "status": "pending",
    "created_at": "2026-01-22T10:00:00Z"
  }
}
```

### 3.2 获取动作序列列表

**GET** `/api/v1/sequences/list`

获取动作序列列表，支持分类过滤和分页。

#### 查询参数
- `limit` (int): 每页数量，默认20，最大100
- `offset` (int): 偏移量，默认0
- `category` (string): 主分类过滤
- `sub_category` (string): 子分类过滤
- `categories` (array): 多分类过滤，逗号分隔
- `status` (string): 状态过滤 (pending/verified/rejected)
- `public_only` (bool): 仅显示公开动作，默认true

#### 响应示例
```json
{
  "code": 2000,
  "message": "success",
  "data": {
    "sequences": [
      {
        "id": 1,
        "name": "左臂挥手",
        "description": "左臂挥手动作序列",
        "category": "arm_movement",
        "sub_category": "left_arm",
        "tags": ["basic", "greeting"],
        "version": "1.0.0",
        "duration": 5.5,
        "frame_count": 275,
        "status": "verified",
        "download_count": 10,
        "is_public": true,
        "created_at": "2026-01-01T00:00:00Z"
      }
    ],
    "total": 1,
    "limit": 20,
    "offset": 0
  }
}
```
### 3.3 获取分类统计

**GET** `/api/v1/sequences/statistics`

获取各分类的动作数量统计。

#### 响应示例
```json
{
  "code": 2000,
  "message": "success",
  "data": [
    {
      "category": "arm_movement",
      "sub_category": "left_arm",
      "count": 8,
      "display_name": "左臂动作"
    },
    {
      "category": "arm_movement",
      "sub_category": "right_arm",
      "count": 7,
      "display_name": "右臂动作"
    }
  ]
}
```

### 3.4 获取动作序列详情

**GET** `/api/v1/sequences/{id}/info`

获取指定动作序列的详细信息。

#### 路径参数
- `id` (int, required): 动作序列ID

#### 响应示例
```json
{
  "code": 2000,
  "message": "success",
  "data": {
    "id": 1,
    "name": "左臂挥手",
    "description": "左臂挥手动作序列",
    "category": "arm_movement",
    "sub_category": "left_arm",
    "tags": ["basic", "greeting"],
    "version": "1.0.0",
    "created_by_robot_id": "EVOBOT-STD-00000001",
    "file_name": "left_arm_wave.ebs",
    "file_size": 1024,
    "file_hash": "abc123def456",
    "duration": 5.5,
    "frame_count": 275,
    "sample_rate": 50.0,
    "status": "verified",
    "download_count": 10,
    "is_public": true,
    "last_modified": "2026-01-22T10:00:00Z",
    "created_at": "2026-01-01T00:00:00Z"
  }
}
```
### 3.5 下载动作序列

**GET** `/api/v1/sequences/{id}/download`

下载指定的动作序列文件。

#### 路径参数
- `id` (int, required): 动作序列ID

#### 查询参数
- `robot_id` (string, required): 机器人ID
- `compensation` (bool): 是否应用补偿，默认false
- `safety_check` (bool): 是否进行安全检查，默认false

#### 响应
- **成功**: 返回二进制文件数据，Content-Type为application/octet-stream
- **失败**: 返回JSON错误响应

#### 补偿说明
当`compensation=true`时，系统会：
1. 获取设备的零位偏差数据
2. 对动作序列中的关节位置进行补偿计算
3. 返回补偿后的动作文件

#### 安全检查说明
当`safety_check=true`时，系统会：
1. 检查动作序列是否超出关节限位
2. 验证动作的连续性和平滑性
3. 确保动作执行的安全性

## 4. 分类管理 API

### 4.1 创建分类

**POST** `/api/v1/categories/`

创建新的分类。

#### 请求体
```json
{
  "name": "finger_movement",
  "display_name": "手指动作",
  "parent_id": null,
  "level": 1,
  "sort_order": 3,
  "description": "手指相关动作分类"
}
```

#### 响应示例
```json
{
  "code": 2000,
  "message": "category created successfully",
  "data": {
    "id": 3,
    "name": "finger_movement",
    "display_name": "手指动作",
    "parent_id": null,
    "level": 1,
    "sort_order": 3,
    "description": "手指相关动作分类",
    "is_active": true,
    "sequence_count": 0,
    "created_at": "2026-01-22T10:00:00Z"
  }
}
```
### 4.2 获取分类列表

**GET** `/api/v1/categories/`

获取分类列表。

#### 查询参数
- `level` (int): 分类层级过滤 (1=主分类, 2=子分类)
- `parent_id` (int): 父分类ID过滤
- `active_only` (bool): 仅显示活跃分类，默认true

### 4.3 获取分类树

**GET** `/api/v1/categories/tree`

获取完整的分类树结构。

#### 响应示例
```json
{
  "code": 2000,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "arm_movement",
      "display_name": "手臂动作",
      "level": 1,
      "sequence_count": 15,
      "children": [
        {
          "id": 2,
          "name": "left_arm",
          "display_name": "左臂动作",
          "level": 2,
          "sequence_count": 8
        },
        {
          "id": 3,
          "name": "right_arm",
          "display_name": "右臂动作",
          "level": 2,
          "sequence_count": 7
        }
      ]
    }
  ]
}
```

### 4.4 获取分类统计

**GET** `/api/v1/categories/with-count`

获取带动作数量统计的分类列表。

### 4.5 获取单个分类

**GET** `/api/v1/categories/{id}`

获取指定分类的详细信息。

### 4.6 更新分类

**PUT** `/api/v1/categories/{id}`

更新指定分类的信息。

### 4.7 删除分类

**DELETE** `/api/v1/categories/{id}`

删除指定分类。注意：只能删除没有子分类和动作的分类。
## 5. 版本管理 API

### 5.1 创建动作库版本

**POST** `/api/v1/versions/library`

创建新的动作库版本。

#### 请求体
```json
{
  "version": "1.2.0",
  "name": "春季更新版本",
  "description": "包含新的手势动作和优化",
  "release_notes": "- 新增5个手势动作\n- 优化动作流畅性\n- 修复已知问题",
  "created_by": "admin"
}
```

#### 响应示例
```json
{
  "code": 2000,
  "message": "library version created successfully",
  "data": {
    "id": 2,
    "version": "1.2.0",
    "name": "春季更新版本",
    "description": "包含新的手势动作和优化",
    "is_stable": false,
    "is_default": false,
    "is_published": false,
    "sequence_count": 0,
    "download_count": 0,
    "release_notes": "- 新增5个手势动作\n- 优化动作流畅性\n- 修复已知问题",
    "created_by": "admin",
    "created_at": "2026-01-22T10:00:00Z"
  }
}
```

### 5.2 获取动作库版本列表

**GET** `/api/v1/versions/library`

获取所有动作库版本列表。

#### 查询参数
- `published_only` (bool): 仅显示已发布版本，默认false
- `stable_only` (bool): 仅显示稳定版本，默认false

### 5.3 获取默认动作库版本

**GET** `/api/v1/versions/library/default`

获取当前默认的动作库版本。

#### 响应示例
```json
{
  "code": 2000,
  "message": "success",
  "data": {
    "id": 1,
    "version": "1.0.0",
    "name": "基础版本",
    "is_stable": true,
    "is_default": true,
    "is_published": true,
    "sequence_count": 25,
    "download_count": 150,
    "published_at": "2026-01-01T00:00:00Z"
  }
}
```
### 5.4 获取指定动作库版本

**GET** `/api/v1/versions/library/{id}`

获取指定ID的动作库版本详情。

### 5.5 发布动作库版本

**POST** `/api/v1/versions/library/{id}/publish`

发布指定的动作库版本。

#### 响应示例
```json
{
  "code": 2000,
  "message": "library version published successfully",
  "data": {
    "id": 2,
    "version": "1.2.0",
    "is_published": true,
    "published_at": "2026-01-22T10:00:00Z"
  }
}
```

### 5.6 设置默认动作库版本

**POST** `/api/v1/versions/library/{id}/set-default`

将指定版本设置为默认版本。

#### 响应示例
```json
{
  "code": 2000,
  "message": "default library version updated successfully"
}
```

## 6. 更新检查 API

### 6.1 检查更新

**POST** `/api/v1/updates/check`

检查是否有可用的动作库更新。

#### 请求体
```json
{
  "robot_id": "EVOBOT-PRD-00000001",
  "library_version": "1.0.0",
  "last_sync_time": "2026-01-01T00:00:00Z",
  "sequences": [
    {
      "name": "左臂挥手",
      "category": "arm_movement",
      "file_hash": "abc123def456",
      "version": "1.0.0",
      "file_path": "/data/sequences/left_arm_wave.ebs"
    }
  ]
}
```
#### 响应示例
```json
{
  "code": 2000,
  "message": "update check completed",
  "data": {
    "library_version": "1.2.0",
    "has_updates": true,
    "update_count": 3,
    "total_size": 2048,
    "updates": [
      {
        "id": 15,
        "name": "右臂挥手",
        "category": "arm_movement",
        "sub_category": "right_arm",
        "version": "1.1.0",
        "file_hash": "def456ghi789",
        "file_size": 1024,
        "download_url": "https://oss.example.com/sequences/right_arm_wave.ebs",
        "update_type": "new",
        "change_log": "新增右臂挥手动作",
        "is_required": false
      }
    ],
    "deletions": [],
    "priority": "medium"
  }
}
```

### 6.2 获取更新清单

**GET** `/api/v1/updates/manifest`

获取最新版本的完整更新清单。

#### 查询参数
- `version` (string): 指定版本号，默认为最新版本

### 6.3 获取最新版本信息

**GET** `/api/v1/updates/latest-version`

获取最新的动作库版本信息。

#### 响应示例
```json
{
  "code": 2000,
  "message": "success",
  "data": {
    "version": "1.2.0",
    "name": "春季更新版本",
    "description": "包含新的手势动作和优化",
    "sequence_count": 28,
    "published_at": "2026-01-22T10:00:00Z",
    "release_notes": "- 新增5个手势动作\n- 优化动作流畅性\n- 修复已知问题"
  }
}
```

### 6.4 获取指定版本清单

**GET** `/api/v1/updates/version/{version_id}/manifest`

获取指定版本的动作清单。

### 6.5 批量下载

**POST** `/api/v1/updates/batch-download`

批量下载多个动作序列。

#### 请求体
```json
{
  "robot_id": "EVOBOT-PRD-00000001",
  "sequence_ids": [15, 16, 17],
  "compensation": true,
  "safety_check": true
}
```

#### 响应
返回ZIP格式的压缩文件，包含所有请求的动作序列。
## 7. 关节限位管理 API

### 7.1 创建关节限位配置

**POST** `/api/v1/joint-limits/`

创建新的关节限位配置。

#### 请求体
```json
{
  "version": "v1.0",
  "name": "标准关节限位",
  "description": "EvoBot-10DOF标准关节限位配置",
  "limits": [
    {
      "joint_id": 0,
      "joint_name": "左肩关节1",
      "min_position": 0,
      "max_position": 4095,
      "description": "左臂肩部第一关节"
    },
    {
      "joint_id": 1,
      "joint_name": "左肩关节2",
      "min_position": 0,
      "max_position": 4095,
      "description": "左臂肩部第二关节"
    }
  ],
  "created_by": "admin"
}
```

#### 响应示例
```json
{
  "code": 2000,
  "message": "joint limits config created successfully",
  "data": {
    "id": 1,
    "version": "v1.0",
    "name": "标准关节限位",
    "description": "EvoBot-10DOF标准关节限位配置",
    "is_default": false,
    "limits": [
      {
        "id": 1,
        "joint_id": 0,
        "joint_name": "左肩关节1",
        "min_position": 0,
        "max_position": 4095,
        "description": "左臂肩部第一关节"
      }
    ],
    "created_by": "admin",
    "created_at": "2026-01-22T10:00:00Z"
  }
}
```

### 7.2 获取关节限位配置列表

**GET** `/api/v1/joint-limits/`

获取所有关节限位配置列表。

#### 查询参数
- `include_limits` (bool): 是否包含详细限位数据，默认false

### 7.3 获取默认关节限位配置

**GET** `/api/v1/joint-limits/default`

获取当前默认的关节限位配置。

#### 响应示例
```json
{
  "code": 2000,
  "message": "success",
  "data": {
    "id": 1,
    "version": "v1.0",
    "name": "标准关节限位",
    "is_default": true,
    "limits": [
      {
        "joint_id": 0,
        "joint_name": "左肩关节1",
        "min_position": 0,
        "max_position": 4095
      }
    ]
  }
}
```
### 7.4 获取指定版本关节限位配置

**GET** `/api/v1/joint-limits/{version}`

获取指定版本的关节限位配置。

### 7.5 更新关节限位配置

**PUT** `/api/v1/joint-limits/{version}`

更新指定版本的关节限位配置。

### 7.6 删除关节限位配置

**DELETE** `/api/v1/joint-limits/{version}`

删除指定版本的关节限位配置。

### 7.7 设置默认版本

**POST** `/api/v1/joint-limits/{version}/set-default`

将指定版本设置为默认关节限位配置。

### 7.8 获取单个关节限位

**GET** `/api/v1/joint-limits/{version}/joints/{joint_id}`

获取指定版本中特定关节的限位信息。

#### 路径参数
- `version` (string, required): 配置版本
- `joint_id` (int, required): 关节ID (0-9)

#### 响应示例
```json
{
  "code": 2000,
  "message": "success",
  "data": {
    "joint_id": 0,
    "joint_name": "左肩关节1",
    "min_position": 0,
    "max_position": 4095,
    "description": "左臂肩部第一关节"
  }
}
```

## 使用示例

### 完整的动作下载流程

```bash
# 1. 获取动作列表
curl -X GET "http://your-server:9189/api/v1/sequences/list?category=arm_movement&limit=10"

# 2. 获取动作详情
curl -X GET "http://your-server:9189/api/v1/sequences/15/info"

# 3. 下载动作文件（带补偿和安全检查）
curl -X GET "http://your-server:9189/api/v1/sequences/15/download?robot_id=EVOBOT-PRD-00000001&compensation=true&safety_check=true" \
     -o left_arm_wave.ebs
```
### 版本更新检查流程

```bash
# 1. 检查更新
curl -X POST "http://your-server:9189/api/v1/updates/check" \
     -H "Content-Type: application/json" \
     -d '{
       "robot_id": "EVOBOT-PRD-00000001",
       "library_version": "1.0.0",
       "last_sync_time": "2026-01-01T00:00:00Z",
       "sequences": [
         {
           "name": "左臂挥手",
           "category": "arm_movement",
           "file_hash": "abc123def456",
           "version": "1.0.0"
         }
       ]
     }'

# 2. 批量下载更新
curl -X POST "http://your-server:9189/api/v1/updates/batch-download" \
     -H "Content-Type: application/json" \
     -d '{
       "robot_id": "EVOBOT-PRD-00000001",
       "sequence_ids": [15, 16, 17],
       "compensation": true,
       "safety_check": true
     }' \
     -o updates.zip
```

### 零位管理流程

```bash
# 1. 上传标准零位（标准机器人）
curl -X POST "http://your-server:9189/api/v1/zero-positions/standard" \
     -H "Content-Type: application/json" \
     -d '{
       "robot_id": "EVOBOT-STD-00000001",
       "left_arm_zeros": [2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048],
       "right_arm_zeros": [2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048],
       "calibration_environment": {
         "temperature": 25.5,
         "humidity": 60.0,
         "technician": "张工程师"
       }
     }'

# 2. 上传设备零位
curl -X POST "http://your-server:9189/api/v1/zero-positions/EVOBOT-PRD-00000001" \
     -H "Content-Type: application/json" \
     -d '{
       "left_arm_zeros": [2050, 2046, 2048, 2051, 2048, 2047, 2048, 2048, 2048, 2048],
       "right_arm_zeros": [2049, 2047, 2048, 2050, 2048, 2046, 2048, 2048, 2048, 2048]
     }'

# 3. 获取补偿参数
curl -X GET "http://your-server:9189/api/v1/zero-positions/EVOBOT-PRD-00000001/compensation"
```
### 动作上传流程（标准机器人）

```bash
# 上传动作序列文件
curl -X POST "http://your-server:9189/api/v1/sequences/upload" \
     -F "name=右臂挥手" \
     -F "description=右臂挥手动作序列" \
     -F "category=arm_movement" \
     -F "sub_category=right_arm" \
     -F 'tags=["basic", "greeting"]' \
     -F "file=@right_arm_wave.ebs"
```

### 分类管理流程

```bash
# 1. 创建主分类
curl -X POST "http://your-server:9189/api/v1/categories/" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "finger_movement",
       "display_name": "手指动作",
       "level": 1,
       "sort_order": 3,
       "description": "手指相关动作分类"
     }'

# 2. 创建子分类
curl -X POST "http://your-server:9189/api/v1/categories/" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "thumb",
       "display_name": "拇指动作",
       "parent_id": 3,
       "level": 2,
       "sort_order": 1,
       "description": "拇指相关动作"
     }'

# 3. 获取分类树
curl -X GET "http://your-server:9189/api/v1/categories/tree"
```

## 最佳实践

### 1. 错误处理
- 始终检查响应中的`code`字段
- 对于网络错误实施指数退避重试机制
- 记录详细的错误日志便于调试

```python
import requests
import time
import logging

def api_request_with_retry(url, method='GET', data=None, max_retries=3):
    for attempt in range(max_retries):
        try:
            if method == 'GET':
                response = requests.get(url, timeout=30)
            elif method == 'POST':
                response = requests.post(url, json=data, timeout=30)
            
            result = response.json()
            if result['code'] == 2000:
                return result['data']
            else:
                logging.error(f"API错误: {result['message']}")
                return None
                
        except requests.exceptions.RequestException as e:
            if attempt < max_retries - 1:
                wait_time = 2 ** attempt
                logging.warning(f"请求失败，{wait_time}秒后重试: {e}")
                time.sleep(wait_time)
            else:
                logging.error(f"请求最终失败: {e}")
                return None
```
### 2. 性能优化
- 使用分页获取大量数据
- 缓存不经常变化的数据（如分类信息）
- 批量操作优于单个操作

```python
# 分页获取所有动作序列
def get_all_sequences():
    all_sequences = []
    offset = 0
    limit = 50
    
    while True:
        url = f"http://your-server:9189/api/v1/sequences/list?limit={limit}&offset={offset}"
        result = api_request_with_retry(url)
        
        if not result or not result['sequences']:
            break
            
        all_sequences.extend(result['sequences'])
        
        if len(result['sequences']) < limit:
            break
            
        offset += limit
    
    return all_sequences

# 缓存分类信息
import functools
import time

@functools.lru_cache(maxsize=1)
def get_category_tree_cached():
    url = "http://your-server:9189/api/v1/categories/tree"
    return api_request_with_retry(url)

# 每小时清除缓存
def clear_category_cache():
    get_category_tree_cached.cache_clear()
```

### 3. 安全考虑
- 验证文件哈希确保数据完整性
- 使用安全检查功能避免危险动作
- 定期更新到最新版本

```python
import hashlib

def verify_file_integrity(file_path, expected_hash):
    """验证文件完整性"""
    with open(file_path, 'rb') as f:
        file_hash = hashlib.md5(f.read()).hexdigest()
    
    if file_hash != expected_hash:
        raise ValueError(f"文件完整性验证失败: 期望 {expected_hash}, 实际 {file_hash}")
    
    return True

def safe_download_sequence(sequence_id, robot_id, output_path):
    """安全下载动作序列"""
    # 1. 获取动作信息
    info_url = f"http://your-server:9189/api/v1/sequences/{sequence_id}/info"
    sequence_info = api_request_with_retry(info_url)
    
    if not sequence_info:
        return False
    
    # 2. 下载文件（启用补偿和安全检查）
    download_url = f"http://your-server:9189/api/v1/sequences/{sequence_id}/download"
    params = {
        'robot_id': robot_id,
        'compensation': True,
        'safety_check': True
    }
    
    response = requests.get(download_url, params=params)
    
    if response.status_code == 200:
        with open(output_path, 'wb') as f:
            f.write(response.content)
        
        # 3. 验证文件完整性
        try:
            verify_file_integrity(output_path, sequence_info['file_hash'])
            return True
        except ValueError as e:
            logging.error(f"文件完整性验证失败: {e}")
            return False
    
    return False
```
### 4. 版本管理
- 定期检查更新保持动作库最新
- 使用语义化版本号管理本地动作
- 备份重要的自定义动作

```python
def check_and_update_library(robot_id, current_version, local_sequences):
    """检查并更新动作库"""
    # 1. 检查更新
    check_data = {
        "robot_id": robot_id,
        "library_version": current_version,
        "last_sync_time": "2026-01-01T00:00:00Z",
        "sequences": local_sequences
    }
    
    url = "http://your-server:9189/api/v1/updates/check"
    update_info = api_request_with_retry(url, 'POST', check_data)
    
    if not update_info or not update_info['has_updates']:
        logging.info("动作库已是最新版本")
        return True
    
    # 2. 批量下载更新
    if update_info['updates']:
        sequence_ids = [update['id'] for update in update_info['updates']]
        
        download_data = {
            "robot_id": robot_id,
            "sequence_ids": sequence_ids,
            "compensation": True,
            "safety_check": True
        }
        
        download_url = "http://your-server:9189/api/v1/updates/batch-download"
        response = requests.post(download_url, json=download_data)
        
        if response.status_code == 200:
            # 保存更新包
            with open('updates.zip', 'wb') as f:
                f.write(response.content)
            
            logging.info(f"成功下载 {len(sequence_ids)} 个动作更新")
            return True
    
    return False
```

## 常见问题

### Q: 如何进行API认证？
A: 系统使用基于HMAC-SHA256的API Key认证：
1. 获取API Key和对应的权限配置
2. 生成时间戳和随机nonce
3. 构建签名字符串：`robotId + timestamp + nonce + method + path`
4. 使用API Key对签名字符串进行HMAC-SHA256加密
5. 在请求头中包含所有认证信息

### Q: 签名验证失败怎么办？
A: 检查以下几点：
- API Key是否正确
- 时间戳是否在有效范围内（±5分钟）
- nonce是否重复使用
- 签名算法实现是否正确
- 请求方法和路径是否与签名一致

### Q: 如何获取不同权限的API Key？
A: 在服务器配置文件中定义不同权限的API Key：
- 标准机器人：`standard`, `upload`, `manage`权限
- 生产机器人：`download`, `sync`权限  
- 管理员：`admin`权限（包含所有权限）

### Q: 下载的动作文件格式是什么？
A: 动作文件使用EBS（EvoBot Sequence）格式，这是一种二进制格式包含完整的动作序列数据，包括关节位置、时间戳、采样率等信息。

### Q: 补偿功能是如何工作的？
A: 系统会根据设备的零位偏差自动调整动作参数：
1. 获取设备零位与标准零位的偏差
2. 对动作序列中每个关节位置应用偏差补偿
3. 确保在不同设备上执行效果一致

### Q: 如何处理网络不稳定的情况？
A: 建议实施以下策略：
- 使用指数退避重试机制
- 在本地缓存重要数据
- 实现断点续传功能
- 设置合理的超时时间

### Q: 零位数据包含多少个关节？
A: EvoBot系统支持20个关节（双臂各10个关节），零位数据分为`left_arm_zeros`和`right_arm_zeros`两个数组，每个数组包含10个关节的位置值。

### Q: 关节限位的数值范围是什么？
A: 关节位置值范围为0-4095，这对应12位ADC的分辨率。不同关节的实际限位范围可能不同，需要根据机械结构设置合适的限位值。

### Q: 如何确保动作执行的安全性？
A: 系统提供多层安全保障：
1. 关节限位检查：确保动作不超出机械限位
2. 安全检查功能：验证动作的连续性和平滑性
3. 补偿计算：避免因零位偏差导致的异常动作
4. 文件完整性验证：确保下载的动作文件未损坏

---

**文档版本**: 2.0.0  
**最后更新**: 2026-01-22  
**联系方式**: support@evobot.com
## 认证示例

### Python客户端认证示例

```python
import hashlib
import hmac
import time
import random
import string
import requests

class EvoBot APIClient:
    def __init__(self, base_url, robot_id, api_key):
        self.base_url = base_url
        self.robot_id = robot_id
        self.api_key = api_key
    
    def _generate_nonce(self, length=16):
        return ''.join(random.choices(string.ascii_letters + string.digits, k=length))
    
    def _generate_signature(self, method, path, timestamp, nonce):
        # 构建签名字符串
        signature_string = f"{self.robot_id}{timestamp}{nonce}{method.upper()}{path}"
        
        # 生成HMAC-SHA256签名
        signature = hmac.new(
            self.api_key.encode('utf-8'),
            signature_string.encode('utf-8'),
            hashlib.sha256
        ).hexdigest()
        
        return signature
    
    def _get_auth_headers(self, method, path):
        timestamp = int(time.time())
        nonce = self._generate_nonce()
        signature = self._generate_signature(method, path, timestamp, nonce)
        
        return {
            'X-Robot-ID': self.robot_id,
            'X-API-Key': self.api_key,
            'X-Timestamp': str(timestamp),
            'X-Nonce': nonce,
            'X-Signature': signature,
            'Content-Type': 'application/json'
        }
    
    def get_sequences(self):
        path = '/api/v1/sequences/list'
        headers = self._get_auth_headers('GET', path)
        
        response = requests.get(f"{self.base_url}{path}", headers=headers)
        return response.json()

# 使用示例
client = EvoBot APIClient(
    base_url="http://localhost:9189",
    robot_id="EVOBOT-PRD-00000001",
    api_key="evobot_prd_key_2024_secure_v1"
)

sequences = client.get_sequences()
print(f"找到 {sequences['data']['total']} 个动作序列")
```

### Go客户端认证示例

```go
package main

import (
    "crypto/hmac"
    "crypto/rand"
    "crypto/sha256"
    "encoding/hex"
    "fmt"
    "net/http"
    "strconv"
    "time"
)

type APIClient struct {
    BaseURL string
    RobotID string
    APIKey  string
}

func (c *APIClient) generateNonce() string {
    bytes := make([]byte, 16)
    rand.Read(bytes)
    return hex.EncodeToString(bytes)
}

func (c *APIClient) generateSignature(method, path string, timestamp int64, nonce string) string {
    signatureString := fmt.Sprintf("%s%d%s%s%s", c.RobotID, timestamp, nonce, method, path)
    
    h := hmac.New(sha256.New, []byte(c.APIKey))
    h.Write([]byte(signatureString))
    return hex.EncodeToString(h.Sum(nil))
}

func (c *APIClient) getAuthHeaders(method, path string) map[string]string {
    timestamp := time.Now().Unix()
    nonce := c.generateNonce()
    signature := c.generateSignature(method, path, timestamp, nonce)
    
    return map[string]string{
        "X-Robot-ID":   c.RobotID,
        "X-API-Key":    c.APIKey,
        "X-Timestamp":  strconv.FormatInt(timestamp, 10),
        "X-Nonce":      nonce,
        "X-Signature":  signature,
        "Content-Type": "application/json",
    }
}
```

### cURL认证示例

```bash
#!/bin/bash

# 配置
ROBOT_ID="EVOBOT-PRD-00000001"
API_KEY="evobot_prd_key_2024_secure_v1"
BASE_URL="http://localhost:9189"
METHOD="GET"
PATH="/api/v1/sequences/list"

# 生成认证参数
TIMESTAMP=$(date +%s)
NONCE=$(openssl rand -hex 16)

# 生成签名
SIGNATURE_STRING="${ROBOT_ID}${TIMESTAMP}${NONCE}${METHOD}${PATH}"
SIGNATURE=$(echo -n "$SIGNATURE_STRING" | openssl dgst -sha256 -hmac "$API_KEY" -hex | cut -d' ' -f2)

# 发送请求
curl -X "$METHOD" \
  -H "X-Robot-ID: $ROBOT_ID" \
  -H "X-API-Key: $API_KEY" \
  -H "X-Timestamp: $TIMESTAMP" \
  -H "X-Nonce: $NONCE" \
  -H "X-Signature: $SIGNATURE" \
  -H "Content-Type: application/json" \
  "$BASE_URL$PATH"
```