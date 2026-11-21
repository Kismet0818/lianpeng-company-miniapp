# 用户学校浏览行为追踪——前端 Codex 交接文档

> 适用项目：联朋升学微信小程序  
> 目的：用户进入具体学校详情页后计时，离开时将有效浏览行为上报给后端，供管理后台显示“行为记录”和“兴趣学校”。  
> 本文给前端电脑上的 Codex 使用。请先检查实际源码再合并，不要覆盖前端同事已有改动。

---

## 一、任务边界

本次只修改微信小程序前端源码，不修改后端、不操作 MySQL、不操作云数据库。

后端接口已经完成并在联调电脑上运行：

- `POST /api/user/activity`
- 必须携带 `Authorization: Bearer <token>`
- 当前允许的学校来源：`international`、`score`、`junior`、`general`
- 单次停留不足 3 秒，后端不会记录
- 单次停留最长按 7200 秒处理

管理后台接口已经完成：

- `GET /api/admin/users/{id}/activities`
- 返回最近行为时间线及按累计停留时长汇总的兴趣学校

前端没有计时和上报代码时，即使登录、手机号绑定和学校详情接口都正常，数据库 `user_activity` 仍会一直是 0。

---

## 二、开始修改前必须确认

1. 找到前端电脑正在使用、能够构建测试版的真实小程序源码目录，不要误改另一份副本。
2. 检查根目录是否存在 `app.js`、`utils/request.js` 和 `pages/school-detail/`。
3. 检查 `utils/request.js` 导出的请求函数名称和参数顺序。本文当前项目约定为：

   ```js
   request(url, method = 'GET', data = {}, needAuth = false)
   ```

4. 检查登录成功后 token 是否保存为：

   ```js
   wx.setStorageSync('token', token)
   ```

5. 联调时确认 `app.js` 使用本地后端：

   ```js
   useCloud: false,
   localBaseUrl: 'http://192.168.3.5:8080'
   ```

   IP 必须以现场后端电脑实际地址为准。如果学校详情和手机号绑定已经能访问该后端，不要擅自切换环境。

---

## 三、新增统一追踪工具

新增文件：`utils/activity.js`

使用以下实现。不要把错误写成空的 `.catch(() => {})`，否则上报失败时无法排查；也不要在请求成功前把记录标记为已发送。

```js
const { request } = require('./request')

function startSchoolView(page, schoolSource, schoolId) {
  const id = Number(schoolId)
  if (!page || !schoolSource || !Number.isInteger(id) || id <= 0) {
    console.warn('[activity] 无效的学校浏览参数', { schoolSource, schoolId })
    return
  }

  page._schoolView = {
    schoolSource,
    schoolId: id,
    startedAt: Date.now(),
    sending: false,
    sent: false
  }

  console.info('[activity] 开始学校详情计时', { schoolSource, schoolId: id })
}

function finishSchoolView(page) {
  const view = page && page._schoolView
  if (!view || view.sent || view.sending || !view.schoolId) return Promise.resolve(false)

  const durationSeconds = Math.floor((Date.now() - view.startedAt) / 1000)
  if (durationSeconds < 3) {
    console.info('[activity] 停留不足 3 秒，不记录', {
      schoolSource: view.schoolSource,
      schoolId: view.schoolId,
      durationSeconds
    })
    return Promise.resolve(false)
  }

  const token = wx.getStorageSync('token')
  if (!token) {
    console.warn('[activity] 本地没有 token，无法上报学校浏览行为')
    return Promise.resolve(false)
  }

  view.sending = true
  return request('/api/user/activity', 'POST', {
    schoolSource: view.schoolSource,
    schoolId: view.schoolId,
    durationSeconds
  }, true).then(() => {
    view.sent = true
    view.sending = false
    console.info('[activity] 学校浏览行为上报成功', {
      schoolSource: view.schoolSource,
      schoolId: view.schoolId,
      durationSeconds
    })
    return true
  }).catch(error => {
    view.sending = false
    console.error('[activity] 学校浏览行为上报失败', {
      schoolSource: view.schoolSource,
      schoolId: view.schoolId,
      durationSeconds,
      error
    })
    return false
  })
}

module.exports = { startSchoolView, finishSchoolView }
```

这段代码的关键点：

