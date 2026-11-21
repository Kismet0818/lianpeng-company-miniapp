# 联朋升学微信小程序前端交接文档（初版）

> 文档版本：v0.1  
> 更新时间：2026-08-26  
> 项目目录：`D:\lianpengshengxue`

## 1. 项目概况

本项目是“联朋升学”微信小程序前端，采用原生微信小程序技术栈：

- 页面：WXML
- 样式：WXSS
- 逻辑：JavaScript
- 配置：JSON
- 网络请求：`wx.request` / 微信云托管 `wx.cloud.callContainer`
- 底部导航：自定义 TabBar
- 当前基础库版本：`3.16.2`

项目不是 Taro、React 或 Vue 项目，没有 `package.json`，也没有 npm 类型检查、Lint 或构建脚本。日常编译、预览和上传均通过微信开发者工具完成。

## 2. 快速启动

### 2.1 开发工具

使用微信开发者工具打开项目根目录：

```text
D:\lianpengshengxue
```

项目 AppID：

```text
wxc9192b93d888816d
```

### 2.2 本地后端模式

当前 `app.js` 配置为本地联调：

```js
useCloud: false,
localBaseUrl: 'http://192.168.3.5:8080'
```

开发者电脑与后端电脑需要处于可互通的局域网，并确保：

- 后端监听 `8080`；
- 微信开发者工具允许请求本地网络地址；
- 调试阶段按需开启“不校验合法域名、web-view（业务域名）、TLS版本以及HTTPS证书”。

如果后端电脑IP发生变化，只修改 `app.js` 中的 `localBaseUrl`，不要在各页面中硬编码基础地址。

### 2.3 云托管模式

切换线上云托管时：

```js
useCloud: true
```

当前云托管配置：

```text
cloudEnv: cloud1-d6g28bq6c04919547
cloudService: lpsx1
```

请求层会自动添加：

```text
X-WX-SERVICE: lpsx1
```

## 3. 核心目录

```text
├─ app.js                    全局初始化、登录、运行环境配置
├─ app.json                  页面路由、窗口配置、自定义TabBar配置
├─ app.wxss                  全局基础样式
├─ custom-tab-bar/           自定义底部导航
├─ pages/                    所有业务页面
├─ assets/                   本地图标、品牌资源和页面图片
└─ utils/
   ├─ request.js             统一网络请求与响应处理
   ├─ image.js               图片地址规范化
   ├─ upload.js              上传工具
   └─ util.js                通用工具
```

页面通常由同名的四个文件组成：

```text
page.js
page.json
page.wxml
page.wxss
```

## 4. 全局导航和主要页面

### 4.1 自定义底部导航

底部四个主入口：

| 标签 | 页面 |
|---|---|
| 首页 | `pages/home/home` |
| 学校库 | `pages/school/school` |
| 留学辅导 | `pages/study-abroad/study-abroad` |
| 个人中心 | `pages/user/user` |

TabBar页面必须使用 `wx.switchTab`，普通详情页面使用 `wx.navigateTo`。

### 4.2 主要业务路由

