// 联朋升学
// 在各页面注册前注入统一分享能力，后续新增页面也会自动生效。
require('./utils/share').installGlobalShare()
require('./utils/auth-gate').installNavigationAuthGate()

App({
  globalData: {
    cloudEnv: 'cloud1-d6g28bq6c04919547',
    cloudService: 'lpsx1',
    useCloud: true,                          // true=云托管（真机/上线）；false=本地后端（局域网调试）
    localBaseUrl: 'http://192.168.3.5:8080', // 本地后端地址（useCloud=false 时使用）
    token: '',
    selectedTab: 0,
    startupAdShown: false
  },

  onLaunch() {
    if (!wx.cloud) {
      console.error('当前微信基础库不支持云开发，请升级基础库')
    } else {
      wx.cloud.init({
        env: this.globalData.cloudEnv,
        traceUser: true
      })
    }
    const token = wx.getStorageSync('token')
    if (token) this.globalData.token = token
  },

  /** 微信登录，返回 token；失败重试时必须重新获取一次性 code */
  login(maxRetries = 1) {
    const { request } = require('./utils/request')
    const loginOnce = () => new Promise((resolve, reject) => {
      // code 只能使用一次，所以每次后端登录请求前都重新调用 wx.login。
      wx.login({
        success: (res) => {
          if (!res.code) {
            reject(new Error('wx.login 未返回 code'))
            return
          }
          request('/api/auth/wx-login', 'POST', { code: res.code })
            .then(data => {
              const token = data && data.token
              if (!token || typeof token !== 'string') {
                throw new Error('登录接口未返回有效 token')
              }
              wx.setStorageSync('token', token)
              this.globalData.token = token
              resolve(token)
            })
            .catch(reject)
        },
        fail: reject
      })
    })

    const loginWithFreshCode = async (retriesLeft) => {
      try {
        return await loginOnce()
      } catch (error) {
        if (retriesLeft <= 0) throw error
        // 重新进入 loginOnce，确保重试使用新的 code。
        return loginWithFreshCode(retriesLeft - 1)
      }
    }

    return loginWithFreshCode(Math.max(0, Number(maxRetries) || 0))
  }
})
