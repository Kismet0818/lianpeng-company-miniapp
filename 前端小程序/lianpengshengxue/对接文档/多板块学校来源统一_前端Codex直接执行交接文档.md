# 多板块学校来源统一：前端 Codex 直接执行交接文档

> 更新时间：2026-09-09  
> 执行对象：维护最新微信小程序源码的前端 Codex  
> 目标：彻底修复学校来源混用导致的浏览记录丢失、返回时提示“学校不存在”、表情评价有时失败或串校、祝福墙学校范围错误。  
> 重要边界：请在前端同事电脑上的**最新源码**中扫描和修改。不要照搬后端电脑上的旧前端副本，也不要假定本文中的页面文件名与最新版本完全相同。

---

## 一、先理解根因

当前小程序有两个产品入口：

1. `学校库`
2. `升学工具`（原名“查分工具”）

这两个名称表示用户从哪个业务板块进入，属于“页面入口/业务模块”；它们不等于学校的数据来源。

升学工具现在也可能展示全部学校，因此不能因为用户从升学工具进入，就把所有学校写成：

```js
schoolSource = 'score'
```

后端共有四类学校数据源：

| 后端数据 | schoolSource | 典型接口 |
|---|---|---|
| 国际学校/原学校表 `school` | `international` | `/api/schools/**` |
| 分数查询高中 `score_query_tool_school` | `score` | `/api/score-query/schools/**` |
| 初中 `junior_high_school` | `junior` | `/api/junior-schools/**` |
| 一般高中 `general_high_school` | `general` | `/api/general-high-schools/**` |

`schoolId` 只在各自表内唯一，不是全系统唯一。相同数字可能代表不同学校。例如 ID 49 同时可以是国际学校、分数高中和初中。系统必须使用以下复合标识：

```text
schoolSource + schoolId
```

2026-09-09 后端日志已经捕获到错误请求：前端从升学工具进入一般高中后，上报了 `source=score`，学校 ID 为 304、317、346、368；这些 ID 实际属于 `general_high_school`。后端目前只对“score 查不到但 general 能查到”的无歧义情况做了临时兼容，这不是长期方案，也无法识别“不同表恰好存在相同 ID”的串校。

---

## 二、本次 Codex 必须完成的工作

不要只改出现弹窗的一个页面。先全局扫描，再统一修复以下链路：

```text
各板块学校列表取得数据
→ 每条学校数据带真实 schoolSource
→ 点击学校时把 schoolSource 和 schoolId 一起传入详情页
→ 详情页保存这两个值
→ 浏览行为、表情评价、祝福发布/列表全部复用同一组值
```

需要全局搜索的关键词至少包括：

```text
schoolSource
schoolId
score-school-detail
school-detail
junior
general-high
/api/user/activity
/api/school-reactions/
/api/blessing
startSchoolView
finishSchoolView
navigateTo
reLaunch
```

同时搜索所有学校数组的拼接、映射和分组逻辑，例如：

```text
map(
concat(
Promise.all
allSchools
schoolList
schoolGroups
```

不要根据旧文件名猜测完成状态。修改前先列出最新源码中：

- 学校库入口页；
- 升学工具入口页；
- 四类学校列表的请求位置；
- 所有学校详情页；
- 统一请求封装；
- 浏览行为工具；
- 表情评价组件或调用；
- 祝福发布页和祝福列表页。

---

## 三、列表层：在数据进入前端时立即标记来源

最稳妥的方法是在每类接口响应映射进前端数组时补上固定来源，不要等点击以后根据页面入口猜。

示例（字段名按最新项目实际结构调整）：

```js
const internationalSchools = internationalResult.map(item => ({
  ...item,
  schoolSource: 'international'
}))

const scoreSchools = scoreResult.map(item => ({
  ...item,
  schoolSource: 'score'
}))

const juniorSchools = juniorResult.map(item => ({
  ...item,
  schoolSource: 'junior'
}))

const generalSchools = generalResult.map(item => ({
  ...item,
  schoolSource: 'general'
}))
```

如果多个接口结果会被合并成一个“全部学校”数组，也必须在合并前完成标记：

```js
const allSchools = [
  ...internationalSchools,
  ...scoreSchools,
  ...juniorSchools,
  ...generalSchools
]
```

严禁使用下面这种逻辑：

```js
// 错误：业务入口不是数据来源
const schoolSource = currentTab === '升学工具' ? 'score' : 'international'

// 错误：只凭 ID 数值范围猜来源
const schoolSource = school.id > 200 ? 'general' : 'score'

// 错误：合并数组后把全部项目统一覆盖成同一来源
allSchools.map(item => ({ ...item, schoolSource: 'score' }))
```

