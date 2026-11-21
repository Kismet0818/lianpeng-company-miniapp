const { request } = require('../../utils/request')

Page({
  data: {
    favorites: [],
    loading: true
  },

  onShow() {
    this.loadFavorites()
  },

  async loadFavorites() {
    this.setData({ loading: true })
    try {
      const data = await request('/api/favorite/list', 'GET', {}, true)
      const favorites = (Array.isArray(data) ? data : []).map(item => ({
        ...item,
        favoriteKey: String(item.schoolSource || 'international') + ':' + item.schoolId,
        displayLogo: this.resolveImageUrl(item.schoolLogo || item.logoUrl)
      }))
      this.setData({
        favorites,
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  onTapSchool(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    const source = String(e.currentTarget.dataset.source || 'international')
    const page = source === 'international' ? '/pages/school-detail/school-detail' : '/pages/score-school-detail/score-school-detail'
    wx.navigateTo({ url: page + '?id=' + id + '&schoolSource=' + encodeURIComponent(source) })
  },

  resolveImageUrl(url) {
    return require('../../utils/image').resolveImageUrl(url)
  }
})
