# 中考祝福墙：后端 Codex 工作文档

## 1. 工作目标

请在后端项目中完成“中考查分工具祝福墙”的接口支持。

前端页面需要在进入中考查分工具后，调用接口并展示 `blessing` 表中最新的 6 条有效祝福。

本任务只修改后端项目，不修改前端项目。

## 2. 业务规则

祝福墙查询的是 `blessing` 表中的全量有效数据：

- 不按学校筛选。
- 不限制 `school_source`。
- 可以同时包含 `score`、`junior`、`general` 等来源。
- 必须排除 `school_name` 为空或空字符串的记录。
- 按最新创建时间倒序排列。
- 创建时间相同时按 `id` 倒序排列。
- 默认返回最新 6 条。
- 接口公开访问，不要求 Authorization。

排序规则必须等价于：

```sql
ORDER BY create_time DESC, id DESC
```

## 3. 接口要求

实现或修正以下接口：

```http
GET /api/blessing/list?page=1&size=6
```

### 3.1 无学校参数时

当请求不带 `schoolSource` 和 `schoolId` 时：

```http
GET /api/blessing/list?page=1&size=6
```

必须查询所有来源的有效祝福。

不得自动追加以下条件：

```sql
school_id IS NULL
```

也不得自动追加：

```sql
school_source = 'score'
```

### 3.2 有学校参数时

原有学校详情页的查询逻辑必须继续保留：

```http
GET /api/blessing/list?page=1&size=20&schoolSource=score&schoolId=52
```

有学校参数时，按以下复合条件查询：

```text
school_source = schoolSource
school_id = schoolId
```

不能只按 `schoolId` 查询，因为不同学校来源的数据表可能存在相同 ID。

如果学校参数不完整或无效，请保持原有接口行为，并确认不会影响无学校参数的全局查询。

## 4. 数据字段要求

数据库字段以实际项目为准，重点确认以下字段：

```text
id
nickname
avatar
school_source
school_id
school_name
content
create_time
```

接口返回字段请使用项目当前前端实际使用的驼峰格式：

```json
{
  "id": 123,
  "nickname": "何意味",
  "avatar": "https://example.com/avatar.jpg",
  "schoolSource": "score",
  "schoolId": 146,
  "schoolName": "上海立信会计金融学院附属中学",
  "content": "祝孩子中考顺利，沉着发挥！",
  "createTime": "2026-09-09 10:30:00"
}
```

特别要求：

- `schoolName` 必须来自 `blessing.school_name`。
- 不需要联表查询学校名称。
- 不要把学校名称重新拼接到 `content` 中。
- `content` 保持数据库原始内容。
- 后端不要为了前端展示而截断或修改 `content`。

## 5. 推荐返回结构

请保持项目现有统一响应结构。如果项目已有固定结构，以项目结构为准，但必须在最终对接文档中明确说明。

推荐结构：

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "records": [
      {
        "id": 123,
        "nickname": "何意味",
        "avatar": "https://example.com/avatar.jpg",
        "schoolSource": "score",
        "schoolId": 146,
        "schoolName": "上海立信会计金融学院附属中学",
        "content": "祝孩子中考顺利，沉着发挥！",
        "createTime": "2026-09-09 10:30:00"
      }
    ],
    "current": 1,
    "pages": 2,
    "total": 8,
    "size": 6
  }
}
```

无有效数据时，也应返回 HTTP 200 和空数组：

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "records": [],
    "current": 1,
    "pages": 0,
    "total": 0,
    "size": 6
  }
}
```

## 6. 后端执行步骤

请按以下顺序工作：

1. 阅读后端项目中祝福相关的 Controller、Service、Mapper/Repository、实体类和现有测试。
2. 确认 `blessing` 表的真实字段名、时间字段和当前分页实现。
3. 检查无学校参数时是否错误追加了 `school_id IS NULL` 或其他学校条件。
4. 修复全局查询逻辑，使其查询所有来源的有效祝福。
5. 增加 `school_name` 非空条件。
6. 增加 `create_time DESC, id DESC` 排序。
7. 确认返回 JSON 使用 `schoolName`、`schoolSource`、`schoolId`、`createTime` 等驼峰字段。
8. 保留并验证带 `schoolSource + schoolId` 的学校详情查询逻辑。
9. 编写或补充自动化测试。
10. 启动本地后端，使用真实接口请求验证。
11. 检查日志、HTTP 状态码和实际 JSON 响应。
12. 生成本文档第 8 节要求的前后端对接文档。

## 7. 必须完成的测试

至少覆盖以下场景：

### 测试 A：全局查询

```http
GET /api/blessing/list?page=1&size=6
```

验证：

- HTTP 200。
- 不需要 Authorization。
- 返回所有来源的数据。
- 返回记录数不超过 6。
- `schoolName` 不为空。
- 排序为 `create_time DESC, id DESC`。
- 不出现“学校参数不正确”。

### 测试 B：分页查询

```http
GET /api/blessing/list?page=2&size=6
```

验证分页字段、分页结果和第一页不重复。

### 测试 C：学校查询

```http
GET /api/blessing/list?page=1&size=20&schoolSource=score&schoolId=52
```

验证：

- 仍然可以按学校查询。
- 只返回 `schoolSource=score` 且 `schoolId=52` 的祝福。
- 不会混入其他来源或相同数字 ID 的学校。

### 测试 D：空学校名

确认数据库中 `school_name` 为空的记录不会出现在全局结果中。

## 8. 工作结束后必须生成的前后端对接文档

后端 Codex 完成代码和测试后，必须在后端项目中新增一份 Markdown 文档，例如：

```text
docs/blessing-wall-frontend-handoff.md
```

文件名可以不同，但必须是 Markdown 文件，并把文件路径告诉用户。

该文档必须包含以下内容：

### 8.1 接口地址

```text
本地完整 URL：
测试环境完整 URL：
HTTP 方法：GET
是否需要 Authorization：是 / 否
```

### 8.2 请求示例

必须提供全局查询请求：

```http
GET /api/blessing/list?page=1&size=6
```

并说明是否需要额外请求头。

### 8.3 完整真实响应

必须提供一次真实接口返回的完整 JSON 示例，至少包含 2 条真实记录，并明确：

- records 实际路径。
- 总记录数。
- 分页字段名称。
- 学校名字段名称。
- 内容字段名称。

### 8.4 字段映射表

至少说明：

| 数据含义 | 实际返回字段 | 类型 |
|---|---|---|
| 昵称 | `nickname` | string |
| 头像 | `avatar` | string/null |
| 学校来源 | `schoolSource` | string |
| 学校 ID | `schoolId` | number/null |
| 学校名 | `schoolName` | string |
| 祝福内容 | `content` | string |
| 创建时间 | `createTime` | string |

### 8.5 前端解析说明

明确告诉前端：

- 返回数据是否是 `data.records`。
- 是否存在二次 `data` 包装。
- 是否可以直接使用 `schoolName`。
- 无数据时的返回结构。
- 请求失败时的错误结构。

### 8.6 验证报告

必须写明：

- 自动化测试命令和结果。
- Maven/Gradle 编译命令和结果。
- 真实接口请求命令和结果。
- 数据库有效祝福数量。
- 被排除的空学校名记录数量。
- 数据来源示例。
- 学校详情筛选验证结果。

## 9. 最终交付格式

工作结束时，请向用户返回以下信息：

```text
后端代码已完成。

修改文件：
- ...

测试结果：
- ...

本地接口：
- ...

前后端对接文档：
- ...
```

没有生成前后端对接 Markdown 文档之前，不要宣称后端任务完成。
