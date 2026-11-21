let checkingPromise = null

/** 登录并检查资料。返回 true 表示可以进入业务页面。 */
function ensurePhoneAuthorized() {
  if (checkingPromise) return checkingPromise

  checkingPromise = (async () => {
    const app = getApp()
    const { request } = require('./request')

    try {
      if (!wx.getStorageSync('token')) await app.login()

      let profile
      try {
        profile = await request('/api/user/profile', 'GET', {}, true)
      } catch (error) {
        // request 遇到 401 会清除 token。重新获取登录 code 和 JWT 后只重试一次。
        if (wx.getStorageSync('token')) throw error
        await app.login()
        profile = await request('/api/user/profile', 'GET', {}, true)
      }

      if (!profile || !profile.phone) {
        const pages = getCurrentPages()
        const current = pages[pages.length - 1]
        if (current && typeof current.setData === 'function') current.setData({ phoneAuthRequired: true })
        return false
      }
      const pages = getCurrentPages()
      const current = pages[pages.length - 1]
      if (current && typeof current.setData === 'function') current.setData({ phoneAuthRequired: false })
      return true
    } catch (error) {
      console.error('手机号门禁检查失败：', error)
      const pages = getCurrentPages()
      const current = pages[pages.length - 1]
      if (current && typeof current.setData === 'function') current.setData({ phoneAuthRequired: true })
      return false
    }
  })().finally(() => { checkingPromise = null })

  return checkingPromise
}

/** 在业务 Page 的 onLoad/onShow 前统一执行门禁，防止分享路径和页面栈绕过。 */
function installAuthGate() {
  if (typeof Page !== 'function' || Page.__phoneAuthGateInstalled) return

  const originalPage = Page
  const pageWithAuthGate = function (options) {
    const pageOptions = options || {}
    const originalOnLoad = pageOptions.onLoad
    const originalOnShow = pageOptions.onShow

    pageOptions.onLoad = function () {
      const args = arguments
      if (this.route === 'pages/phone-auth/phone-auth') return originalOnLoad && originalOnLoad.apply(this, args)
      ensurePhoneAuthorized().then(allowed => {
        if (typeof originalOnLoad === 'function') originalOnLoad.apply(this, args)
      })
    }

    pageOptions.onShow = function () {
      const args = arguments
      if (this.route === 'pages/phone-auth/phone-auth') return originalOnShow && originalOnShow.apply(this, args)
      ensurePhoneAuthorized().then(allowed => {
        if (typeof originalOnShow === 'function') originalOnShow.apply(this, args)
      })
    }

    return originalPage(pageOptions)
  }

  pageWithAuthGate.__phoneAuthGateInstalled = true
  Page = pageWithAuthGate
}

module.exports = { ensurePhoneAuthorized, installAuthGate }

// 功能按钮统一门禁：只在用户发起页面跳转时触发，不在小程序启动时自动登录。
function installNavigationAuthGate() {
  if (wx.__navigationAuthGateInstalled) return
  wx.__navigationAuthGateInstalled = true
  const { request } = require('./request')
  const exempt = ['/pages/home/home', '/pages/user/user', '/pages/agreement/agreement', '/pages/phone-auth/phone-auth']
  const needsAuth = url => {
    const path = String(url || '').split('?')[0]
    return path && exempt.indexOf(path) < 0
  }
  const authorizeThen = async (url, proceed) => {
    if (!needsAuth(url)) return proceed()
    try {
      let token = wx.getStorageSync('token')
      if (!token) {
        const modal = await new Promise(resolve => wx.showModal({
          title: '登录后继续',
          content: '使用该功能前需要微信登录，用于保存您的使用记录。',
          confirmText: '微信登录',
          cancelText: '暂不登录',
          success: resolve
        }))
        if (!modal.confirm) return
        await withTimeout(getApp().login(), 12000, '登录请求超时')
        token = wx.getStorageSync('token')
      }
      const profile = await withTimeout(request('/api/user/profile', 'GET', {}, true), 10000, '账号查询超时')
      if (!profile || !profile.phone) {
        wx.setStorageSync('authRedirectUrl', url)
        wx.navigateTo({ url: '/pages/phone-auth/phone-auth' })
        return
      }
      proceed()
    } catch (error) {
      console.error('功能授权门禁失败：', error)
      wx.showToast({ title: '登录失败，请稍后重试', icon: 'none' })
    }
  }
  ;['navigateTo', 'redirectTo', 'reLaunch', 'switchTab'].forEach(name => {
    const original = wx[name]
    if (typeof original !== 'function') return
    wx[name] = options => {
      const opts = options || {}
      const url = opts.url || ''
      authorizeThen(url, () => original.call(wx, opts))
    }
  })
}

module.exports.installNavigationAuthGate = installNavigationAuthGate

function withTimeout(promise, ms, message) {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error(message)), ms))
  ])
}
