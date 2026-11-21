# 收藏功能优化 — 后端改动 & 前端适配文档

> 日期：2026-07-29  
> 目标：将 `/api/favorite/list` 从 N+1 次查询优化为 1 次三表 JOIN，补全用户昵称和头像信息

---

## 一、后端改动清单

### 1. 新建 `src/main/java/com/example/lpsx/vo/FavoriteVO.java`

| 字段 | 类型 | 来源 |
|------|------|------|
| `favoriteId` | Integer | `user_favorite.id` |
| `userId` | Integer | `user.id` |
| `userNickname` | String | `user.nickname` |
| `userAvatar` | String | `user.avatar` |
| `schoolId` | Integer | `school.id` |
| `schoolName` | String | `school.name` |
| `schoolLogo` | String | `school.logo_url` |
| `district` | String | `school.district` |
| `curriculum` | String | `school.curriculum` |
| `grades` | String | `school.grades` |
| `createTime` | LocalDateTime | `user_favorite.create_time` |

### 2. 修改 `mapper/UserFavoriteMapper.java`

新增三表 JOIN 查询：

```java
@Select("SELECT f.id AS favoriteId, u.id AS userId, u.nickname AS userNickname, " +
        "u.avatar AS userAvatar, s.id AS schoolId, s.name AS schoolName, " +
        "s.logo_url AS schoolLogo, s.district, s.curriculum, s.grades, " +
        "f.create_time AS createTime " +
        "FROM user_favorite f " +
        "JOIN user u ON f.user_id = u.id " +
        "JOIN school s ON f.school_id = s.id " +
        "WHERE f.user_id = #{userId} " +
        "ORDER BY f.create_time DESC")
List<FavoriteVO> selectFavoriteList(@Param("userId") Integer userId);
```

### 3. 修改 `service/FavoriteService.java`

```diff
- List<Map<String, Object>> list(Integer userId);
+ List<FavoriteVO> list(Integer userId);
```

### 4. 修改 `service/impl/FavoriteServiceImpl.java`

- **删除**：`SchoolMapper` 注入、N+1 逐条查 school 的 for 循环、Map 手动组装
- **替换为**：`favoriteMapper.selectFavoriteList(userId)` 一行
- `toggle()` 方法不变

### 5. 修改 `controller/FavoriteController.java`

```diff
- public Result<List<Map<String, Object>>> list(...)
+ public Result<List<FavoriteVO>> list(...)
```

---

## 二、API 响应变化

### Before

```json
[{
  "favoriteId": 1, "schoolId": 3,
  "schoolName": "上海XX学校", "logoUrl": "https://...",
  "district": "浦东新区", "curriculum": "IB",
  "grades": "小学,初中", "createTime": "2026-07-29T10:00:00"
}]
```

### After

```json
[{
  "favoriteId": 1, "userId": 5,
  "userNickname": "小明妈妈", "userAvatar": "https://...",
  "schoolId": 3, "schoolName": "上海XX学校",
  "schoolLogo": "https://...", "district": "浦东新区",
  "curriculum": "IB", "grades": "小学,初中",
  "createTime": "2026-07-29T10:00:00"
}]
```

**变化**：新增 `userId`/`userNickname`/`userAvatar`；`logoUrl` 更名为 `schoolLogo`。

---

## 三、性能对比

| | 原方案 | 新方案 |
|------|------|------|
| SQL 次数 | 1 + N（逐条查 school） | **1 次** |
| 50 条收藏 | ~51 次 DB 往返 | 1 次 DB 往返 |
| 类型安全 | `Map<String, Object>` | `FavoriteVO` 强类型 |

---

## 四、前端需要配合的改动

> 文件：`pages/home/home.js`

### 改动 1：`onTabChange` 方法

```javascript
// 当前：
onTabChange(e) {
  const activeTab = Number(e.currentTarget.dataset.index)
  this.setData({ activeTab })
},

// 改为：
onTabChange(e) {
  const activeTab = Number(e.currentTarget.dataset.index)
  this.setData({ activeTab })
  if (activeTab === 2) {
    this.loadFavorites()
  } else {
    this.setData({ displaySchools: this.data.hotSchools })
  }
},
```

### 改动 2：新增 `loadFavorites` 方法

```javascript
async loadFavorites() {
  try {
    const list = await request('/api/favorite/list')
    const schools = (list || []).map(item => ({
      id: item.schoolId,
      name: item.schoolName,
      logoUrl: item.schoolLogo,
      district: item.district,
      curriculum: item.curriculum,
      grades: item.grades
    }))
    this.setData({
      displaySchools: schools,
      displayMultipleItems: Math.min(schools.length, 4) || 1
    })
  } catch (e) {}
},
```

### 改动 3：`loadOverview` 中缓存 `hotSchools`

在 `setData` 调用后加一行：

```javascript
this.data.hotSchools = hotSchools
```

---

## 五、字段映射速查

| 前端模板字段 | API 返回字段 |
|-------------|-------------|
| `item.id` | `schoolId` |
| `item.name` | `schoolName` |
| `item.logoUrl` | `schoolLogo` |
| `item.district` | `district` |
| `item.curriculum` | `curriculum` |
| —（新增可用） | `userNickname` / `userAvatar` / `userId` |

---

## 六、不变的文件

- `pages/home/home.wxml` / `home.wxss` — JS 层已做字段映射，模板无需改
- `pages/school-detail/school-detail.js` — `/api/favorite/toggle` 接口不变
- `app.js` — 全局状态不变
