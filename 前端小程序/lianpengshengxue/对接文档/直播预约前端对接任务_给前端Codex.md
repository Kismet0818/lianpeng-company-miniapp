# 直播预约前端对接任务（给前端 Codex）

## 1. 任务目标

请在微信小程序中完成“直播预约”板块。业务流程是：运营人员先在公众号/视频号发布直播预告，后台录入对应的直播场次信息，用户在小程序看到场次后点击“预约”，小程序调用微信视频号直播预约能力完成预约。

本功能不是普通网页直播链接跳转，不要新增或依赖 `liveUrl`、`linkUrl` 等直播链接字段。

## 2. 后端现有数据和接口

### 2.1 公开直播列表

`GET /api/live/list`

无需登录，返回 `is_active = 1` 的直播场次。

返回数据字段：

| 字段 | 类型 | 用途 |
|---|---|---|
| `id` | number | 场次 ID |
| `title` | string | 直播标题 |
| `cover` | string | 封面地址，可为空 |
| `liveTime` | string | 直播时间，通常为 `YYYY-MM-DDTHH:mm:ss` |
| `finderUserName` | string | 视频号原始 ID，调用微信接口必需 |
| `channelsName` | string | 视频号展示名称 |
| `channelsAvatar` | string | 视频号头像地址，可为空 |
| `sort` | number | 排序值，越小越靠前 |
| `isActive` | number | 是否展示，`1` 展示 |

兼容处理：前端同时兼容驼峰字段和下划线字段，例如 `liveTime/live_time`、`finderUserName/finder_user_name`。

### 2.2 后台管理接口（仅供后台管理页使用）

这些接口由 `X-Admin-Key` 保护，普通小程序端不要调用：

- `GET /api/admin/live-sessions`
- `POST /api/admin/live-sessions`
- `PUT /api/admin/live-sessions/{id}`
- `DELETE /api/admin/live-sessions/{id}`

前端小程序只需要消费公开的 `/api/live/list`。

## 3. 页面要求

请新增或完善直播页面，例如 `/pages/monthly-live/monthly-live`，至少包含：

1. 近期直播列表。
2. 每张直播卡片展示：封面、标题、日期、时间、视频号名称、视频号头像。
3. 每张未开始的场次显示“预约直播”按钮。
4. 已结束场次不可预约，可显示“已结束”。
5. 没有数据时显示空状态。
6. 接口失败时显示错误状态和“重新加载”按钮。
7. 必须防止重复点击预约按钮。

## 4. 微信预约实现要求

点击预约时必须按以下流程执行：

```text
读取当前卡片的 finderUserName 和 liveTime
        ↓
调用 wx.getChannelsLiveNoticeInfo({ finderUserName })
        ↓
合并返回值中的 noticeId 和 otherInfos
        ↓
按 startTime 与当前卡片 liveTime 匹配到分钟
        ↓
检查 reservable !== false 且 status === 0
        ↓
调用 wx.reserveChannelsLive({ noticeId })
```

示例逻辑：

```js
const notices = []
if (info && info.noticeId) notices.push(info)
if (info && Array.isArray(info.otherInfos)) notices.push(...info.otherInfos)

const target = notices.find(item => matchLiveTime(item.startTime, card.liveTime))
if (!target || target.reservable === false || Number(target.status) !== 0) {
  // 提示：该场直播暂未开放预约
  return
}

wx.reserveChannelsLive({
  noticeId: target.noticeId,
  success() {
    // 提示预约成功
  },
  fail() {
    // 用户取消时不要误报错误；其他失败提示稍后重试
  }
})
```

### 时间匹配规则

- 比较年份、月份、日期、小时、分钟。
- 忽略秒数。
- 同时兼容 `YYYY-MM-DD HH:mm:ss`、`YYYY-MM-DDTHH:mm:ss` 和 10/13 位时间戳。
- 不要只取微信返回的第一条预告；必须优先匹配当前卡片的直播时间。
- `finderUserName` 为空时，不得调用微信预约接口，应提示“该场直播暂未开放预约”。

