Page({
  data: { src: 'https://lianpeng.vip' },
  onLoad() {
    const src = wx.getStorageSync('webviewTarget')
    if (src) {
      wx.removeStorageSync('webviewTarget')
      this.setData({ src })
    }
  }
})
