# 区重点学校接口交接报告（修正版）

> 日期：2026-09-03  
> 本报告为最新版本，区重点范围包含 ID 75，请以本版本为准。

## 1. 完成内容

后端已新增按区县查询区重点学校的公开接口。前端选择区县并点击“区重点”后，可调用该接口展示对应学校，再通过返回的学校 ID 进入现有学校详情页。

## 2. 接口定义

- 方法：`GET`
- 路径：`/api/score-query/schools/district-key`
- 鉴权：无需登录
- 必填参数：`district`，例如 `杨浦区`、`浦东新区`

本地联调示例：

```text
http://192.168.3.5:8080/api/score-query/schools/district-key?district=浦东新区
```

## 3. 最新筛选口径

区重点学校从 ID 75 开始，包含 ID 75：

```sql
id >= 75
AND short_name IS NOT NULL
AND TRIM(short_name) <> ''
```

补充说明：

- ID 75 为“上海交通大学附属中学浦东实验高中”。
- ID 75 的简称为“交附浦实”，所属“浦东新区”。
- 本地数据库按最新口径共有 92 所区重点学校。
- `school_type` 表示公办或民办，不是学校重点级别，前端不要用它判断区重点。

## 4. 返回格式

```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 75,
      "name": "上海交通大学附属中学浦东实验高中",
      "shortName": "交附浦实",
      "logo": "https://lianpeng.oss-cn-shanghai.aliyuncs.com/score-query/school-logo/75.jpg",
      "district": "浦东新区"
    }
  ]
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| `id` | 学校 ID，用于进入详情页 |
| `name` | 学校完整名称 |
| `shortName` | 学校简称 |
| `logo` | 校徽 URL，个别学校可能为空 |
| `district` | 学校所属区 |

无匹配学校时，`data` 返回空数组 `[]`。

## 5. 前端调用示例

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

点击学校后，将接口返回的 `id` 传给现有详情页：

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

## 6. 前端注意事项

- 点击“区重点”前应先确认用户已选择区县。
- 学校标题可优先显示 `shortName`，完整名称使用 `name`。
- `logo` 继续通过项目现有 `resolveImageUrl` 处理。
- `logo` 为空或加载失败时显示默认校徽。
- 返回空数组时显示“暂无区重点学校”。
- 接口结果已按学校 ID 升序排列。

## 7. 验证结果

- Maven 编译成功。
- 本地接口请求成功，返回 `code = 200`。
- 浦东新区返回 19 所区重点学校。
- 浦东新区结果已包含 ID 75“上海交通大学附属中学浦东实验高中”。
- 全市区重点共 92 所。
- 未修改本地数据库或云数据库。

## 8. 部署状态

接口代码目前已在本地后端完成并验证，尚未发布至腾讯云托管。前端现阶段使用本地地址联调；云端发布后接口相对路径不变。
