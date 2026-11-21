# 联朋升学 (LPSX) 项目文档

> 最后更新：2026-08-06

---

## 一、项目概述

"联朋升学"是一个上海国际学校择校服务平台，由 **微信小程序（前端）** + **Spring Boot（后端）** 构成，面向家长提供学校查询、升学测评、择校指南、留资咨询等功能。

- **小程序名称**：联朋升学
- **AppID**：wx2b23cebe1b9fe614
- **品牌色**：主色 `#1B3A5C`（深蓝），辅色 `#8B1A2B`（赭红），紫色系 `#7358ef` / `#7C5CFC`
- **品牌Slogan**：Guiding Every Step Toward Excellence.

---

## 二、技术栈

| 层级 | 技术 |
|------|------|
| **后端框架** | Spring Boot 3.4.7 + JDK 25 |
| **ORM** | MyBatis-Plus 3.5.5 |
| **数据库** | MySQL 8.0 (192.168.3.5:3306 / lpsx) |
| **认证** | JWT (jjwt 0.11.5, HS256, 7天过期) |
| **工具库** | Hutool 5.8.26, Lombok |
| **前端框架** | 微信原生小程序 (基础库 3.16+) |
| **图片存储** | 阿里云 OSS (`lianpeng` bucket, `oss-cn-shanghai`) |
| **云服务** | 微信云托管 (CloudBase Run)，云数据库 `cloud1-d6g28bq6c04919547` |

---

## 三、项目结构

### 后端

```
lpsx/
├── pom.xml
├── src/main/java/com/example/lpsx/
│   ├── LpsxApplication.java              # 启动类
│   ├── common/
│   │   ├── config/
│   │   │   ├── CorsConfig.java           # 跨域
│   │   │   ├── peizhi.java               # 配置类
│   │   │   └── WebMvcConfig.java         # JWT拦截器 + 白名单
│   │   ├── exception/
│   │   │   ├── BusinessException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── interceptor/
│   │   │   └── JwtInterceptor.java
│   │   └── result/
│   │       └── Result.java               # 统一响应
│   ├── controller/
│   │   ├── AssessmentController.java     # 择校测评
│   │   ├── AuthController.java           # 登录/手机解密
│   │   ├── FavoriteController.java       # 收藏（返回 FavoriteVO）
│   │   ├── GuideController.java          # 择校指南文章（公开）
│   │   ├── GuideAdminController.java     # 择校指南管理（后台）
│   │   ├── HomeController.java           # 首页聚合
│   │   ├── LeadController.java           # 留资表单（含关注方向）
│   │   ├── OssController.java            # OSS 文件上传
│   │   ├── PlatformConfigController.java # 平台配置（视频号/直播）
│   │   ├── SchoolAdminController.java    # 学校管理（后台）
│   │   ├── SchoolController.java         # 学校列表/详情/对比/招生
│   │   └── UserController.java           # 用户信息
│   ├── entity/
│   │   ├── AdmissionInfo.java            # 招生动态（结构化字段）
│   │   ├── ChannelsVideo.java            # 视频号视频列表
│   │   ├── CustomerLead.java             # 留资（含 concern 字段）
│   │   ├── GuideArticle.java             # 择校指南文章
│   │   ├── PlatformConfig.java           # 平台配置 (KV表)
│   │   ├── School.java                   # 学校（15+字段）
│   │   ├── SchoolImage.java              # 学校图片
│   │   ├── User.java                     # 用户
│   │   └── UserFavorite.java             # 收藏（含冗余字段）
│   ├── mapper/                           # 9个 Mapper
│   ├── service/
│   │   └── impl/                         # 11个 Service
│   ├── vo/
│   │   └── FavoriteVO.java               # 收藏三表 JOIN 视图
│   └── utils/
│       ├── JwtUtils.java
│       └── WxDecryptUtils.java           # 微信手机号AES解密
├── src/main/resources/
│   ├── application.yml
│   ├── application-cloud.yml             # 云托管配置
│   └── static/logo/                      # 校徽（本地残余）
```

### 前端（微信小程序）

