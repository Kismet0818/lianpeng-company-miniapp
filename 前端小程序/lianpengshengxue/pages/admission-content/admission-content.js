const { request } = require('../../utils/request')

Page({
  data: {
    categories: ['全部', '入园', '幼升小', '小升初'],
    category: '全部',
    allArticles: [],
    articles: [],
    total: 0,
    loading: true,
    error: '', navBarHeight: 88, menuTop: 26, menuHeight: 32
  },

  onLoad() { try { const menu = wx.getMenuButtonBoundingClientRect(); this.setData({ navBarHeight: menu.bottom + 8, menuTop: menu.top, menuHeight: menu.height }) } catch (error) {} this.load() },
  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()) },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const result = await request('/api/parent-qa/articles', 'GET', { page: 1, size: 100 })
      const list = Array.isArray(result) ? result : (result && (result.records || result.list || result.content) || [])
      const allArticles = list.slice().sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
      this.setData({ allArticles, loading: false }, () => this.applyCategory(this.data.category))
    } catch (e) {
      this.setData({ loading: false, error: '内容加载失败，请稍后重试' })
    }
  },

  selectCategory(e) {
    const category = String(e.currentTarget.dataset.category || '全部')
    this.setData({ category }, () => this.applyCategory(category))
  },

  applyCategory(category) {
    const keywords = { 入园: ['入园', '幼儿园'], 幼升小: ['幼升小', '小学入户', '人户分离'], 小升初: ['小升初', '初中'] }[category]
    const articles = !keywords ? this.data.allArticles : this.data.allArticles.filter(item => {
      const value = [item.category, item.sourceCategory, item.title, item.summary].filter(Boolean).join('')
      return keywords.some(keyword => value.indexOf(keyword) >= 0)
    })
    this.setData({ articles, total: articles.length })
  },

  openArticle(e) {
    wx.navigateTo({ url: '/pages/admission-content-detail/admission-content-detail?id=' + encodeURIComponent(e.currentTarget.dataset.id) + '&source=parent-qa' })
  },

  goBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack({ delta: 1 })
    else wx.switchTab({ url: '/pages/home/home' })
  }
})