如果后端响应本身已经返回 `schoolSource`，优先原样保留；只有各独立接口没有返回时，才在该接口的映射位置补固定值。

---

## 四、跳转层：来源必须跟随学校进入详情页

所有点击学校的跳转都要同时传 `schoolId` 和 `schoolSource`。建议封装统一跳转函数，避免不同入口各写一套。

```js
function openSchoolDetail(school) {
  const schoolId = Number(school.id || school.schoolId)
  const schoolSource = String(school.schoolSource || '')

  if (!schoolId || !['international', 'score', 'junior', 'general'].includes(schoolSource)) {
    console.error('[school] invalid identity', { schoolId, schoolSource, school })
    wx.showToast({ title: '学校信息不完整', icon: 'none' })
    return
  }

  wx.navigateTo({
    url: `/pages/实际详情页路径/实际详情页路径?id=${schoolId}&schoolSource=${encodeURIComponent(schoolSource)}`
  })
}
```

如果不同来源仍使用不同详情页，可以保留页面分流，但来源仍要传递：

```js
const pageBySource = {
  international: '/pages/school-detail/school-detail',
  score: '/pages/score-school-detail/score-school-detail',
  junior: '/pages/junior-school-detail/junior-school-detail',
  general: '/pages/general-high-school-detail/general-high-school-detail'
}

const page = pageBySource[school.schoolSource]
wx.navigateTo({
  url: `${page}?id=${Number(school.id)}&schoolSource=${encodeURIComponent(school.schoolSource)}`
})
```

分享链接、最近浏览、收藏、搜索结果、首页推荐和对比结果也可能进入详情页，必须一并检查，不能只修两个主入口。

---

## 五、详情页：只使用传入的真实来源

在详情页 `onLoad(options)` 中读取并校验：

```js
const VALID_SOURCES = ['international', 'score', 'junior', 'general']

Page({
  data: {
    schoolId: 0,
    schoolSource: '',
    school: null
  },

  onLoad(options) {
    const schoolId = Number(options.id || options.schoolId)
    const schoolSource = String(options.schoolSource || '')

    if (!schoolId || !VALID_SOURCES.includes(schoolSource)) {
      console.error('[school-detail] invalid options', options)
      wx.showToast({ title: '学校信息不完整', icon: 'none' })
      return
    }

    this.setData({ schoolId, schoolSource })
    this.loadSchoolDetail()
  }
})
```

详情接口也要按来源选择，而不是按“从学校库还是升学工具进入”选择：

```js
const detailApi = {
  international: id => `/api/schools/${id}`,
  score: id => `/api/score-query/schools/${id}`,
  junior: id => `/api/junior-schools/${id}`,
  general: id => `/api/general-high-schools/${id}`
}
```

确认最新请求封装接受的参数格式后再调用。

---

## 六、浏览行为上报

接口：

```http
POST /api/user/activity
Authorization: Bearer <JWT>
Content-Type: application/json
```

请求体：

```json
{
  "schoolSource": "general",
  "schoolId": 368,
  "durationSeconds": 8
}
```

调用统一行为工具时，必须传详情页保存的来源：

```js
startSchoolView(this, this.data.schoolSource, this.data.schoolId)
```

不要再写死：

```js
startSchoolView(this, 'score', schoolId)
```

可靠性要求：

- 停留少于 3 秒不发送；
- 有有效 JWT 才发送；
- 防止 `onHide` 和 `onUnload` 重复提交；
- 请求成功后再标记 `sent=true`；
- 失败不能弹“学校不存在”打断用户，但开发版控制台必须保留完整错误日志；
- 不允许用 `.catch(() => {})` 完全吞掉错误。

后端当前会兼容 `score → general` 的无歧义情况，但前端仍必须修正，因为后端无法解决不同表恰好拥有相同 ID 的情况。

---

## 七、五级表情评价

### 7.1 获取评价

```http
GET /api/school-reactions/{schoolId}?schoolSource={schoolSource}
Authorization: Bearer <JWT>
```

示例：

```http
GET /api/school-reactions/368?schoolSource=general
```

### 7.2 提交或更换评价

```http
POST /api/school-reactions/{schoolId}
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{
  "schoolSource": "general",
  "reactionType": 2
}
```

`reactionType` 只允许 1～5。GET 查询参数与 POST 请求体里的 `schoolSource` 必须完全一致，并且都来自详情页保存的真实来源。

可封装为：

```js
function loadReaction(page) {
  const { schoolSource, schoolId } = page.data
  return request(
    `/api/school-reactions/${schoolId}?schoolSource=${encodeURIComponent(schoolSource)}`,
    'GET',
    null,
    true
  )
}

function submitReaction(page, reactionType) {
  const { schoolSource, schoolId } = page.data
  return request(
    `/api/school-reactions/${schoolId}`,
    'POST',
    { schoolSource, reactionType: Number(reactionType) },
    true
  )
}
```

