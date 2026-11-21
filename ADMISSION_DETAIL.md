# 招生动态 — 后端改动 & 前端对接文档

> 日期：2026-07-30

---

## 一、后端改动清单

### 新建文件

| 文件 | 说明 |
|------|------|
| `entity/AdmissionInfo.java` | 招生动态实体，映射 `admission_info` 表，13 字段 |
| `mapper/AdmissionInfoMapper.java` | MyBatis-Plus `BaseMapper` |
| `service/AdmissionInfoService.java` | 接口，`getBySchoolId(schoolId)` |
| `service/impl/AdmissionInfoServiceImpl.java` | 按 `school_id` + `is_active=1` 查单条 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `controller/SchoolController.java` | 新增 `GET /api/schools/{id}/admission`，注入 `AdmissionInfoService` |

---

## 二、接口详情

### `GET /api/schools/{id}/admission`

无需鉴权，公开接口。

**请求：**
```
GET /api/schools/5/admission
```

**成功响应（有数据）：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "schoolId": 5,
    "admissionYear": "2026",
    "admissionTime": "2026年3月-6月",
    "targetGrades": "小学/初中/高中",
    "tuitionRange": "17.2-21万/年",
    "enrollmentCount": 200,
    "curriculum": "IB/A-Level",
    "targetStudents": "符合上海入学条件的适龄学生",
    "admissionRequirements": "笔试（数学、英语）+ 面试",
    "graduationResults": "2025届100%升入世界前50",
    "isActive": 1,
    "createTime": "2026-07-30T10:00:00",
    "updateTime": "2026-07-30T10:00:00"
  }
}
```

**成功响应（无数据）：**
```json
{
  "code": 200,
  "data": null
}
```

---

## 三、数据库表结构

表名：`admission_info`（学校已建好，位于 `lpsx` 库）

| 字段 | 类型 | 说明 | 前端用到 |
|------|------|------|----------|
| `id` | int | 主键 | — |
| `school_id` | int | 关联学校ID | 查询条件 |
| `admission_year` | varchar(20) | 招生年份 | — |
| `admission_time` | varchar(50) | 招生时间 | ✅ 招生进度 |
| `target_grades` | varchar(50) | 开设学段 | ✅ 招生项目 |
| `tuition_range` | varchar(100) | 学费区间 | ✅ 招生项目（高亮） |
| `enrollment_count` | int | 招生人数 | ✅ 招生项目 |
| `curriculum` | varchar(100) | 课程体系 | ✅ 招生项目 |
| `target_students` | varchar(200) | 招生对象 | ✅ 考生要求 |
| `admission_requirements` | text | 录取要求 | ✅ 招生进度 |
| `graduation_results` | text | 升学情况 | — |
| `is_active` | tinyint | 是否显示 | 过滤条件 |
| `create_time` | datetime | 创建时间 | — |
| `update_time` | datetime | 更新时间 | — |

---

## 四、前端调用方式

```javascript
const admission = await request('/api/schools/' + schoolId + '/admission', 'GET', {}, false)
```

返回值可能为 `null`（该学校无招生数据）。前端需做空判断，显示占位文案。

---

## 五、页面路由

```
学校库列表 → 点"招生动态"按钮
  → wx.navigateTo({ url: '/pages/admission-detail/admission-detail?id=' + schoolId })
```

传递参数 `id` = 学校 ID，页面在 `onLoad` 中读取并调用两个接口：
1. `GET /api/schools/{id}` — 获取学校名称、Logo
2. `GET /api/schools/{id}/admission` — 获取招生数据

---

## 六、页面文件

| 文件 | 路径 |
|------|------|
| JS | `pages/admission-detail/admission-detail.js` |
| WXML | `pages/admission-detail/admission-detail.wxml` |
| WXSS | `pages/admission-detail/admission-detail.wxss` |
| JSON | `pages/admission-detail/admission-detail.json` |

已在 `app.json` 中注册。
