# 区重点学校列表接口—前端交接文档

> 交接日期：2026-09-03  
> 用途：用户选择区县后，点击“区重点”，展示该区对应的区重点学校。

## 1. 接口信息

- 请求方法：`GET`
- 请求路径：`/api/score-query/schools/district-key`
- 是否需要登录：否，该接口为公开接口
- 请求参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `district` | string | 是 | 学校所属区，例如 `杨浦区`、`浦东新区` |

### 本地联调示例

```text
GET http://192.168.3.5:8080/api/score-query/schools/district-key?district=杨浦区
```

`district` 建议由请求工具自动进行 URL 编码，不要手动拼接未编码的复杂参数。

## 2. 成功返回

```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 76,
      "name": "上海体育大学附属中学",
      "shortName": "上体附中",
      "logo": "https://lianpeng.oss-cn-shanghai.aliyuncs.com/score-query/school-logo/76.jpg",
      "district": "杨浦区"
    }
  ]
}
```

### 学校字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | number | 学校 ID，进入详情页时使用 |
| `name` | string | 学校全称 |
| `shortName` | string | 学校简称 |
| `logo` | string/null | 学校校徽图片 URL，个别学校可能为空 |
| `district` | string | 学校所属区 |

查询成功但该区没有匹配学校时，`data` 返回空数组 `[]`。

## 3. 小程序调用示例

项目已有统一请求工具，页面中建议按以下方式调用：

```javascript
const { request } = require('../../utils/request')

async function loadDistrictKeySchools(district) {
  return request(
    '/api/score-query/schools/district-key',
    'GET',
    { district },
    false
  )
}
```

页面中的使用示例：

```javascript
async onDistrictKeyTap() {
  const district = this.data.selectedDistrict

  if (!district) {
    wx.showToast({ title: '请先选择区县', icon: 'none' })
    return
  }

  wx.showLoading({ title: '加载中' })
  try {
    const schools = await loadDistrictKeySchools(district)
    this.setData({ districtKeySchools: schools || [] })
  } catch (error) {
    console.error('加载区重点学校失败', error)
  } finally {
    wx.hideLoading()
  }
}
```

## 4. 进入学校详情页

用户点击某所学校时，将列表返回的 `id` 传给现有学校详情页。

```javascript
onSchoolTap(e) {
  const { id } = e.currentTarget.dataset
  wx.navigateTo({
    url: `/pages/score-school-detail/score-school-detail?id=${id}`
  })
}
```

详情页继续使用现有接口：

```text
GET /api/score-query/schools/{id}
GET /api/score-query/schools/{id}/images
```

第一个接口返回学校详细信息，第二个接口返回学校风光图。

## 5. 前端展示建议

- 标题优先展示 `shortName`，需要完整名称时使用 `name`。
- 校徽使用 `logo`，建议继续通过项目现有 `resolveImageUrl` 处理。
- `logo` 为空或图片加载失败时，显示默认校徽占位图。
- `data` 为空数组时，显示“暂无区重点学校”。
- 接口结果已按学校 ID 升序排列，前端不必再次排序。

## 6. 后端筛选口径

当前业务数据按以下条件识别区重点：

- `score_query_tool_school.id >= 75`（从 ID 75 这一行开始）；
- `short_name` 非空；
- `district` 与前端传入的区县名称精确匹配。

`score_query_tool_school.school_type` 当前表示公办/民办，不是市重点/区重点/一般高中，前端不要用该字段做学校级别判断。

## 7. 当前状态

- 后端代码已完成并通过 Maven 编译。
- 本地接口已实际请求验证。
- 验证示例：杨浦区返回 9 所区重点学校。
- 本次没有修改本地或云端数据库。
- 截至交接时，新接口尚未发布到云托管；前端先使用本地后端联调，云端发布后无需更改接口相对路径。
