# 政策 PDF 附件下载预览：前端 Codex 交接文档

更新时间：2026-09-09  
适用范围：前端同事电脑上的最新微信小程序源码  
后端状态：本机后端已实现并通过接口实测；云端尚未部署

## 一、这次要解决什么

以前政策详情把 PDF 写成正文里的超链接和裸网址，前端只能把网址显示出来。现在后端已把附件改为结构化数组，前端应将其渲染成“查看/下载附件”按钮，用户点击后下载并打开 PDF。

不要修改本机旧前端作为最终版本。请在前端同事电脑的最新代码中接入。

## 二、接口

详情接口路径没有变化：

```http
GET /api/guide/article/{id}
```

政策文章会新增 `attachments`；普通文章也会返回该字段，但值为 `[]`。旧字段保持不变。

实测文章：

```text
GET /api/guide/article/71
```

核心响应示例：

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": 71,
    "title": "2026杨浦区一年级新生招生范围",
    "content": "<p><strong>政策摘要：</strong>...</p>",
    "attachments": [
      {
        "id": 16,
        "fileName": "杨浦小学对口地段.pdf",
        "fileUrl": "https://lianpeng.oss-cn-shanghai.aliyuncs.com/education-policy/4/278_杨浦小学对口地段.pdf",
        "fileExtension": "pdf",
        "mediaType": "pdf",
        "sortOrder": 1
      }
    ]
  }
}
```

字段说明：

| 字段 | 类型 | 用途 |
|---|---|---|
| `attachments` | array | 附件列表；没有附件时为 `[]` |
| `fileName` | string | 按钮文案或附件标题 |
| `fileUrl` | string | OSS 下载地址 |
| `fileExtension` | string | 传给 `wx.openDocument` 的文件类型 |
| `mediaType` | string | 当前为 `pdf`，供后续扩展 |
| `sortOrder` | number | 多附件显示顺序，后端已排序 |

说明：对于已经结构化的政策附件，后端响应中的 `content` 会自动移除历史“政策附件 + 裸网址”片段；前端不需要再用正则清洗正文。

## 三、前端接入示例

WXML：

```xml
<view class="policy-attachments" wx:if="{{article.attachments.length}}">
  <view class="attachment-title">政策附件</view>
  <button
    class="attachment-button"
    wx:for="{{article.attachments}}"
    wx:key="id"
    data-url="{{item.fileUrl}}"
    data-name="{{item.fileName}}"
    data-extension="{{item.fileExtension}}"
    bindtap="openPolicyAttachment"
  >
    查看 {{item.fileName}}
  </button>
</view>
```

页面 JS：

```js
openPolicyAttachment(e) {
  const { url, name, extension } = e.currentTarget.dataset

  if (!url) {
    wx.showToast({ title: '附件地址无效', icon: 'none' })
    return
  }

  wx.showLoading({ title: '附件加载中', mask: true })

  wx.downloadFile({
    url,
    success: (res) => {
      if (res.statusCode !== 200) {
        wx.showToast({ title: '附件下载失败', icon: 'none' })
        return
      }

      wx.openDocument({
        filePath: res.tempFilePath,
        fileType: (extension || 'pdf').toLowerCase(),
        showMenu: true,
        fail: () => {
          wx.showToast({ title: '附件打开失败', icon: 'none' })
        }
      })
    },
    fail: () => {
      wx.showToast({ title: '附件下载失败，请稍后重试', icon: 'none' })
    },
    complete: () => {
      wx.hideLoading()
    }
  })
}
```

如果接口初始化阶段可能没有 `attachments`，建议在数据映射时兜底：

```js
const article = {
  ...res.data,
  attachments: Array.isArray(res.data.attachments)
    ? res.data.attachments
    : []
}
```

## 四、微信后台配置

把下面域名加入小程序的 `downloadFile` 合法域名：

```text
https://lianpeng.oss-cn-shanghai.aliyuncs.com
```

开发者工具里的“不校验合法域名”只能用于临时调试，不能代替微信公众平台配置。最终必须用真机体验版验证。

## 五、不要这样做

- 不要继续把 `fileUrl` 当正文文本直接显示。
- 不要在页面加载时自动下载，用户点击附件时再下载。
- 不要自行拼 OSS 地址，始终使用后端返回的 `fileUrl`。
- 不要解析或正则清洗 `content` 来寻找 PDF。
- 不要因为本机旧前端能跑，就覆盖前端同事电脑上的最新代码。

## 六、验收清单

1. 请求文章 71，页面正文不再出现裸 OSS 网址。
2. 页面出现“查看 杨浦小学对口地段.pdf”按钮。
3. 真机点击按钮，显示加载状态，PDF 能正常打开。
4. 打开一个普通、无附件文章，页面正常且不显示附件区。
5. 断网或使用错误地址时有失败提示，不白屏、不持续 loading。
6. 确认体验版已配置 OSS 下载合法域名。

## 七、后端实现边界

- 本次没有修改数据库结构，也没有重传 PDF。
- 数据库现有 9 条附件记录均已有 OSS URL。
- 接口采用向后兼容的新增字段方式，原详情路径和原字段不变。
- 当前只在本机后端生效；正式小程序使用云端接口前，需要随下一次后端发布部署。

