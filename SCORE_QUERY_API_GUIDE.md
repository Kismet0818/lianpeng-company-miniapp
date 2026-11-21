# 查分工具模块 — 接口文档

> 模块：留学辅导 → 查分工具
> 版本：2026-08-25（新增学校风光图接口 + logo 字段 + 管理端批量上传工具）
> 鉴权：**公开接口，无需登录**（已加入拦截器白名单 `/api/score-query/**`）

---

## 一、接口总览

| # | 方法 | 路径 | 用途 |
|---|------|------|------|
| 1 | GET | `/api/score-query/meta` | 筛选条件（学年 / 目标区 / 目标初中）|
| 2 | GET | `/api/score-query/region` | 名额到区查询 |
| 3 | GET | `/api/score-query/school` | 名额到校查询 |
| 4 | GET | `/api/score-query/schools/{id}` | 高中学校详情 |
| 5 | GET | `/api/score-query/volunteer/meta` | 1-15志愿筛选条件（学年/目标区/学校级别）|
| 6 | GET | `/api/score-query/volunteer` | 1-15志愿查询 |
| 7 | GET | `/api/score-query/schools/{id}/images` | 高中学校风光图列表 |
| 8 | GET | `/api/score-query/schools/district-key` | 按区查询区重点学校 |

**基础地址**
- 本地：`http://192.168.3.5:8080`
- 云托管（线上）：`https://lpsx1-295231-8-1463346517.sh.run.tcloudbase.com`

---

## 二、⚠️ 核心字段说明（target_type 语义）

```
target_type = 2  →  名额到区（记录带 target_district，target_school_name 为空）
target_type = 1  →  名额到校（记录带 target_school_name，target_district 为空）
```

> 注意：**2=到区，1=到校**（与常见直觉相反，前端按此理解即可，接口已按真实规则实现）。

---

## 三、统一返回结构

```json
{
  "code": 200,      // 200=成功，400=业务异常，401=未登录
  "msg": "成功",
  "data": { ... }   // 具体数据
}
```

---

## 四、接口详情

### 1️⃣ 筛选条件 `GET /api/score-query/meta`

页面加载时调用一次，拿下拉选项。

**参数**：无

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "years": ["2025-2026", "2024-2025"],
    "districts": ["徐汇区", "黄浦区", "长宁区", "静安区", "普陀区", "虹口区", "杨浦区", "闵行区", "宝山区", "嘉定区", "浦东新区", "金山区", "松江区", "青浦区", "奉贤区", "崇明区"],
    "juniorSchools": ["上海市XX初级中学", "XX中学初中部", "..."]
  }
}
```

**字段说明**
| 字段 | 说明 |
|------|------|
| `years` | 可选学年（倒序，最新在前）|
| `districts` | 可选目标区（用于"名额到区"）|
| `juniorSchools` | 可选目标初中（用于"名额到校"）|

---

### 2️⃣ 名额到区 `GET /api/score-query/region`

> 查询 `target_type = 2` 的记录。

**参数**（均可选）

| 参数 | 类型 | 说明 |
|------|------|------|
| `district` | string | 目标区，如 `徐汇区` |
| `year` | string | 学年，如 `2025-2026` |

**调用示例**
```
/api/score-query/region?district=徐汇区&year=2025-2026
```

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 5,
      "highSchoolId": 10,
      "highSchoolName": "上海市上海中学",
      "highSchoolDistrict": "徐汇区",
      "targetType": 2,
      "targetDistrict": "徐汇区",
      "targetSchoolName": null,
      "quotaCount": 8,
      "minScore": 712,
      "changeRange": 7.0,
      "academicYear": "2025-2026"
    }
  ]
}
```

**字段说明**
| 字段 | 说明 |
|------|------|
| `highSchoolName` | 高中学校名称 |
| `highSchoolDistrict` | 高中所在区 |
| `quotaCount` | 名额数量（到区通常较多，如 8~20 名）|
| `minScore` | 最低录取分数线 |
| `changeRange` | 涨幅（正数上涨，负数下降，单位分）|
| `academicYear` | 学年 |

> 默认按 `minScore` **降序**（分数从高到低）。到区数据应为"同一高中一条记录、名额较多"。

---

### 3️⃣ 名额到校 `GET /api/score-query/school`

> 查询 `target_type = 1` 的记录。

**参数**（均可选）

| 参数 | 类型 | 说明 |
|------|------|------|
| `school` | string | 目标初中名称（**模糊匹配**，如 `建平`）|
| `year` | string | 学年，如 `2025-2026` |

**调用示例**
```
/api/score-query/school?school=建平&year=2025-2026
```

**返回示例**（结构同"名额到区"）
```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 100,
      "highSchoolId": 10,
      "highSchoolName": "上海市上海中学",
      "targetType": 1,
      "targetDistrict": null,
      "targetSchoolName": "上海市建平实验中学",
      "quotaCount": 1,
      "minScore": 705,
      "changeRange": 2.5,
      "academicYear": "2025-2026"
    }
  ]
}
```