| 模块 | 页面路径 | 说明 |
|---|---|---|
| 首页 | `pages/home/home` | 首页聚合、推荐学校、视频和功能入口 |
| 学校库 | `pages/school/school` | 学校列表、搜索、筛选、对比 |
| 学校详情 | `pages/school-detail/school-detail` | 学校库完整详情、相册、收藏、咨询 |
| 学校排名 | `pages/school-ranking/index` | 排名入口 |
| 测评 | `pages/assessment/assessment` | 匹配测评 |
| 对比 | `pages/compare/compare` | 学校对比 |
| 招生活动 | `pages/open-day/open-day` | 开放日列表 |
| 开放日详情 | `pages/open-day-detail/open-day-detail` | 活动详情和咨询 |
| 攻略列表 | `pages/guide-list/guide-list` | 升学攻略 |
| 攻略详情 | `pages/guide-detail/guide-detail` | 文章详情 |
| 留学辅导 | `pages/study-abroad/study-abroad` | 工具、语言、竞赛、留学、课程 |
| 咨询表单 | `pages/lead/lead` | 通用咨询线索提交 |
| 个人中心 | `pages/user/user` | 用户资料、收藏和咨询记录 |
| 关于我们 | `pages/about/about` | 品牌介绍和官网入口 |
| 官网WebView | `pages/webview/webview` | 加载 `https://lianpeng.vip` |
| 查分工具首页 | `pages/score-tool/score-tool` | 16区县入口 |
| 区县招生页 | `pages/district-admission/district-admission` | 区县计划统计和三类查询入口 |
| 招生查询页 | `pages/quota-query/quota-query` | 名额到区、名额到校、1-15志愿 |
| 查分学校详情 | `pages/score-school-detail/score-school-detail` | 查分工具独立学校基础详情 |

`pages/feedback/` 当前是空目录，反馈页及路由已删除，可在清理项目时移除该空目录。

## 5. 网络请求规范

所有页面统一引用：

```js
const { request } = require('../../utils/request')
```

方法签名：

```js
request(url, method = 'GET', data = {}, needAuth = false)
```

后端标准响应格式：

```json
{
  "code": 200,
  "msg": "成功",
  "data": {}
}
```

请求层行为：

- `code === 200`：返回 `data`；
- `code === 401`：清除本地token；
- 其他业务状态：Toast显示后端 `msg`；
- 非2xx HTTP状态：显示具体HTTP状态码；
- 支持普通JSON字符串、带BOM JSON和二次序列化JSON；
- 无法解析时在控制台打印响应前300个字符。

需要登录的接口必须将第四个参数传为 `true`：

```js
request('/api/favorite/list', 'GET', {}, true)
```

## 6. 登录、Token和本地缓存

登录入口在 `app.js` 的 `login(maxRetries)`：

1. 调用 `wx.login` 获取一次性 `code`；
2. 请求 `/api/auth/wx-login`；
3. 保存后端返回的token；
4. 重试时重新调用 `wx.login`，不得复用旧code。

主要缓存键：

| Key | 用途 |
|---|---|
| `token` | 登录凭证 |
| `compareList` | 学校对比ID |
| `recentSchoolIds` | 最近浏览学校ID |
| `recentSchoolCache` | 最近浏览学校摘要 |
| `schoolPresetGrade` | 学校库预设学段 |

## 7. 学校库模块

主要接口：

```text
GET  /api/schools/list
GET  /api/schools/{id}
GET  /api/schools/{id}/images
POST /api/schools/batch
POST /api/schools/compare
```

学校列表支持名称、区县、学段、学费、性质和寄宿条件。寄宿条件目前由前端对自由文本进行判断，修改字段或文案时需要同步检查 `matchesBoardingType`。

学校详情相册来自：

```text
/api/schools/{id}/images
```

相册兼容数组、对象列表、JSON字符串和逗号分隔字符串，并按 `sortOrder` 排序。

## 8. 查分工具模块

### 8.1 页面链路

```text
留学辅导（工具）
  → 查分工具首页
  → 选择区县
  → 区县招生计划
  → 名额到区 / 名额到校 / 1-15志愿
  → 查分工具学校基础详情
```

查分工具入口路径：

```text
/pages/score-tool/score-tool
```

### 8.2 固定学年

当前前端查询学年固定为：

```js
const ACADEMIC_YEAR = '2025-2026'
```

页面显示为“2026年”。后续开放其他年份时，应统一改为页面状态和接口参数，不要只增加静态年份标签。

### 8.3 核心字段语义

实际工具表字段语义：

```text
target_type = 2：名额到区
target_type = 1：名额到校
high_school_district：提供名额的高中所在区
target_district：招生目标区；到校数据中表示目标初中所在区
target_school_name：目标初中名称
```

