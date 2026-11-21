# 查分工具：学校名额与志愿查询后端 Codex 工作文档

## 1. 目标

为“查分工具”提供学校检索和学校详情招生数据接口。用户选择学段（初中/高中）并填写学校名后，进入学校详情页，展示对应招生数据：

- 高中：名额到区、名额到校、1-15 志愿；
- 初中：仅名额到校。

后端完成实现、数据库核对、接口联调和部署说明后，必须在 `对接文档/` 下生成一份《查分工具学校名额与志愿查询_前端对接文档_日期.md》，供前端 Codex 直接执行。该前端文档必须写明最终实际接口、请求参数、响应示例、字段含义、鉴权要求和验收结果，不得只写计划接口。

## 2. 现有前端调用约定（必须兼容）

当前小程序详情页已经使用以下路径：

```text
GET /api/score-query/region?year=2025-2026
GET /api/score-query/school?year=2025-2026
GET /api/score-query/volunteer?year=2025-2026
```

初中名额到校调用 `/api/score-query/school`，并额外传：

```text
GET /api/score-query/school?year=2025-2026&district={学校所在区}
```

如需调整路径或参数，必须同时保留上述兼容形式，或在前端对接文档中给出明确迁移方案；不得让前端自行猜测。

## 3. 必须实现的接口

### 3.1 学校搜索接口

建议实现：

```http
GET /api/score-query/schools/search?level=junior|high&name={学校名}&page=1&size=20
```

要求：

1. `level` 必填，`junior` 表示初中，`high` 表示高中；同时兼容前端可能使用的 `senior` 别名，最终在交接文档中固定一种写法。
2. `name` 必填，去除首尾空格；支持学校全名、简称或包含匹配。
3. 返回学校唯一 `id`、学校名 `name`、简称 `shortName`、所在区 `district`、学段 `level` 和详情页所需的 `schoolSource`。
4. 初高中学校 ID 必须可区分，不能因不同表同 ID 而串校。
5. 无结果返回空数组，不得返回 500。

成功响应统一使用项目既有格式：

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "records": [
      {
        "id": 1001,
        "name": "示例学校",
        "shortName": "示例学校",
        "district": "徐汇区",
        "level": "high",
        "schoolSource": "score"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 20,
    "pages": 1
  }
}
```

### 3.2 学校详情接口

高中详情继续兼容：

```http
GET /api/score-query/schools/{id}
```

初中详情继续兼容：

```http
GET /api/junior-schools/{id}
```

详情至少返回：`id`、`name`、`shortName`、`district`、学段标识，以及当前详情页正常渲染所需的学校基础字段。不存在时返回明确业务错误（例如 `code: 404`），不要返回空对象。

### 3.3 招生数据接口

#### 高中名额到区

```http
GET /api/score-query/region?year={年份}
```

每条记录至少包含：

```json
{
  "id": 1,
  "highSchoolId": 1001,
  "schoolName": "示例高中",
  "targetDistrict": "徐汇区",
  "quotaCount": 20,
  "minScore": 680,
  "changeRange": 3
}
```

#### 高中名额到校

```http
GET /api/score-query/school?year={年份}
```

每条记录至少包含：`id`、`highSchoolId`、`schoolName`、`targetSchoolName`、`targetDistrict`、`quotaCount`、`minScore`、`changeRange`。

#### 高中 1-15 志愿

```http
GET /api/score-query/volunteer?year={年份}
```

每条记录至少包含：`id`、`highSchoolId`、`schoolName`、`targetDistrict`、`planCount`、`minScore`、`changeRange`、`moduleType`。`moduleType=1` 表示详情页 1-15 志愿数据，其他类型不得混入该结果集。

#### 初中名额到校

沿用：

```http
GET /api/score-query/school?year={年份}&district={区}
```

每条记录至少包含：`id`、`highSchoolName`、`highSchoolDistrict`、`targetSchoolName`、`quotaCount`、`minScore`、`changeRange`。`targetSchoolName` 用于匹配用户选择的初中名称。

## 4. 数据和响应规则

- `year` 必须支持 `2025-2026`，并明确默认年份；禁止把年份写死在 SQL 中。
- 数字字段返回数字或 `null`，不要返回不可解析的空字符串；未公布最低分使用 `null`。
- `changeRange` 约定为相对上一统计口径的分数变化：正数上升、负数下降、0 持平；无数据为 `null`。
- 列表按学校名、目标区域和业务规定顺序稳定排序。
- 接口必须防止高中数据和初中数据交叉返回。
- 查询接口是否需要 JWT 必须明确；若为公开查分功能，应加入公开白名单并在交接文档中写出。
- 参数错误返回 400；学校不存在返回 404；服务异常返回 500，并记录可检索日志。

## 5. 后端实施与验证要求

1. 检查现有表结构、字段含义、年份数据量和学校关联关系；如字段不足，补充迁移脚本和回滚说明。
2. 为学校名搜索增加必要索引，避免 `%name%` 查询在大表上造成明显性能问题。
3. 使用真实数据验证：至少验证一个高中同时有三类数据、一个初中只有名额到校。
4. 验证不存在学校、空名称、非法学段、非法年份、无数据年份和未登录请求。
5. 后端编译、单元测试/接口测试和实际 HTTP 请求均需记录结果。
6. 完成后生成前端交接文档，至少包括最终接口清单、完整请求示例、完整成功/失败响应、字段映射、鉴权、默认年份、空态和联调数据。

## 6. 给后端 Codex 的执行指令

```text
请完整阅读《查分工具学校名额与志愿查询_后端Codex工作文档.md》，完成学校学段+学校名搜索、学校详情和招生数据接口。必须兼容现有 /api/score-query/region、/school、/volunteer 调用约定，并严格区分高中与初中：高中返回名额到区、名额到校、1-15志愿；初中只返回名额到校。完成数据库核对、接口测试和真实数据联调后，在对接文档目录生成《查分工具学校名额与志愿查询_前端对接文档_日期.md》，写入最终可用的接口和响应，不要只修改代码而不生成交接文档。
```
