Page({
  data: {
    navTop: 20,
    navHeight: 44,
    pageReady: false
  },

  onLoad() {
    try {
      const menu = wx.getMenuButtonBoundingClientRect()
      const windowInfo = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
      const statusBarHeight = windowInfo.statusBarHeight || 20
      this.setData({
        navTop: statusBarHeight,
        navHeight: menu && menu.height ? (menu.top - statusBarHeight) * 2 + menu.height : 44
      })
    } catch (e) {}

    this.enterTimer = setTimeout(() => {
      this.setData({ pageReady: true })
    }, 30)
  },

  onUnload() {
    if (this.enterTimer) clearTimeout(this.enterTimer)
  },

  goBack() {
    wx.navigateBack({ delta: 1 })
  },

  openWebsite() {
    wx.navigateTo({ url: '/pages/webview/webview' })
  }
})