```
lianpengshengxue/
├── app.js / app.json / app.wxss
├── utils/
│   └── request.js                        # 封装wx.request
├── pages/
│   ├── home/home.*                       # 首页（聚合展示）
│   ├── school/school.*                   # 学校库（含学费筛选）
│   ├── school-detail/school-detail.*     # 学校详情页
│   ├── admission-detail/admission-detail.* # 招生动态详情（新增）
│   ├── assessment/assessment.*           # 智能择校测评
│   ├── lead/lead.*                       # 留资表单（新版：Banner+关注方向）
│   ├── school-ranking/                   # 胡润百学Top50排行榜
│   ├── study-abroad/study-abroad.*       # 留学辅导
│   ├── user/user.*                       # 个人中心
│   ├── my-favorites/my-favorites.*       # 我的收藏（新增）
│   ├── consultation-records/             # 咨询记录（新增）
│   ├── guide-list/guide-list.*           # 择校指南列表
│   ├── guide-detail/guide-detail.*       # 择校指南详情
│   └── open-day/open-day.*               # 学校开放日（占位）
```

---

## 四、数据库

### school（学校信息表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT PK | 主键 |
| name | VARCHAR(100) | 学校名称 |
| city | VARCHAR(50) | 城市（默认上海） |
| district | VARCHAR(50) | 行政区 |
| curriculum | VARCHAR(50) | 课程体系 |
| tuition_min / tuition_max | DECIMAL(10,2) | 学费区间(万元) |
| grades | VARCHAR(50) | 学段（小学/初中/高中） |
| description | TEXT | 富文本介绍 |
| logo_url | VARCHAR(255) | 校徽URL (OSS) |
| teaching_mode | VARCHAR(255) | 授课方式 |
| founding_time | VARCHAR(50) | 创办时间 |
| school_type | VARCHAR(50) | 学校性质 |
| address | VARCHAR(255) | 学校地址 |
| student_count | INT | 在校学生人数 |
| campus_area | DECIMAL(10,2) | 占地面积（㎡） |
| teacher_senior_ratio | DECIMAL(5,2) | 中高级职称教师比例 |
| graduation_result | TEXT | 升学情况 |
| boarding_info | VARCHAR(255) | 寄宿项目 |
| class_size | VARCHAR(50) | 平均班级人数 |
| teacher_ratio | VARCHAR(50) | 师生平均比例 |
| is_active | TINYINT(1) | 逻辑删除 |

**数据量**：154+ 所学校（含 37 所新增，IDs 171-207）

### admission_info（招生动态表）— 新增

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT PK | 主键 |
| school_id | INT | 关联学校 |
| school_name | VARCHAR(100) | 学校名称 |
| admission_year | VARCHAR(10) | 招生年份 |
| admission_event_info | TEXT | 招生进度/活动信息 |
| admission_time | VARCHAR(100) | 招生时间 |
| educational_stages | VARCHAR(100) | 开设学段 |
| tuition_range | VARCHAR(100) | 学费区间 |
| enrollment_count | VARCHAR(50) | 招生人数 |
| curriculum | VARCHAR(100) | 课程体系 |
| target_students | VARCHAR(255) | 招生对象 |
| admission_rules | TEXT | 录取要求 |
| exam_content | TEXT | 入学测试详情 |
| application_flow | TEXT | 报名与申请流程 |
| graduation_result | TEXT | 升学情况 |
| is_active | TINYINT(1) | 是否显示 |

> 后端通过 `buildStructured()` 方法将实体转换为结构化 sections 数组返回前端。

### platform_config（平台配置表）— 新增

KV 键值对配置表，支持灵活扩展：

| 键名 | 说明 |
|------|------|
| finder_user_name | 视频号用户名 |
| live_notice_id | 直播预约 ID |
| live_title | 直播标题 |
| guide_article_url | 择校指南文章链接 |
| 等... | 可动态扩展 |

### channels_video（视频号视频列表）— 新增

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT PK | 主键 |
| feed_id | VARCHAR(100) | 视频 feedId |
| title | VARCHAR(200) | 视频标题 |
| cover | VARCHAR(500) | 封面图 OSS 地址 |
| sort | INT | 排序 |
| is_active | TINYINT(1) | 是否显示 |