- 进入详情页只开始计时，不立刻写数据库。
- 页面离开时才上报。
- 少于 3 秒不记录，也不提前标记为已发送。
- 请求成功后才设置 `sent=true`。
- 请求失败会在控制台明确打印，不再静默吞掉错误。
- `sending` 防止 `onHide` 和 `onUnload` 同时产生重复请求。

---

## 四、接入国际学校详情页

目标文件：`pages/school-detail/school-detail.js`

文件顶部，在现有 `request` 引入后加入：

```js
const { startSchoolView, finishSchoolView } = require('../../utils/activity')
```

在页面现有的 `onLoad(options)` 开头接入：

```js
onLoad(options) {
  startSchoolView(this, 'international', options.id)

  // 保留本页原有的加载逻辑，不要删除或覆盖
}
```

给页面增加生命周期函数：

```js
onHide() {
  finishSchoolView(this)
},

onUnload() {
  finishSchoolView(this)
},
```

如果该页面原来已经存在 `onHide` 或 `onUnload`，必须把调用合并进原函数，不能再声明一个同名函数覆盖旧逻辑。例如：

```js
onUnload() {
  finishSchoolView(this)
  this.stopExistingTimer()
}
```

学校列表跳转应携带 `id`，例如：

```js
wx.navigateTo({
  url: '/pages/school-detail/school-detail?id=' + e.currentTarget.dataset.id
})
```

若旧版前端实际使用的参数名不是 `id`，要按真实参数调整，确保传给 `startSchoolView` 的是数据库 `school.id`，不能传数组序号、招生代码或名称。

---

## 五、接入分数查询学校详情页

如果前端源码存在 `pages/score-school-detail/score-school-detail.js`，按同样方式接入。

顶部加入：

```js
const { startSchoolView, finishSchoolView } = require('../../utils/activity')
```

在 `onLoad(options)` 开头加入：

```js
startSchoolView(this, 'score', options.id)
```

增加或合并：

```js
onHide() {
  finishSchoolView(this)
},

onUnload() {
  finishSchoolView(this)
},
```

这里的 ID 必须对应后端 `score_query_tool_school.id`。

---

## 六、初中和一般高中详情页

后端已经支持以下来源，但只有前端实际存在对应详情页时才接入：

| 页面类型 | `schoolSource` | ID 对应表 |
|---|---|---|
| 国际学校 | `international` | `school.id` |
| 分数查询/区重点学校 | `score` | `score_query_tool_school.id` |
| 初中 | `junior` | `junior_high_school.id` |
| 一般高中 | `general` | `general_high_school.id` |

如果旧版前端当前只有国际学校详情页，本次先保证 `international` 测试通过，不要为了凑齐四种来源虚构不存在的页面。

后续新增初中或一般高中详情页时，在其 `onLoad` 和离开生命周期中复用同一个 `utils/activity.js`，只替换 `schoolSource`。

---

## 七、必须完成的联调验收

### 1. 清理旧缓存并登录

1. 微信开发者工具执行“清缓存 → 全部清除”。
2. 重新编译并进入小程序。
3. 完成登录/手机号授权。
4. 在 Storage 中确认存在非空的 `token`。

### 2. 单条行为测试

1. 从学校库进入任意一所国际学校详情。
2. 控制台应出现：`[activity] 开始学校详情计时`。
3. 停留 5～10 秒。
4. 点击左上角返回学校列表。
5. Network 中必须出现：

   ```text
   POST /api/user/activity
   ```

6. 请求头必须包含：

   ```text
   Authorization: Bearer ...
   ```

7. 请求体示例：

   ```json
   {
     "schoolSource": "international",
     "schoolId": 17,
     "durationSeconds": 8
   }
   ```

8. HTTP 应为 200，响应体业务 `code` 应为 200。
9. 控制台应出现：`[activity] 学校浏览行为上报成功`。
10. 管理后台刷新用户列表，找到测试用户，点击“查看行为”，应出现该学校。

### 3. A/B/C 完整规则测试

用同一个微信用户完成：

1. 学校 A 停留约 12 秒后返回。
2. 学校 B 停留约 5 秒后返回。
3. 再次进入学校 A，停留约 8 秒后返回。
4. 学校 C 停留 1～2 秒后返回。

预期结果：

- A：约 20 秒、2 次，兴趣排序第一。
- B：约 5 秒、1 次，排在 A 后。
- C：不足 3 秒，不应出现。

