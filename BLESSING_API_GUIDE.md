# 中考祝福墙模块 — 接口文档

> 模块：中考祝福墙（用户发布祝福 + 展示祝福，纯展示型，无点赞无评论）
> 版本：2026-08-27
> 鉴权：列表**公开**，发布**需登录**（JWT），管理**需 X-Admin-Key**

---

## 一、接口总览

| # | 方法 | 路径 | 用途 | 鉴权 |
|---|------|------|------|------|
| 1 | POST | `/api/blessing` | 发布某所学校的祝福 | 🔒 需登录 |
| 2 | GET | `/api/blessing/list` | 按学校查询祝福（分页） | 🌐 公开 |
| 3 | GET | `/api/admin/blessing` | 管理端全部祝福 | 🔑 X-Admin-Key |
| 4 | DELETE | `/api/admin/blessing/{id}` | 删除祝福 | 🔑 X-Admin-Key |

**基础地址**
- 本地：`http://192.168.3.5:8080`
- 云托管（线上）：`https://lpsx1-295231-8-1463346517.sh.run.tcloudbase.com`

> ⚠️ **数据说明**：`blessing` 表目前只建在**本地库**，线上云托管库无此表。上线祝福墙前需先在线上库建表。

---

## 二、统一返回结构

```json
{
  "code": 200,      // 200=成功，400=业务异常，401=未登录/未授权
  "msg": "成功",
  "data": { ... }   // 具体数据
}
```

**鉴权头**
| 头 | 值 | 说明 |
|----|----|------|
| `Authorization` | `Bearer <token>` | 发布祝福用（JWT） |
| `X-Admin-Key` | `LP@870263#lpsx` | 管理接口用 |

---

## 三、接口详情

### 1️⃣ 发布祝福 `POST /api/blessing`（需登录）

> 需 JWT 登录；昵称/头像自动取当前微信用户（`user` 表），无需前端传。

**请求体**
```json
{
  "content": "张三 祝福 李四 考上 复旦",
  "schoolSource": "score",
  "schoolId": "123"
}
```

**参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | string | ✅ | 祝福内容，≤200 字，前后空格自动去除 |
| `schoolSource` | string | ✅ | `international` / `score` / `junior` / `general` |
| `schoolId` | string/number | ✅ | 对应来源表的学校主键 |

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": 2,
    "openid": "oX7...",
    "nickname": "初三家长·张妈妈",
    "avatar": "https://thirdwx.qlogo.cn/...",
    "content": "张三 祝福 李四 考上 复旦",
    "status": 1,
    "createTime": "2026-08-26 16:24:59"
  }
}
```

**错误示例**
```json
{ "code": 400, "msg": "祝福内容不能为空", "data": null }
{ "code": 401, "msg": "未授权，请先登录", "data": null }
```

---

### 2️⃣ 祝福墙列表 `GET /api/blessing/list`（公开）

> 分页返回**已发布**（status=1）的祝福，按发布时间倒序（最新在前）。无需登录。

**参数**
| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `page` | int | 否 | 1 | 页码（≥1） |
| `size` | int | 否 | 20 | 每页条数（1~50） |
| `schoolSource` | string | 否* | - | 与 `schoolId` 同时传入，查询具体学校 |
| `schoolId` | int | 否* | - | 与 `schoolSource` 同时传入；都不传时只返回历史全局祝福 |

**调用示例**
```
/api/blessing/list?page=1&size=20&schoolSource=score&schoolId=123
```

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "records": [
      {
        "id": 2,
        "openid": "oX7...",
        "nickname": "初三家长·张妈妈",
        "avatar": "https://thirdwx.qlogo.cn/...",
        "content": "祝我家宝贝 小宇 中考顺利，考进理想的 上海中学，愿你金榜题名！",
        "status": 1,
        "createTime": "2026-08-26 16:24:59"
      }
    ],
    "total": 1,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

**分页字段说明**
| 字段 | 说明 |
|------|------|
| `records` | 当前页祝福列表 |
| `total` | 总条数 |
| `size` | 每页条数 |
| `current` | 当前页码 |
| `pages` | 总页数 |

**Blessing 字段说明**
| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | number | 主键 |
| `openid` | string | 发布者微信 openid |
| `nickname` | string | 发布者昵称（未设置时为"匿名用户"） |
| `avatar` | string | 发布者头像 URL（可为空） |
| `content` | string | 祝福内容 |
| `status` | number | 1=已发布 0=待审 2=隐藏 |
| `createTime` | string | 发布时间 `yyyy-MM-dd HH:mm:ss` |

---

### 3️⃣ 管理端全部祝福 `GET /api/admin/blessing`（需 X-Admin-Key）

> 返回**全部**祝福（含待审/隐藏），按时间倒序。用于管理端审核/展示。

**参数**：无

**返回示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    { "id": 2, "openid": "oX7...", "nickname": "初三家长·张妈妈", "avatar": "", "content": "...", "status": 1, "createTime": "2026-08-26 16:24:59" }
  ]
}
```

---

### 4️⃣ 删除祝福 `DELETE /api/admin/blessing/{id}`（需 X-Admin-Key）

> 删除指定祝福（物理删除）。用于清理违规内容。

**调用示例**
```
DELETE /api/admin/blessing/2
```

**返回示例**
```json
{ "code": 200, "msg": "成功", "data": null }
```

**错误示例**
```json
{ "code": 400, "msg": "记录不存在", "data": null }
```

---

## 四、数据库表结构（本地库 `lpsx`）

```sql
CREATE TABLE `blessing` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `openid`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '发布者openid',
  `nickname`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '发布者昵称',
  `avatar`      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '发布者头像',
  `school_source` VARCHAR(20) DEFAULT NULL COMMENT '学校来源',
  `school_id`   INT          DEFAULT NULL COMMENT '对应来源表的学校ID',
  `school_name` VARCHAR(150) DEFAULT NULL COMMENT '发布时学校名称快照',
  `content`     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '祝福内容',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1-已发布 0-待审 2-隐藏',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_blessing_school_time` (`school_source`,`school_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中考祝福墙';
```

> 该表为**新增表**，实现时未改动任何其他表数据。

---

## 五、附注

- **前端接入建议**：列表页请求 `GET /api/blessing/list`（滚动分页），发布框在登录态下 `POST /api/blessing`。
- **内容审核（待接入）**：目前发布**未接**微信 `security.msgSecCheck` 机审，如需接入在后端 `POST /api/blessing` 前增加一步校验即可。
- **状态字段**：当前发布即 `status=1`（直接可见）；后续如需"发布后待人工审核"，可改默认 `status=0` 并在管理端审核。
- **代码位置**：`controller/BlessingController.java`（公开）、`controller/BlessingAdminController.java`（管理）、`entity/Blessing.java`、`mapper/BlessingMapper.java`、`common/config/MybatisPlusConfig.java`（分页插件）。
