const DEFAULT_SHARE_TITLE = '联朋升学｜懂学校，更懂升学'
const DEFAULT_SHARE_PATH = '/pages/home/home'
// 可选：上传 5:4 分享封面到 OSS 后填入 HTTPS 地址；留空时由微信自动生成封面。
const DEFAULT_SHARE_IMAGE_URL = ''

function getQueryFromPath(path) {
  const queryIndex = String(path || '').indexOf('?')
  return queryIndex >= 0 ? String(path).slice(queryIndex + 1) : ''
}

function enableShareMenu() {
  if (typeof wx.showShareMenu !== 'function') return
  wx.showShareMenu({
    withShareTicket: true,
    menus: ['shareAppMessage', 'shareTimeline']
  })
}

/**
 * 为所有页面统一开启“转发给好友”和“分享到朋友圈”。
 * 页面自己实现的 onShareAppMessage / onShareTimeline 优先级更高。
 */
function installGlobalShare() {
  if (typeof Page !== 'function' || Page.__globalShareInstalled) return

  const originalPage = Page
  const pageWithShare = function (options) {
    const pageOptions = options || {}
    const originalOnLoad = pageOptions.onLoad
    const customShareAppMessage = pageOptions.onShareAppMessage

    pageOptions.onLoad = function () {
      enableShareMenu()
      if (typeof originalOnLoad === 'function') {
        return originalOnLoad.apply(this, arguments)
      }
    }

    if (typeof customShareAppMessage !== 'function') {
      pageOptions.onShareAppMessage = function () {
        const result = {
          title: DEFAULT_SHARE_TITLE,
          path: DEFAULT_SHARE_PATH
        }
        if (DEFAULT_SHARE_IMAGE_URL) result.imageUrl = DEFAULT_SHARE_IMAGE_URL
        return result
      }
    }

    if (typeof pageOptions.onShareTimeline !== 'function') {
      pageOptions.onShareTimeline = function () {
        const appMessage = typeof customShareAppMessage === 'function'
          ? customShareAppMessage.call(this, { from: 'timeline' })
          : null
        const result = {
          title: (appMessage && appMessage.title) || DEFAULT_SHARE_TITLE,
          query: getQueryFromPath(appMessage && appMessage.path)
        }
        const imageUrl = (appMessage && appMessage.imageUrl) || DEFAULT_SHARE_IMAGE_URL
        if (imageUrl) result.imageUrl = imageUrl
        return result
      }
    }

    return originalPage(pageOptions)
  }

  pageWithShare.__globalShareInstalled = true
  Page = pageWithShare
}

module.exports = {
  DEFAULT_SHARE_IMAGE_URL,
  DEFAULT_SHARE_PATH,
  DEFAULT_SHARE_TITLE,
  enableShareMenu,
  installGlobalShare
}
