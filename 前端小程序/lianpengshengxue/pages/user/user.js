const { request } = require('../../utils/request')
const { upload } = require('../../utils/upload')
const app = getApp()

Page({
  data: {
    loggedIn: false,
    userInfo: { nickname: '', avatar: '', phone: '' },
    favorites: [],
    leads: [],
    activePanel: '',
    showConsultImage: false,
    statusBarHeight: 20,
    profileTop: 112
  },

  onLoad() {
    this.setProfileSafeTop()
  },

  setProfileSafeTop() {
    try {
      const windowInfo = wx.getWindowInfo
        ? wx.getWindowInfo()
        : wx.getSystemInfoSync()
      const menuRect = wx.getMenuButtonBoundingClientRect()
      const statusBarHeight = windowInfo.statusBarHeight || 20
      const profileTop = Math.max(menuRect.bottom + 28, statusBarHeight + 72)
      this.setData({ statusBarHeight, profileTop })
    } catch (e) {}
  },

  onShow() {
    app.globalData.selectedTab = 3
    const tabBar = this.getTabBar && this.getTabBar()
    if (tabBar) tabBar.setData({ selected: 3 })
    wx.nextTick(() => {
      const currentTabBar = this.getTabBar && this.getTabBar()
      if (currentTabBar) currentTabBar.setData({ selected: 3 })
    })
    const token = wx.getStorageSync('token')
    this.setData({ loggedIn: !!token })
    if (token) {
      this.loadProfile()
    }
  },

  async handleLogin() {
    wx.showLoading({ title: '登录中...' })
    try {
      // 仅在用户主动点击后建立基础账号，不索取手机号、头像或昵称。
      await app.login()
      this.setData({ loggedIn: true })
      await this.loadProfile()
      wx.hideLoading()
      wx.showToast({ title: '登录成功', icon: 'success' })
    } catch (e) {
      wx.hideLoading()
      const detail = e && (e.errMsg || e.message) ? String(e.errMsg || e.message) : ''
      console.error('用户主动登录失败：', e)
      wx.showToast({ title: detail.indexOf('Failed to fetch') >= 0 ? '云服务连接失败' : '登录失败，请重试', icon: 'none' })
    }
  },

  handleLogout() {
    wx.removeStorageSync('token')
    app.globalData.token = ''
    this.setData({ loggedIn: false, userInfo: { nickname: '', avatar: '', phone: '' }, favorites: [], leads: [], activePanel: '' })
    wx.reLaunch({ url: '/pages/home/home' })
  },

  async loadProfile() {
    try {
      const data = await request('/api/user/profile', 'GET', {}, true)
      this.setData({ userInfo: { nickname: data.nickname || '', avatar: data.avatar || '', phone: data.phone || '' } })
    } catch (e) {}
  },

  async onChooseAvatar(e) {
    const avatarUrl = e.detail.avatarUrl
    if (!avatarUrl) return
    wx.showLoading({ title: '上传中...' })
    try {
      // 临时路径需先上传 OSS，存 https 持久地址
      const ossUrl = await upload(avatarUrl, 'avatar')
      this.setData({ 'userInfo.avatar': ossUrl })
      await this.saveProfile()
    } catch (err) {
      wx.showToast({ title: '头像上传失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  async onBindPhone(e) {
    const code = e.detail && e.detail.code
    if (!code) {
      wx.showToast({ title: '已取消手机号授权', icon: 'none' })
      return
    }
    wx.showLoading({ title: '绑定中...' })
    try {
      await request('/api/user/phone', 'POST', { code }, true)
      await this.loadProfile()
      wx.showToast({ title: '手机号已绑定', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: '绑定失败，请稍后重试', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  onNicknameChange(e) {
    const nickname = e.detail.value
    this.setData({ 'userInfo.nickname': nickname })
    this.saveProfile()
  },

  async saveProfile() {
    try {
      await request('/api/user/profile', 'PUT', {
        nickname: this.data.userInfo.nickname,
        avatarUrl: this.data.userInfo.avatar
      }, true)
    } catch (e) {}
  },

  async loadFavorites() {
    try {
      const data = await request('/api/favorite/list', 'GET', {}, true)
      this.setData({ favorites: data })
    } catch (e) {
      if (e.code === 401) this.setData({ loggedIn: false })
    }
  },

  async loadLeads() {
    try {
      const data = await request('/api/user/lead-history', 'GET', {}, true)
      this.setData({ leads: data })
    } catch (e) {
      if (e.code === 401) this.setData({ loggedIn: false })
    }
  },

  onMenuTap(e) {
    const action = e.currentTarget.dataset.action
    if (action === 'records') {
      wx.navigateTo({ url: '/pages/consultation-records/consultation-records' })
      return
    }
    if (action === 'favorites') {
      wx.navigateTo({ url: '/pages/my-favorites/my-favorites' })
      return
    }
    if (action === 'about') {
      wx.navigateTo({ url: '/pages/webview/webview' })
      return
    }
    if (action === 'agreement-user') {
      wx.navigateTo({ url: '/pages/agreement/agreement?type=user' })
      return
    }
    if (action === 'agreement-privacy') {
      wx.navigateTo({ url: '/pages/agreement/agreement?type=privacy' })
      return
    }
    if (action === 'schools' || action === 'settle') {
      this.setData({ showConsultImage: true })
      return
    }
  },

  closeConsultImage() {
    this.setData({ showConsultImage: false })
  },

  openBeianMps() { wx.setStorageSync('webviewTarget', 'https://beian.mps.gov.cn/#/query/webSearch?code=31010402337112'); wx.navigateTo({ url: '/pages/webview/webview' }) },
  openBeianMiit() { wx.setStorageSync('webviewTarget', 'https://beian.miit.gov.cn/'); wx.navigateTo({ url: '/pages/webview/webview' }) },

  onTapFav(e) {
    wx.navigateTo({ url: '/pages/school-detail/school-detail?id=' + e.currentTarget.dataset.id + '&schoolSource=international' })
  }
})