注意：此前测试版只传 `schoolId`，部分评价看似成功，实际可能写到了同 ID 的国际学校。不能以“接口返回成功”作为唯一验收标准，必须重新进入同一所学校检查高亮，并在管理后台确认学校名称。

---

## 八、祝福墙

发布祝福必须携带：

```json
{
  "content": "祝福内容",
  "schoolSource": "general",
  "schoolId": 368
}
```

查询某所学校祝福时同时带两个范围参数：

```http
GET /api/blessing/list?page=1&size=20&schoolSource=general&schoolId=368
```

详情页跳转到祝福发布页或列表页时，也必须把当前页面保存的 `schoolSource`、`schoolId`、`schoolName` 一起传递；发布页不得把来源重新写死为 `score`。

---

## 九、兼容分享链接与旧缓存

旧分享链接可能只有 `id`、没有 `schoolSource`。不要静默猜错学校。

建议策略：

1. 新生成的分享链接必须带 `schoolSource`。
2. 页面从收藏、最近浏览等本地缓存恢复时，缓存结构也要保存来源。
3. 旧缓存没有来源时，应重新通过当前列表数据定位；无法唯一确定时提示用户返回学校列表重新进入。
4. 不允许仅凭 ID 到四张表依次试查并取第一个结果，因为重复 ID 会命中错误学校。

推荐的本地缓存结构：

```js
{
  schoolId: 368,
  schoolSource: 'general',
  schoolName: '上海市老港中学'
}
```

---

## 十、错误处理要求

需要区分核心功能和后台统计：

- 学校详情加载失败：页面显示明确错误，并记录请求 URL、参数和响应。
- 表情评价提交失败：恢复点击前状态，提示“评价提交失败，请重试”，控制台记录详细错误。
- 祝福提交失败：保留用户输入，不能清空后只提示失败。
- 浏览行为上报失败：不打断用户、不弹业务 Toast，但开发版控制台必须记录详细错误，便于联调。
- HTTP 401：清理失效 token 并重新登录。
- HTTP 403：才表示账号确实被停用。
- HTTP 429：当前后端阈值已经大幅放宽；若仍出现，记录具体 URL、响应头和时间，不要无限自动重试。

---

## 十一、必须完成的验收矩阵

至少选择四所学校，每种来源一所：

| 测试类型 | 预期 schoolSource | 必测内容 |
|---|---|---|
| 国际学校 | `international` | 详情、停留上报、评价、祝福 |
| 分数查询高中 | `score` | 详情、停留上报、评价、祝福 |
| 初中 | `junior` | 详情、停留上报、评价、祝福 |
| 一般高中 | `general` | 详情、停留上报、评价、祝福 |

每所学校按以下步骤测试：

1. 分别从“学校库”和“升学工具”进入同一所学校，确认两条路径传递的 `schoolSource` 一致。
2. 在 Network 查看详情请求使用了与来源对应的接口。
3. 停留 5～10 秒后返回，确认 `POST /api/user/activity` 请求体来源正确、业务码为 200。
4. 重新进入，点击一个表情，再更换另一个表情；确认 GET 和 POST 来源一致，计数和高亮正确。
5. 发布一条测试祝福，确认只在当前学校列表出现，不出现在另一所学校。
6. 在管理后台查看该用户，确认兴趣学校、评价学校和祝福学校名称都与前端点击的学校一致。
7. 测试完成后删除明确标记的测试祝福；不要删除无法确认归属的历史数据。

特别回归以下已出现问题的 ID：

```text
general: 304、317、346、368
```

其中：

```text
general / 368 = 上海市老港中学
```

从“升学工具”进入它时，也必须传 `general`，不能传 `score`。

---

## 十二、Codex 完成后的交付格式

请不要只回复“已修改”。完成后向用户提供：

1. 扫描到的学校入口页、列表请求、详情页和相关工具文件清单。
2. 实际修改的文件清单及每个文件的作用。
3. 修复前错误的数据流与修复后的正确数据流。
4. 四类学校的 Network 验收结果，包括详情、行为、评价和祝福接口的 `schoolSource`。
5. 编译、语法检查和真机/开发者工具测试结果。
6. 仍未完成或需要后端配合的事项。
7. 明确说明修改发生在前端同事电脑的最新源码中，而不是后端电脑的旧副本。

完成标准不是“页面不再弹错”，而是：四类学校无论从学校库还是升学工具进入，详情正确，浏览行为正确入库，评价归属正确，祝福按学校隔离，管理后台显示的学校名称与用户实际点击一致。
