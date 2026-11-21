# 初中学校接口交接文档

后端已提供公开接口（无需登录），统一返回 `{ code, msg, data }`。`code=200` 表示成功。

## 1. 获取地区

`GET /api/junior-schools/districts`

返回去重、排序后的地区名称数组：

```json
{"code":200,"msg":"成功","data":["黄浦区","徐汇区","长宁区"]}
```

## 2. 按地区获取初中

`GET /api/junior-schools?district=杨浦区`

返回该区全部初中，按学校名称排序。地区为空返回 `400`。每项字段：`id`、`name`、`shortName`、`backgroundImageUrl`、`district`、`tuitionMin`、`tuitionMax`、`boardingInfo`、`schoolType`、`address`、`description`。

`schoolType` 当前为 `公办初中` 或 `民办初中`。

## 3. 获取初中详情

`GET /api/junior-schools/{id}`

例如 `GET /api/junior-schools/704`。不存在返回 `400`，成功时 `data` 为单个学校对象。

## 前端交互建议

页面加载先请求 `/districts` 填充地区选择器；地区改变后请求列表接口填充初中选择器；用户选择学校后使用返回的 `id` 请求详情接口并展示所有字段。不要跨表复用其他学校表的 ID。