### school_image（学校图片表）— 新增

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT PK | 主键 |
| school_id | INT | 关联学校 |
| image_url | VARCHAR(500) | 图片 OSS 地址 |
| type | VARCHAR(50) | 图片类型（轮播图/卡片等） |
| sort | INT | 排序 |

### guide_article（择校指南文章）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT PK | 主键 |
| category | VARCHAR(50) | 一级分类 |
| path_name | VARCHAR(50) | 二级路径 |
| title | VARCHAR(200) | 标题 |
| content | TEXT | 正文 |
| summary | VARCHAR(500) | 摘要 |
| sort_order | INT | 排序 |

**数据量**：69 篇文章，覆盖 4×4=16 个分类路径

### 其他表
- **user** — openid, phone, nickname, avatar
- **customer_lead** — parent_name, child_grade, target_school, parent_phone, concern, status
- **user_favorite** — user_id, school_id, user_nickname, school_name（冗余加速查询）

---

## 五、核心API

### 公开接口（无需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/wx-login | 微信登录 |
| GET | /api/home/overview | 首页聚合数据 |
| GET | /api/home/district-stats | 热门地区统计 |
| GET | /api/schools/list | 学校分页搜索（支持 tuitionMin/tuitionMax 学费筛选） |
| GET | /api/schools/{id} | 学校详情 |
| GET | /api/schools/{id}/admission | 招生动态（结构化 sections 数组） |
| POST | /api/schools/compare | 学校对比 |
| POST | /api/assessment/match | 智能测评匹配 |
| GET | /api/guide/paths | 择校指南路径列表 |
| GET | /api/guide/articles | 择校指南文章列表 |
| GET | /api/guide/article/{id} | 文章详情 |
| GET | /api/config/platform | 平台配置（视频号/直播信息） |
| GET | /api/config/channels-videos | 视频号视频列表 |

### 需JWT认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/decrypt-phone | 微信手机号解密绑定 |
| GET/PUT | /api/user/profile | 用户资料 |
| GET | /api/user/lead-history | 咨询历史 |
| POST | /api/favorite/toggle | 收藏切换 |
| GET | /api/favorite/list | 收藏列表（三表 JOIN 返回 FavoriteVO） |
| POST | /api/lead/submit | 提交留资（含 concern 关注方向） |

### 后台管理接口（`/api/admin/**`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/oss/upload | OSS 文件上传 |
| CRUD | /api/admin/guides | 择校指南管理 |
| CRUD | /api/admin/schools | 学校信息管理 |

---

## 六、页面功能矩阵

### 首页 (home)
- 轮播Banner + 搜索栏 + 统计数据
- 学校排行榜 / 1V1择校规划 入口
- 横向自动轮播热门学校卡片（4张/次）
- 热门地区 2×4 宫格（8区实时统计）
- 择校服务 6入口（测评/学校库/收藏/留资/开放日/资料）
- 择校指南 4步骤（升学规划→择校探校→递交申请→录取准备）
- Banner广告轮播 + 招生动态5条
- 品牌页脚

### 学校库 (school)
- 搜索框（300ms防抖）
- 区域/学段双筛选 + **学费区间筛选（新增）**
- 分页学校列表 → 点击进详情

### 学校详情 (school-detail)
- 顶部轮播图（350rpx）
- 基础信息卡片（名称/标签/学费/位置）
- 招生动态提示条 → 点击跳 `admission-detail`
- 4 Tab：关键信息 | 招生项目 | 学校详情 | 资料
- 关键信息：家长交流群卡片 / 四列核心数据 / 真题引流 / 双列详情
- 底部固定：收藏 + 预约探校 + 定制升学方案

### 招生动态详情 (admission-detail) — 新增
- 结构化展示：招生进度、入学测试、申请流程、升学情况等
- 后端返回 sections 数组，前端按序渲染

### 留学辅导 (study-abroad)
- 黄青色渐变顶部 + Tab分类（课程/语言/留学/竞赛/工具）
- 左图右文课程卡片列表
- 咨询立享优惠CTA

### 择校测评 (assessment)
- 4步答题（区域→课程→学费→学段）
- 智能匹配返回学校列表

### 排行榜 (school-ranking)
- 胡润百学2025全国TOP50
- 前三名🥇🥈🥉特殊样式

