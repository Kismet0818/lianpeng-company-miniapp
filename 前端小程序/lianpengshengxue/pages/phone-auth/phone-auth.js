const { request } = require('../../utils/request')
const app = getApp()

Page({
  data: {
    checking: true,
    phoneBinding: false,
    error: ''
  },

  onLoad() {
    this.checkProfile()
  },

  continueToPendingFeature() {
    const redirectUrl = wx.getStorageSync('authRedirectUrl')
    wx.removeStorageSync('authRedirectUrl')
    if (redirectUrl) {
      const path = String(redirectUrl).split('?')[0]
      if (path === '/pages/home/home' || path === '/pages/school/school' || path === '/pages/study-abroad/study-abroad' || path === '/pages/user/user') {
        wx.switchTab({ url: redirectUrl })
      } else {
        wx.navigateTo({ url: redirectUrl })
      }
      return
    }
    wx.switchTab({ url: '/pages/home/home' })
  },

  async checkProfile() {
    this.setData({ checking: true, error: '' })
    try {
      if (!wx.getStorageSync('token')) await app.login()
      let profile
      try {
        profile = await request('/api/user/profile', 'GET', {}, true)
      } catch (error) {
        if (wx.getStorageSync('token')) throw error
        await app.login()
        profile = await request('/api/user/profile', 'GET', {}, true)
      }
      if (profile && profile.phone) {
        wx.reLaunch({ url: '/pages/home/home' })
        return
      }
      this.setData({ checking: false })
    } catch (error) {
      console.error('登录或资料查询失败：', error)
      this.setData({ checking: false, error: '登录失败，请检查网络后重试' })
    }
  },

  async onGetPhoneNumber(e) {
    if (this.data.phoneBinding) return
    const code = e.detail && e.detail.code
    if (!code) {
      wx.showToast({ title: '需要授权手机号后才能继续使用', icon: 'none' })
      return
    }

    if (!wx.getStorageSync('token')) {
      wx.showToast({ title: '登录已失效，正在重新登录', icon: 'none' })
      await this.checkProfile()
      return
    }

    this.setData({ phoneBinding: true, error: '' })
    try {
      await request('/api/user/phone', 'POST', { code }, true)
      const profile = await request('/api/user/profile', 'GET', {}, true)
      if (!profile || !profile.phone) throw new Error('手机号绑定结果未生效')
      wx.showToast({ title: '授权成功', icon: 'success' })
      setTimeout(() => this.continueToPendingFeature(), 500)
    } catch (error) {
      console.error('手机号绑定失败：', error)
      if (!wx.getStorageSync('token')) {
        this.setData({ error: '登录已失效，请重试' })
      }
    } finally {
      this.setData({ phoneBinding: false })
    }
  }
})
