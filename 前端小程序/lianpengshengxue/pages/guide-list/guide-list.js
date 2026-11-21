const { request } = require('../../utils/request')

Page({
  data: {
    safeTop: 20,
    categories: [
      { label: '推荐', value: '推荐' },
      { label: '升学规划', value: '升学规划' },
      { label: '择校探校', value: '择校探校' },
      { label: '递交申请', value: '递交申请' },
      { label: '录取准备', value: '录取准备' }
    ],
    selectedCategory: '升学规划',
    initialPath: '',
    groups: [],
    loading: true,
    error: '',
    showQr: false
  },

  onLoad(options) {
    const category = decodeURIComponent(options.category || '升学规划')
    const initialPath = decodeURIComponent(options.path || '')
    let safeTop = 20
    try {
      const menu = wx.getMenuButtonBoundingClientRect()
      safeTop = menu && menu.top ? menu.top : 20
    } catch (e) {}
    this.setData({ safeTop, selectedCategory: category, initialPath })
    this.loadCategory()
  },

  goBack() { wx.navigateBack() },
  openParentChat() {
    this.setData({ showQr: true })
  },
  closeQr() { this.setData({ showQr: false }) },

  selectCategory(e) {
    const selectedCategory = e.currentTarget.dataset.category
    if (selectedCategory === this.data.selectedCategory) return
    this.setData({ selectedCategory, initialPath: '' }, () => this.loadCategory())
  },

  async loadCategory() {
    const displayCategory = this.data.selectedCategory
    const queryCategory = displayCategory === '推荐' ? '升学规划' : displayCategory
    this.setData({ loading: true, error: '', groups: [] })
    try {
      const data = await request('/api/guide/paths?category=' + encodeURIComponent(queryCategory))
      let paths = Array.isArray(data) ? data : []
      if (this.data.initialPath && paths.includes(this.data.initialPath)) {
        paths = [this.data.initialPath].concat(paths.filter(path => path !== this.data.initialPath))
      }
      const groups = await Promise.all(paths.map(async pathName => {
        try {
          const articles = await request('/api/guide/articles?category=' + encodeURIComponent(queryCategory) + '&path=' + encodeURIComponent(pathName))
          return { pathName, articles: Array.isArray(articles) ? articles : [] }
        } catch (e) {
          return { pathName, articles: [] }
        }
      }))
      this.setData({ groups, loading: false })
    } catch (e) {
      this.setData({ loading: false, error: '指南内容加载失败，请检查网络后重试' })
    }
  },

  openArticle(e) {
    wx.navigateTo({ url: '/pages/guide-detail/guide-detail?id=' + e.currentTarget.dataset.id })
  }
})