### 留资表单 (lead) — 新版重新设计
- 顶部品牌 Banner
- 白色卡片式表单
- 新增「关注方向」多行文本输入
- 自定义导航栏

### 个人中心 (user)
- 微信登录 / 头像昵称 / 手机号绑定
- 我的收藏（跳转 `my-favorites`）
- 咨询记录（跳转 `consultation-records`）

### 我的收藏 (my-favorites) — 新增
- 收藏列表，展示学校名称、Logo、收藏时间

### 咨询记录 (consultation-records) — 新增
- 历史留资记录查询

---

## 七、当前进度

| 模块 | 状态 |
|------|------|
| 首页全模块 | ✅ 完成 |
| 学校库搜索筛选 | ✅ 完成（含学费筛选） |
| 学校详情页 | ✅ 新版已上线 |
| 招生动态详情页 | ✅ 前后端完成 |
| 学校数据（154+） | ✅ 含 37 所新校 + Logo 上传 OSS |
| 择校测评 | ✅ 完成 |
| 排行榜 | ✅ 完成 |
| 收藏系统 | ✅ 三表 JOIN 优化 + 独立页面 |
| 留资表单 | ✅ 新版重新设计（Banner+关注方向） |
| 平台配置系统 | ✅ KV 配置表 + API（视频号/直播配置） |
| OSS 图片上传 | ✅ 阿里云 OSS 集成 |
| 留学辅导 | ✅ Tab+卡片完成，内容待充实 |
| 择校指南后端 | ✅ 完整 CRUD（公开+管理） |
| 择校指南前端 | 🚧 搭档开发中 |
| 学校开放日 | ⏳ 占位页 |
| 择校资料 | ⏳ 按钮就绪，功能待开发 |
| 家长群二维码 | ✅ 弹窗就绪，缺真实二维码 |
| 学校相册/轮播图 | 🚧 school_image 表已建，图片待上传 |
| 学校卡片图上传 | 🚧 30/37 校 Logo 已上传，卡片图待补全 |
| 视频号直播预约 | 🚧 后端 API 就绪，前端待对接 |
| 视频号视频列表 | 🚧 后端 API 就绪，前端待对接 |
| 后台管理认证 | ⚠️ 当前无鉴权，需加固 |
| 云托管部署 | ⏳ CloudBase 环境已备，jar 待部署 |

---

## 八、设计决策备忘

- **同名学校合并**：数据库层面去重，grades字段存"小学,初中"，不在Java内存合并
- **排行榜数据**：前端硬编码，不走后端
- **择校指南菜单数据**：前端硬编码
- **择校指南文章**：建表走数据库，通用接口按 category+path 参数筛选
- **图片存储**：已迁移至阿里云 OSS `lianpeng` bucket，不再依赖本地 static 目录
- **收藏优化**：使用 FavoriteVO + 三表 JOIN 替代 N+1 查询
- **招生动态**：后端 `buildStructured()` 将实体转结构化 sections 数组，前端直接渲染
- **平台配置**：KV 表设计，支持灵活扩展（视频号、直播、链接等）
- **学费筛选**：`tuitionMin`/`tuitionMax` 参数，后端做区间交集匹配
- **JWT白名单**：`/api/auth/**`, `/api/schools/**`, `/api/home/**`, `/api/assessment/**`, `/api/guide/**`（公开文章）, `/api/config/**`
- **数据库 IP**：`192.168.3.5:3306`（之前为 `192.168.1.165`）
- **前端 baseUrl**：配置在 `app.js` globalData，指向后端实际 IP
- **云托管**：使用新企业微信账号的 CloudBase 环境，数据库 `cloud1-d6g28bq6c04919547`

---

## 九、已知问题

| 问题 | 状态 |
|------|------|
| TabBar 高亮偶尔不切换 | ⚠️ 微信框架时序问题，暂未解决 |
| 后台管理接口无鉴权 | ⚠️ 需添加管理员认证 |
| 学校卡片图缺 7 所 | ⏳ 等待素材补全后上传 |
| 后端启动偶发 classpath 过期 | 💡 `mvn clean compile` 后重启即可 |