## 5. 状态和异常提示

请至少处理以下情况：

| 情况 | 前端提示 |
|---|---|
| 微信版本不支持 `wx.getChannelsLiveNoticeInfo` 或 `wx.reserveChannelsLive` | 当前微信版本不支持直播预约，请升级微信后重试 |
| `finderUserName` 缺失 | 该场直播暂未开放预约 |
| 找不到相同时间的微信预告 | 暂未获取到该场直播预告 |
| `reservable === false` 或 `status !== 0` | 该场直播暂未开放预约 |
| 用户主动取消 | 不提示错误，恢复按钮状态 |
| 微信接口失败 | 直播预约未完成，请稍后重试 |
| `/api/live/list` 请求失败 | 直播列表加载失败，请重试 |
| 重复点击 | 按钮进入 loading，接口结束后恢复 |

不要把直播预约失败降级成打开网页，也不要拼接不存在的直播链接。

## 6. 与现有后端的边界

后端已经完成或已有：

- `live_session` 直播场次数据模型。
- 公开接口 `GET /api/live/list`。
- `liveTime`、`finderUserName`、`channelsName`、`channelsAvatar` 等字段。
- 管理端直播场次新增、编辑、删除和隐藏接口。
- 后端对直播时间、视频号原始 ID、视频号名称的基本校验。

前端需要完成：

- 直播列表页面和卡片视觉呈现。
- `/api/live/list` 请求、加载、空状态和错误状态。
- 时间格式化及历史场次判断。
- `wx.getChannelsLiveNoticeInfo` 和 `wx.reserveChannelsLive` 调用。
- 按直播时间匹配微信预告。
- 预约按钮 loading、成功、失败和取消状态。
- 真机验证微信版本兼容性。

## 7. 验收标准

完成后请至少验证：

1. 能正确显示后端返回的多场直播。
2. 多场直播使用相同视频号时，点击不同卡片会匹配不同直播时间。
3. 没有直播链接字段时，页面仍能正常工作。
4. 找不到对应微信预告时不会调用错误的 `noticeId`。
5. 预约成功、预约关闭、用户取消、微信版本不支持等状态提示正确。
6. 已结束场次不可预约。
7. 真机上验证，不要只依赖开发者工具模拟结果。

## 8. 前端交付物（必须提交）

前端完成代码和自测后，必须在项目的 `对接文档/` 目录新增一份 Markdown 文件，文件名建议：

`直播预约前端完成情况与后端对接文档_YYYYMMDD.md`

该文档必须包含以下章节：

### A. 已完成什么

逐项说明：

- 修改或新增了哪些页面和文件。
- 使用了哪些后端接口。
- 直播卡片展示了哪些字段。
- 微信预约调用和时间匹配是否完成。
- 已测试的设备、微信版本和测试结果。
- 已知问题、截图或日志（如有）。

### B. 还需要后端做什么

明确列出后端待办，不要写笼统的“需要联调”。至少说明：

- 是否需要后端新增或修改接口。
- 是否需要数据库字段或数据迁移。
- 是否需要补充线上直播场次数据。
- 是否需要部署云托管新版本。
- 是否需要后端提供特定测试场次、`finderUserName` 或预告时间。
- 每项待办的负责人、优先级和验收方式（如果已知）。

如果确认不需要后端继续修改，也必须明确写出：

`后端代码无需继续修改，但需要部署当前版本并录入真实视频号直播预告数据。`

### C. 联调记录

请记录：

- 联调环境（本地/测试/线上）。
- API 地址和请求时间。
- 使用的直播场次时间和视频号原始 ID（可脱敏）。
- 微信预告是否能按时间匹配。
- `wx.reserveChannelsLive` 返回结果。
- 最终通过/不通过结论。

## 9. 重要提醒

直播预约依赖微信视频号真实发布的直播预告。仅在数据库中新增直播时间和视频号名称，并不能凭空生成微信预约资格；必须有对应视频号、对应开始时间，并且微信预告处于可预约状态。