特别注意：早期接口文档曾写“名额到校的 targetDistrict 为空”，但实际SQL中的名额到校记录包含 `target_district`。前后端联调时以当前数据库和实际接口返回为准。

### 8.4 当前接口

```text
GET /api/score-query/region
    ?district=闵行区
    &year=2025-2026

GET /api/score-query/school
    ?district=闵行区
    &year=2025-2026

GET /api/score-query/volunteer
    ?district=闵行区
    &year=2025-2026

GET /api/score-query/volunteer/meta
GET /api/score-query/schools/{id}
```

名额到区前端再次校验：

```text
targetType === 2
targetDistrict === 当前区
```

名额到校前端再次校验：

```text
targetType === 1
targetDistrict === 当前区
```

随后按 `targetSchoolName` 分组。

1-15志愿筛选逻辑：

- 全部：不筛选 `highSchoolDistrict`；
- 本区高中：`highSchoolDistrict === 当前区`；
- 外区高中：`highSchoolDistrict !== 当前区`；
- 本区和外区互补；
- 基础结果始终限制 `targetDistrict === 当前区`。

### 8.5 查分学校ID与学校库ID不能混用

这是当前最重要的数据风险：

- 查分工具使用 `score_query_tool_school.id`；
- 学校库使用 `school.id`；
- 两套ID不是同一套主键；
- 不得直接拿 `highSchoolId` 跳转 `/pages/school-detail/school-detail?id=...`，否则可能打开错误学校。

当前查分列表点击后进入：

```text
/pages/score-school-detail/score-school-detail?id={highSchoolId}
```

如果后续需要复用学校库完整详情，后端应增加明确的 `school_id` 外键映射，或提供可靠的映射接口。不要仅依赖模糊名称匹配。

### 8.6 计划变化字段尚未明确

当前接口中的：

- `quotaCount`：本年度计划人数；
- `minScore`：最低分；
- `changeRange`：目前按最低分相比上年变化展示。

计划栏若要展示“▲2 / ▼2 / 平”，后端应补充：

```text
quotaChangeRange
```

或返回上一年度计划人数供前端计算。不要把最低分的 `changeRange` 同时用于计划人数变化。

## 9. 咨询、收藏和用户数据

主要接口：

```text
POST /api/lead/submit
GET  /api/favorite/list
POST /api/favorite/toggle
GET  /api/user/profile
PUT  /api/user/profile
GET  /api/user/lead-history
POST /api/auth/decrypt-phone
```

收藏、个人资料和咨询记录需要登录。咨询提交页面会读取：

```text
GET /api/config/platform
```

各业务页进入咨询页时通常通过 `target` 和 `source` 参数传递来源。

## 10. 图片与资源

本地资源目录：

```text
assets/brand
assets/icons
assets/images
assets/liuxuefudao
assets/monthly-live
assets/study-abroad
```

远程资源主要位于阿里云OSS。图片显示应统一使用合适的 `mode`：

- Logo、图标：`aspectFit`
- 封面和相册：`aspectFill`
- 需要按原始比例撑开：`widthFix`

不要把带临时签名和过期时间的OSS URL作为长期静态资源写入代码。

## 11. 导航栏与安全区域

全局默认配置为自定义导航：

```json
"navigationStyle": "custom"
```

部分页面在自己的JSON中覆盖为系统导航，例如查分查询页。新增或修改页面时必须先检查页面JSON，避免同时出现系统导航和自定义导航。

自定义导航推荐使用：

```js
wx.getWindowInfo()
wx.getMenuButtonBoundingClientRect()
```

底部固定内容需要考虑：

```css
env(safe-area-inset-bottom)
```

## 12. 官网WebView配置

关于我们页面通过内部WebView打开：

```text
https://lianpeng.vip
```

微信公众平台需要配置业务域名：

```text
https://lianpeng.vip
```

