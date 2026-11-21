# 首页 Tab 切换 — 前后端对接文档

> 涉及页面：`pages/home/home.*`  
> 后端改动：**无**（全部复用已有接口）  
> 日期：2026-08-06

---

## 一、当前状态

首页四个 Tab「当地热门学校 / 最近浏览 / 收藏的学校 / 对比的学校」共用 `displaySchools` 渲染，模板使用字段 `name`、`logoUrl`、`district`、`curriculum`、`tuitionMin`、`tuitionMax`。

| Tab | 索引 | 当前数据 | 问题 |
|-----|------|----------|------|
| 当地热门 | 0 | `hotSchools` | ✅ 正常 |
| 最近浏览 | 1 | `hotSchools` | ❌ 假数据 |
| 收藏的学校 | 2 | `GET /api/favorite/list` | ⚠️ 字段名不匹配 |
| 对比的学校 | 3 | `hotSchools` | ❌ 假数据 |

---

## 二、Tab 1「最近浏览」

### 数据流

```
用户浏览学校详情页 → wx.Storage 写入 schoolId
切到 Tab 1 → 读 Storage → POST /api/schools/compare → 渲染
```

### 前端实现

#### 2.1 在学校详情页记录浏览

`pages/school-detail/school-detail.js`，在 `onLoad` 或 `onShow` 中：

```js
const schoolId = options.id  // 从页面参数获取

let history = wx.getStorageSync('browseHistory') || []

// 去重
history = history.filter(id => id !== schoolId)
// 插入头部
history.unshift(schoolId)
// 上限 10
history = history.slice(0, 10)

wx.setStorageSync('browseHistory', history)
```

#### 2.2 首页 Tab 切换时加载

`pages/home/home.js`，修改 `onTabChange`：

```js
onTabChange(e) {
    const activeTab = Number(e.currentTarget.dataset.index)
    this.setData({ activeTab })

    if (activeTab === 1) {
        this.loadRecentBrowsed()       // 新增
    } else if (activeTab === 2) {
        this.loadFavorites()           // 已有
    } else if (activeTab === 3) {
        this.loadCompared()            // 新增
    } else {
        this.setData({ displaySchools: this.data.hotSchools })
    }
}
```

#### 2.3 `loadRecentBrowsed` 方法

```js
loadRecentBrowsed() {
    const history = wx.getStorageSync('browseHistory') || []
    if (history.length === 0) {
        this.setData({ displaySchools: [] })
        return
    }
    request('/api/schools/compare', 'POST', { schoolIds: history })
        .then(data => {
            this.setData({ displaySchools: data })
        })
}
```

---

## 三、Tab 2「收藏的学校」

### 问题

`GET /api/favorite/list` 返回 `FavoriteVO`，字段名与 `School` 不同：

| WXML 模板字段 | School 实体 | FavoriteVO |
|---------------|-------------|------------|
| `name` | `school.name` | ❌ 无，对应 `schoolName` |
| `logoUrl` | `school.logoUrl` | ❌ 无，对应 `schoolLogo` |
| `district` | `school.district` | ✅ 有 |
| `curriculum` | `school.curriculum` | ✅ 有 |
| `grades` | `school.grades` | ✅ 有 |
| `tuitionMin` | `school.tuitionMin` | ❌ 无 |
| `tuitionMax` | `school.tuitionMax` | ❌ 无 |

### 解决方案（二选一）

#### 方案 A：前端做字段映射（推荐，后端不改）

在 `loadFavorites` 中转换：

```js
loadFavorites() {
    request('/api/favorite/list', 'GET', {}, true).then(data => {
        const schools = data.map(item => ({
            id: item.schoolId,
            name: item.schoolName,
            logoUrl: item.schoolLogo,
            district: item.district,
            curriculum: item.curriculum,
            grades: item.grades
        }))
        this.setData({ displaySchools: schools })
    })
}
```

#### 方案 B：后端新增一个简洁接口

在 `FavoriteController` 加一个方法，直接调 `POST /api/schools/compare` 逻辑，返回 `List<School>` 而非 `List<FavoriteVO>`。

> 如需方案 B，告知后端补充即可。

---

## 四、Tab 3「对比的学校」

### 数据流

```
学校库添加对比 → app.globalData.compareList 更新
切到 Tab 3 → 读 compareList → POST /api/schools/compare → 渲染
```

### 前端实现

#### 4.1 `loadCompared` 方法

```js
loadCompared() {
    const app = getApp()
    const compareList = app.globalData.compareList || []
    if (compareList.length === 0) {
        this.setData({ displaySchools: [] })
        return
    }
    request('/api/schools/compare', 'POST', { schoolIds: compareList })
        .then(data => {
            this.setData({ displaySchools: data })
        })
}
```

> 无需 JWT（`/api/schools/**` 在白名单中），第三个参数 `needAuth` 可省略。

---

## 五、首页 WXML 适配

`displaySchools` 每一项点击跳转学校详情：

```html
<swiper-item wx:for="{{displaySchools}}" wx:key="id">
  <view class="school-card" bindtap="onTapSchool" data-id="{{item.id}}">
    <image src="{{item.logoUrl}}" />
    <text>{{item.name}}</text>
    ...
  </view>
</swiper-item>
```

`onTapSchool` 方法：

```js
onTapSchool(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/school-detail/school-detail?id=${id}` })
}
```

---

## 六、接口速查

| 接口 | 方法 | 鉴权 | 返回 | 用途 |
|------|------|------|------|------|
| `/api/home/overview` | GET | 无 | `{ hotSchools: List<School> }` | Tab 0 当地热门 |
| `/api/schools/compare` | POST | 无 | `List<School>` | Tab 1 + Tab 3 |
| `/api/favorite/list` | GET | JWT | `List<FavoriteVO>` | Tab 2 收藏 |

---

## 七、完整 `onTabChange` 参考

```js
onTabChange(e) {
    const activeTab = Number(e.currentTarget.dataset.index)
    this.setData({ activeTab })

    switch (activeTab) {
        case 0: // 当地热门学校
            this.setData({ displaySchools: this.data.hotSchools })
            break
        case 1: // 最近浏览
            this.loadRecentBrowsed()
            break
        case 2: // 收藏的学校
            this.loadFavorites()
            break
        case 3: // 对比的学校
            this.loadCompared()
            break
    }
}
```

---

## 八、School 实体字段参考（`POST /api/schools/compare` 返回）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 学校 ID |
| name | String | 学校名称 |
| logoUrl | String | 校徽 OSS 地址 |
| district | String | 行政区 |
| city | String | 城市 |
| curriculum | String | 课程体系 |
| grades | String | 学段 |
| tuitionMin | BigDecimal | 最低学费 |
| tuitionMax | BigDecimal | 最高学费 |
| schoolType | String | 学校性质 |
| address | String | 地址 |
| description | String | 介绍（富文本） |
| teachingMode | String | 授课方式 |
| foundingTime | String | 创办时间 |
| studentCount | Integer | 在校人数 |
| campusArea | BigDecimal | 占地面积 |
| teacherRatio | String | 师生比 |
| classSize | String | 班级人数 |
| boardingInfo | String | 寄宿信息 |
| graduationResult | String | 升学情况 |
| teacherSeniorRatio | BigDecimal | 高级职称比例 |