> `school` 支持模糊匹配，传入部分校名即可；`targetSchoolName` 返回完整初中名。到校数据通常"每所初中 1 名"，属正常。

---

### 4️⃣ 高中学校详情 `GET /api/score-query/schools/{id}`

**参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | int | 高中学校 ID（来自查询结果 `highSchoolId`）|

**调用示例**
```
/api/score-query/schools/10
```

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": 10,
    "name": "上海市上海中学",
    "logo": "https://lianpeng.oss-cn-shanghai.aliyuncs.com/score-query/school-logo/10.jpg",
    "district": "徐汇区",
    "tuitionMin": 0.30,
    "tuitionMax": 0.30,
    "boardingInfo": "全部寄宿",
    "schoolType": "公办高中",
    "address": "上海市徐汇区上中路400号",
    "description": "上海市上海中学创始于1865年...",
    "createdTime": "2026-08-18 11:25:43",
    "updatedTime": "2026-08-18 11:25:43"
  }
}
```

**字段说明**
| 字段 | 说明 |
|------|------|
| `name` | 学校名称 |
| `logo` | 学校校徽 Logo URL（OSS，可空）|
| `district` | 所属区 |
| `tuitionMin` / `tuitionMax` | 最低/最高学费（万元）|
| `boardingInfo` | 寄宿情况 |
| `schoolType` | 学校性质（公办/民办等）|
| `address` | 地址 |
| `description` | 学校介绍（长文本，可换行 `\n`）|

---

### 5️⃣ 1-15志愿筛选条件 `GET /api/score-query/volunteer/meta`

页面加载时调用，拿下拉选项。

**参数**：无

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "years": ["2025-2026"],
    "districts": ["徐汇区", "黄浦区", "..."],
    "schoolTypes": ["市实验性示范性高中", "区实验性示范性高中", "市特色普通高中", "一般高中"]
  }
}
```

**字段说明**
| 字段 | 说明 |
|------|------|
| `years` | 可选学年 |
| `districts` | 可选招生目标区（考生所在区）|
| `schoolTypes` | 可选学校级别 |

---

### 6️⃣ 1-15志愿查询 `GET /api/score-query/volunteer`

**参数**（均可选）

| 参数 | 类型 | 说明 |
|------|------|------|
| `district` | string | 招生目标区（考生所在区，如 `徐汇区`）|
| `year` | string | 学年，如 `2025-2026` |
| `type` | string | 学校级别，如 `市实验性示范性高中` |

**调用示例**
```
/api/score-query/volunteer?district=徐汇区&year=2025-2026&type=市实验性示范性高中
```

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 90,
      "schoolName": "上海中学",
      "highSchoolDistrict": "徐汇区",
      "schoolType": "市实验性示范性高中",
      "targetDistrict": "徐汇区",
      "planCount": 7,
      "minScore": 716.0,
      "changeRange": 9.0,
      "academicYear": "2025-2026"
    }
  ]
}
```

**字段说明**
| 字段 | 说明 |
|------|------|
| `schoolName` | 高中学校名称 |
| `highSchoolDistrict` | 高中所在区 |
| `schoolType` | 学校级别 |
| `planCount` | 计划招生数 |
| `minScore` | 最低录取分数线（可能带 .5，如 716.0）|
| `changeRange` | 涨幅（正数上涨，负数下降）|
| `academicYear` | 学年 |

> 默认按 `minScore` **降序**（分数从高到低）。

---

### 7️⃣ 高中学校风光图列表 `GET /api/score-query/schools/{id}/images`

> 返回某所高中的学校风光图，用于学校详情页轮播展示。

**参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | int | 高中学校 ID |

**调用示例**
```
/api/score-query/schools/10/images
```

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 28,
      "schoolId": 10,
      "imageUrl": "https://lianpeng.oss-cn-shanghai.aliyuncs.com/score-query/school-gallery/10/01.png",
      "sortOrder": 1,
      "createdTime": "2026-08-25 13:43:15"
    },
    {
      "id": 29,
      "schoolId": 10,
      "imageUrl": "https://lianpeng.oss-cn-shanghai.aliyuncs.com/score-query/school-gallery/10/02.png",
      "sortOrder": 2,
      "createdTime": "2026-08-25 13:43:18"
    }
  ]
}
```

**字段说明**
| 字段 | 说明 |
|------|------|
| `schoolId` | 关联学校 ID |
| `imageUrl` | 学校风光图 URL（OSS）|
| `sortOrder` | 排序号（从 1 开始，升序展示）|
| `createdTime` | 创建时间 |

> 无数据时返回空数组 `[]`；图片 URL 前端可经 `resolveImageUrl` 追加 `?x-oss-process=...` 压缩参数。

---

### 8️⃣ 按区查询区重点学校 `GET /api/score-query/schools/district-key`

> 用于前端选择某个区后，点击“区重点”展示该区对应的学校。