不要改为唤起外部浏览器，也不要将官网直接嵌入关于我们内容页。

## 13. 视觉规范

当前品牌基础色：

```text
品牌主色：#64A8CD
品牌渐变：linear-gradient(135deg, #4D92BB 0%, #64A8CD 55%, #91C9E3 100%)
深色标题：#24485D
正文：#405D6D
辅助文字：#8AA0AD
页面背景：#F4F8FA
卡片背景：#FFFFFF
橙红强调：#F05A32（主要用于查分年份和关键状态）
```

设计原则：

- 蓝白为主，橙红只用于少量重点；
- 卡片阴影保持轻量；
- 不使用大面积高饱和渐变、玻璃拟态和复杂插画；
- 单页样式写在页面WXSS中，不依赖修改全局样式解决局部问题；
- 优先复用项目现有图标和真实品牌资源。

## 14. 发布前配置检查

上线前至少检查：

1. `app.js` 的 `useCloud` 是否符合目标环境；
2. 云环境和云托管服务名是否正确；
3. 微信公众平台request合法域名是否配置；
4. `https://lianpeng.vip` 是否配置为业务域名；
5. OSS图片域名是否配置为downloadFile/image合法域名；
6. 登录、手机号授权、收藏和咨询是否在真机验证；
7. 自定义TabBar选中状态是否正确；
8. iPhone全面屏底部安全区是否正常；
9. 查分工具三个模式是否按区返回正确数据；
10. 所有新增页面是否已注册到 `app.json`。

## 15. 已知问题和待办

### 高优先级

- 查分学校ID与学校库ID没有正式映射，暂时保留独立查分详情页。
- 名额到校按区查询参数与早期接口文档不一致，需要后端补充并维护最新文档。
- 计划人数同比变化缺少独立字段。
- 反馈/纠错功能因后端未实现已删除。

### 中优先级

- 区县招生页计划统计目前在前端静态维护，应逐步改为后端接口。
- 查分工具当前仅开放2026年，年份标签尚未数据化。
- 项目没有自动化Lint、测试和构建脚本，主要依赖开发者工具人工验证。
- 多个页面存在相似的自定义导航计算，可考虑后续抽成公共组件。

### 清理项

- `pages/feedback/` 为空目录，可删除。
- `pages/ranking/` 与 `pages/school-ranking/` 同时存在，需确认旧目录是否仍有用途后再清理。
- `pages/score-school-detail/` 当前仍在使用，不可删除。

## 16. 推荐联调验证清单

### 基础

- 首页加载成功；
- 四个Tab切换正常；
- 页面返回、滚动和胶囊区域无重叠；
- 图片无404和拉伸。

### 学校库

- 列表分页；
- 名称、区县、学段、学费、性质和寄宿筛选；
- 学校详情和相册；
- 收藏、取消收藏；
- 对比添加和移除。

### 查分工具

- 16个区县入口均可点击；
- 区县名称和学校数量未改变；
- 名额到区按 `targetDistrict` 隔离；
- 名额到校按目标初中分组；
- 1-15志愿的全部、本区、外区互补；
- 学校点击进入查分基础详情，不串到学校库错误ID；
- 空数据、接口超时和非JSON响应有明确状态。

### 用户与咨询

- 微信登录；
- 手机号授权；
- 咨询提交；
- 咨询记录；
- 收藏列表；
- 关于我们和官网WebView。

## 17. 后续交接建议

建议正式交接时同时提供：

- 最新后端接口文档；
- 测试账号和管理员账号；
- 本地后端启动方式；
- 云托管发布权限；
- 微信公众平台权限；
- OSS目录和上传规范；
- 数据库表结构及最新备份；
- 测试环境、正式环境地址；
- 已知问题负责人及优先级。

本文件是第一版，可继续补充部署截图、接口示例、页面截图、负责人、账号权限申请流程和版本发布记录。
