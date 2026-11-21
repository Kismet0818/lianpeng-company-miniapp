# 联朋升学祝福墙按学校隔离——前端 Codex 交接文档

> 后端及本地数据库已完成学校隔离。本文件用于前端电脑上的 Codex 合并前端改动。目标是：用户在学校 A 发布的祝福，只能在学校 A 看见，学校 B 绝不能看见。

## 一、接口规则

学校来源：`international` 对应 `school.id`；`score` 对应 `score_query_tool_school.id`；`junior` 对应 `junior_high_school.id`；`general` 对应 `general_high_school.id`。

发布祝福必须携带学校：

```http
POST /api/blessing
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "content": "祝孩子中考顺利，考入理想高中！",
  "schoolSource": "score",
  "schoolId": "123"
}
```

查询某校祝福：

```http
GET /api/blessing/list?page=1&size=20&schoolSource=score&schoolId=123
```

后端会验证学校并保存学校名称。无学校参数的列表只返回历史全局祝福，不会混入具体学校祝福。

## 二、需要修改的文件

- `pages/score-school-detail/score-school-detail.js`
- `pages/blessing-compose/blessing-compose.js`
- `pages/blessing-compose/blessing-compose.wxml`
- `pages/blessing-list/blessing-list.js`

若其他学校详情页也展示祝福，则按真实数据表使用对应 `schoolSource`，不得跨表混用 ID。

## 三、学校详情页

分数查询学校详情加载前三条祝福：

```js
const schoolId = Number(this.data.schoolId)
const data = await request(
  '/api/blessing/list?page=1&size=3&schoolSource=score&schoolId=' + schoolId
)
```

打开发布页：

```js
const schoolId = Number(this.data.schoolId)
const schoolName = this.data.school && this.data.school.name ? this.data.school.name : ''
wx.navigateTo({
  url: '/pages/blessing-compose/blessing-compose?schoolSource=score&schoolId=' + schoolId
    + '&schoolName=' + encodeURIComponent(schoolName)
})
```

打开全部祝福页：

```js
wx.navigateTo({
  url: '/pages/blessing-list/blessing-list?schoolSource=score&schoolId=' + schoolId
    + '&schoolName=' + encodeURIComponent(schoolName)
})
```

## 四、发布页

在 `onLoad(options)` 保存范围：

```js
onLoad(options) {
  const schoolSource = String(options.schoolSource || '')
  const schoolId = Number(options.schoolId)
  const schoolName = decodeURIComponent(options.schoolName || '')
  this.setData({ schoolSource, schoolId, schoolName })
}
```

提交前验证并上报：

```js
if (!this.data.schoolSource || !this.data.schoolId) {
  wx.showToast({ title: '学校信息缺失，请返回重试', icon: 'none' })
  return
}

await request('/api/blessing', 'POST', {
  content,
  schoolSource: this.data.schoolSource,
  schoolId: String(this.data.schoolId)
}, true)
```

WXML 显示所属学校：

```xml
<view wx:if="{{schoolName}}" class="compose-card">
  <text class="card-title">祝福所属学校</text>
  <text class="field-help">{{schoolName}}</text>
</view>
```

## 五、全部祝福页

`onLoad(options)` 保存 `schoolSource`、`schoolId`、`schoolName`。分页、下拉刷新、触底加载的每一次请求都必须带相同的 `schoolSource` 和 `schoolId`，不能翻页后丢失范围。

## 六、A/B 隔离验收

1. 学校 A 发布“学校A隔离测试”。
2. 学校 A 的详情预览和全部祝福均能看到。
3. 学校 B 的详情预览和全部祝福均不能看到。
4. 学校 B 发布“学校B隔离测试”，再回学校 A，不能看到 B 的内容。
5. Network 中所有 list 请求均须有 `schoolSource`、`schoolId`；POST 请求体也须有这两个字段。
6. 管理后台祝福管理和用户祝福明细应显示所属学校。

## 七、禁止事项

- 不得使用无学校参数的列表渲染学校详情。
- 不得只把学校名拼进 content 代替结构化 ID。
- 不得把历史全局祝福猜测分配给学校。
- 前端电脑不得操作本地或云数据库。
- 不得覆盖页面原有功能，应在真实源码中合并。

## 八、直接发给前端 Codex

```text
请完整阅读 C:\claude\MMMMMMMMMMMMMMMMMMMMD\祝福墙按学校隔离_前端Codex交接文档.md，在当前电脑实际用于构建测试版的联朋升学小程序源码中完成祝福墙按学校隔离。至少修改 score-school-detail、blessing-compose 和 blessing-list，保留已有功能。完成后编译并做学校 A/B 双向隔离测试，报告 Network 中 list 请求参数和 POST 请求体。只改前端，不操作数据库或云数据库。
```