---

## 八、出现问题时按这个顺序查

### Network 中完全没有 `/api/user/activity`

- 确认修改的是正在构建测试版的源码目录。
- 确认 `utils/activity.js` 已存在且路径大小写正确。
- 确认详情页顶部成功引入。
- 确认 `onLoad` 控制台出现开始计时日志。
- 确认点击返回时触发 `onHide` 或 `onUnload`。
- 确认详情页收到的 `options.id` 是有效正整数。

### 返回 401 或提示找不到模块

- 检查详情页到 `utils/activity.js` 的相对路径是否为 `../../utils/activity`。
- 检查 `activity.js` 内到 `request.js` 的相对路径是否为 `./request`。

### 接口返回 401

- 查看 Storage 是否存在 `token`。
- 查看请求头是否含 `Authorization: Bearer ...`。
- 重新清缓存并登录，避免使用过期 token。
- 不要因为手机号已经绑定就假定当前 Storage 一定有有效 token。

### 接口返回 404

- 确认访问的是当前联调后端地址。
- 确认后端运行进程已加载 `UserActivityController`。
- 当前后端电脑已验证该接口存在；若前端仍收到 404，重点检查实际请求 URL。

### 接口返回“学校不存在”

- `schoolSource` 与 ID 所属表不匹配。
- 国际学校必须使用 `international + school.id`。
- 区重点/分数查询学校必须使用 `score + score_query_tool_school.id`。

### 接口显示成功但后台暂时看不到

- 刷新管理页面并重新打开该用户的“查看行为”。
- 确认后台查看的是同一个手机号/用户 ID。
- 让后端检查本地 `user_activity` 表，而不是云数据库。

### 管理后台的最终展示要求

行为追踪接通后，管理后台用户卡片应满足：

1. “收藏、留资、评价、祝福”的数量都可点击并查看具体明细。
2. 评价明细显示学校、五级评价文字（夯、赞、一般、差、拉）和时间；当前评价不是文字评论。
3. 祝福明细显示祝福原文、发布状态和时间。
4. 收藏明细显示学校名称和收藏时间。
5. 留资明细显示家长、年级、目标学校、电话、问题、预约信息、状态和时间。
6. 兴趣学校默认只显示累计停留时间最长的 3 所；超过 3 所时右侧显示箭头，点击后展开该用户浏览过的全部学校，再次点击收起。
7. 最近浏览时间线继续显示每次浏览的学校、时间和单次停留时长。
8. 前端完成 A/B/C 测试后，要同时核对 Network 上报成功、管理后台前三排序正确、展开后能看到全部兴趣学校。

---

## 九、禁止事项

- 不要操作云数据库。
- 不要修改后端接口路径或请求结构。
- 不要把浏览学校自动当作用户明确填写的“目标学校”。
- 不要记录不足 3 秒的误触。
- 不要用空 `.catch(() => {})` 吞掉行为上报错误。
- 不要为了合并追踪代码覆盖详情页其他已有功能。
- 不要把学校列表页停留时间当成具体学校兴趣。

---

## 十、交给前端 Codex 的直接指令

可将下面整段连同本文路径一起发给前端电脑上的 Codex：

```text
请完整阅读：
C:\claude\MMMMMMMMMMMMMMMMMMMMD\用户学校浏览行为追踪_前端Codex交接文档.md

然后在当前电脑实际用于构建测试版的“联朋升学”微信小程序源码中完成用户学校浏览行为追踪。

要求：
1. 修改前先检查真实目录、utils/request.js 的调用约定、token 保存方式、详情页已有生命周期和未提交改动。
2. 新增 utils/activity.js，按文档使用带成功/失败日志、成功后才标记 sent 的实现。
3. 至少接入 pages/school-detail/school-detail.js；若存在 score-school-detail，也一并接入。
4. 不覆盖详情页已有 onHide/onUnload，必须合并。
5. 确认联调配置继续连接后端电脑的本地 8080，不要切换云端。
6. 不操作任何数据库，不操作云数据库。
7. 修改后编译并按文档完成 5～10 秒单条测试，必须在 Network 看到 POST /api/user/activity，并报告 HTTP 状态、业务 code 和控制台日志。
8. 如果测试失败，不要隐藏错误，按文档第八节定位并报告具体请求和响应。
```
