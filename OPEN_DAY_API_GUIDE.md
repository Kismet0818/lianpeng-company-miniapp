# 学校开放日模块 — 前后端对接文档

> 涉及页面：`pages/open-day/`（列表 + 详情 + 预约弹窗）
> 后端：已完成，需重启后生效
> 日期：2026-08-07

---

## 一、功能概述

- **开放日列表**：展示外籍人员子女学校（16 所）的开放日信息卡片
- **开放日详情**：学校介绍、学段、课程、地址
- **预约**：详情页底部"预约"按钮 → 弹出留资表单（姓名 + 电话 + 选择日期）→ 提交

---

## 二、接口速查

| 方法 | 路径 | 鉴权 | 用途 |
|------|------|------|------|
| GET | `/api/open-day/list` | 无 | 开放日列表（含 Logo） |
| GET | `/api/open-day/{id}` | 无 | 开放日详情 |
| POST | `/api/lead/submit` | **JWT** | 提交预约（复用留资接口） |

---

## 三、接口详情

### 1. GET `/api/open-day/list`

返回 `Result<List<OpenDayVO>>`，`data` 为数组：

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "schoolId": 192,
      "schoolName": "上海美国外籍人员子女学校",
      "educationalStages": "幼儿园小班至12年级",
      "curriculum": "美国核心课程、AP课程、IB课程",
      "address": "浦东校区：上海市浦东三甲港凌白路1600号；浦西校区：上海市闵行区金丰路258号",
      "openTime": null,
      "description": "上海美国外籍人员子女学校（SAS）成立于1912年，...",
      "logoUrl": "https://lianpeng.oss-cn-shanghai.aliyuncs.com/logo/c5e07bce2d594f4a86219f49fdfe3bfa.jpg"
    }
  ]
}
```

> ⚠️ **`openTime` 当前为 null** — 开放日时间尚未录入，前端需隐藏或显示"时间待定"。

### 2. GET `/api/open-day/{id}`

返回单个 `OpenDayVO` 对象，结构同上。

### 3. POST `/api/lead/submit`（预约提交）

**需 JWT**（`Authorization: Bearer token`）。

Body：

```json
{
  "parent_name": "张三",
  "parent_phone": "13800138000",
  "child_grade": "小学三年级",
  "target_school": "上海美国外籍人员子女学校",
  "open_day_id": 1,
  "reserve_time": "2026-08-20"
}
```

| 参数 | 必填 | 说明 |
|------|------|------|
| parent_name | ✅ | 家长姓名 |
| parent_phone | ✅ | 联系电话 |
| child_grade | 选填 | 孩子年级 |
| target_school | 选填 | 目标院校（前端自动带出开放日学校名） |
| concern | 选填 | 疑问 |
| open_day_id | 选填 | 开放日 ID（预约时必传） |
| reserve_time | 选填 | 预约日期，格式 `YYYY-MM-DD` |

> 注意：后端有 **24 小时防重**，同手机号 24h 内重复提交会返回错误提示。

---

## 四、前端页面结构建议

### 1. 开放日列表页 `pages/open-day/index`

```
┌─────────────────────────────┐
│ 顶部标题栏「学校开放日」      │
├─────────────────────────────┤
│ ┌─────────┐                 │
│ │  校徽   │ 学校名称         │
│ │         │ 学段 · 课程      │
│ │         │ 地址             │
│ └─────────┘  [查看详情]      │
│ ┌─────────┐                 │
│ │  校徽   │ 下一所学校       │
│ └─────────┘                 │
│ ...（16 所卡片）             │
└─────────────────────────────┘
```

点击卡片 → `wx.navigateTo` 到详情页，传 `id`。

### 2. 开放日详情页 `pages/open-day/detail`

- 顶部校徽 + 学校名称
- 信息区：学段 / 课程 / 地址
- 学校介绍（`description`，富文本）
- 底部固定「预约开放日」按钮

### 3. 预约弹窗（底部半屏 popup）

```
┌─────────────────────────────┐
│ 预约开放日                  │
│ 学校：上海美国外籍人员子女学校 │
│ ─────────────────────────── │
│ 家长姓名  [ 输入框 ]         │
│ 联系电话  [ 输入框 ]         │
│ 预约日期  [ 2026-08-20 ▾ ]  │ ← picker mode="date"
│ ─────────────────────────── │
│        [ 确认预约 ]          │
└─────────────────────────────┘
```

**日期选择**：微信原生 `picker mode="date"`

```xml
<picker mode="date" value="{{reserveTime}}" bindchange="onDateChange" start="{{today}}">
  <view class="picker-value">{{reserveTime || '请选择日期'}}</view>
</picker>
```

```js
onDateChange(e) {
  this.setData({ reserveTime: e.detail.value })  // 形如 "2026-08-20"
}
```

`start` 设为今天，禁止选过去日期。

### 4. 提交逻辑

```js
async onSubmit() {
  if (!this.data.parentName || !this.data.parentPhone || !this.data.reserveTime) {
    wx.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  try {
    await request('/api/lead/submit', 'POST', {
      parent_name: this.data.parentName,
      parent_phone: this.data.parentPhone,
      child_grade: this.data.childGrade,
      target_school: this.data.schoolName,
      open_day_id: this.data.openDayId,
      reserve_time: this.data.reserveTime
    }, true)  // 需要登录
    wx.showToast({ title: '预约成功，老师会尽快联系您', icon: 'success' })
    // 关闭弹窗
  } catch (e) {}
}
```

---

## 五、OpenDayVO 字段参考

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 开放日 ID |
| schoolId | Integer | 关联学校 ID |
| schoolName | String | 学校名称 |
| educationalStages | String | 学段 |
| curriculum | String | 开设课程 |
| address | String | 学校地址 |
| openTime | String/Date | 开放日时间（**当前为 null**） |
| description | String | 学校介绍 |
| logoUrl | String | 校徽 OSS 地址 |

---

## 六、注意事项

| 事项 | 说明 |
|------|------|
| 登录要求 | 预约提交需 JWT，未登录先走 `wx.login` 流程 |
| 时间待定 | `openTime` 暂无数据，页面做"时间待定"占位 |
| 防重提示 | 24h 内重复提交后端会返回错误，前端 toast 提示即可 |
| 卡片跳转 | 列表 → 详情传 `id` 参数 |