**参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `district` | string | 是 | 学校所属区，如 `杨浦区` |

**调用示例**
```
/api/score-query/schools/district-key?district=杨浦区
```

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 76,
      "name": "上海体育大学附属中学",
      "shortName": "上体附中",
      "logo": "https://lianpeng.oss-cn-shanghai.aliyuncs.com/score-query/school-logo/76.png",
      "district": "杨浦区"
    }
  ]
}
```

**筛选口径**

- `score_query_tool_school.id >= 75`；
- `short_name` 非空；
- `district` 与请求参数精确匹配；
- 按学校 ID 升序返回。

> `school_type` 字段表示公办/民办，不用于判断“区重点”。点击列表中的学校后，继续使用 `/api/score-query/schools/{id}` 查询详情。

---

## 五、前端调用建议

```
① 页面加载 → GET /api/score-query/meta 和 /api/score-query/volunteer/meta → 渲染各筛选下拉
② 用户选标签
   名额到区 → 选 学年+目标区 → GET /region?district=X&year=Y
   名额到校 → 选 学年+目标初中 → GET /school?school=X&year=Y
   1-15志愿 → 选 目标区+学年+学校级别 → GET /volunteer?district=X&year=Y&type=Z
③ 点某高中 → GET /schools/{id}              → 展示学校详情（跳详情页）
④ 详情页   → GET /schools/{id}/images       → 校徽用返回的 logo，下方轮播学校风光图
```

**注意事项**
- 返回字段为**驼峰命名**（如 `highSchoolName`、`minScore`）
- `targetType`：**2=到区，1=到校**
- `changeRange` 可能是负数（分数下降），前端展示时可加色区分（涨红跌绿/箭头）
- `minScore` 为空时（`null`），前端显示"—"即可
- 学费单位是**万元**（`0.30` = 3000 元）
- 若 `year` 不传，返回全部学年数据；建议前端**始终带上所选学年**

---

## 六、数据库表（供参考）

**`score_query_tool`**（名额分配）
- `target_type`：**2=名额到区**，**1=名额到校**
- `target_district`：目标区（到区用）
- `target_school_name`：目标初中（到校用）
- `academic_year`：学年

**`score_query_tool_school`**（学校信息）
- 学校名称/区/学费/寄宿/性质/地址/介绍
- `logo`：学校校徽 Logo URL（OSS）

**`score_query_tool_school_image`**（学校风光图）
- `school_id`：关联 score_query_tool_school.id
- `image_url`：风光图 URL（OSS）
- `sort_order`：排序号（从 1 开始）
- `created_time`：创建时间

**`score_volunteer_1_15`**（1-15志愿 / 统一招生）
- `school_name`：高中学校名称
- `school_type`：学校级别（市/区实验性示范性高中、市特色普通高中、一般高中）
- `target_district`：招生目标区（考生所在区）
- `plan_count`：计划招生数
- `min_score`：最低录取分数线（decimal，可带 .5）
- `change_range`：涨幅
- `academic_year`：学年

---

## 七、管理端批量上传接口（内部工具，一次性）

> 鉴权：**需携带请求头 `X-Admin-Key: LP@870263#lpsx`**（管理密钥）
> 说明：用于把 74 所学校的校徽 + 风光图批量上传 OSS 并写库，属于运维一次性工具，非小程序业务接口。

### `GET /api/admin/score-image/upload`

**参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| `mode` | string | `all`=校徽+风光图（默认），`logo`=仅校徽，`gallery`=仅风光图 |

**调用示例**
```
GET /api/admin/score-image/upload?mode=all
Header: X-Admin-Key: LP@870263#lpsx
```

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "logo": {
      "total": 74,
      "success": 74,
      "failed": 0,
      "unmatched": []
    },
    "gallery": {
      "schoolTotal": 74,
      "schoolDone": 74,
      "imageInserted": 274,
      "failed": 0,
      "unmatched": []
    }
  }
}
```

**字段说明**
| 字段 | 说明 |
|------|------|
| `logo.total/success/failed` | 校徽总数 / 成功 / 失败 |
| `logo.unmatched` | 文件名未匹配到学校的清单（应为空）|
| `gallery.schoolTotal/schoolDone` | 风光图学校总数 / 已处理 |
| `gallery.imageInserted` | 插入 score_query_tool_school_image 的记录数 |
| `gallery.unmatched` | 未匹配到学校的文件夹清单（应为空）|

**上传结果落点**
- 校徽 → OSS `score-query/school-logo/{schoolId}.jpg` → 写 `score_query_tool_school.logo`
- 风光图 → OSS `score-query/school-gallery/{schoolId}/{序号}.png` → 写 `score_query_tool_school_image`

> ⚠️ 接口**非幂等**：重复调用会重复插入 `score_query_tool_school_image`（无唯一约束），需要时先清表或去重。
> 当前仅在**本地库**验证通过（74 校徽 + 274 风光图），**线上库未执行**。
