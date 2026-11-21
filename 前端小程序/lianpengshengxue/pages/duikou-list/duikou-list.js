const { request } = require('../../utils/request')

Page({
  data: {
    safeTop: 20,
    districts: [],
    selected: '',
    items: [],
    visibleItems: [],
    loading: true,
    error: '',
    mode: 'duikou',
    policyCategories: ['志愿填报小知识', '详细文章'],
    policyCategory: '志愿填报小知识',
    years: []
  },

  onLoad(options) {
    const mode = options && options.mode === 'policy' ? 'policy' : 'duikou'
    this.setData({ mode })
    let safeTop = 20
    try {
      const menu = wx.getMenuButtonBoundingClientRect()
      safeTop = menu && menu.top ? menu.top : 20
    } catch (e) {}
    this.setData({ safeTop })
    this.loadAll()
  },

  async loadPolicies() {
    this.setData({ loading: true, error: '' })
    try {
      const result = await request('/api/admission-content/articles', 'GET', { category: this.data.policyCategory })
      const list = Array.isArray(result) ? result : (result && (result.records || result.list || result.content) || [])
      const items = this.prioritizeItems(list, (a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
      this.setData({ items, visibleItems: items, loading: false })
    } catch (e) {
      this.setData({ loading: false, error: '政策资料加载失败，请检查网络后重试' })
    }
  },

  selectPolicyCategory(e) {
    const category = e.currentTarget.dataset.category
    if (category === this.data.policyCategory) return
    this.setData({ policyCategory: category, items: [], visibleItems: [] }, () => this.loadPolicies())
  },

  goBack() { wx.navigateBack() },

  async loadAll() {
    this.setData({ loading: true, error: '' })
    try {
      const [items, districts] = await Promise.all([
        request('/api/duikou/list'),
        request('/api/duikou/districts')
      ])
      this.setData({
        items: this.prioritizeItems(Array.isArray(items) ? items : [], (a, b) => Number(b.id || 0) - Number(a.id || 0)),
        districts: Array.isArray(districts) ? districts : [],
        loading: false
      }, () => this.applyFilter())
    } catch (e) {
      this.setData({ loading: false, error: '对口资料加载失败，请检查网络后重试' })
    }
  },

  selectDistrict(e) {
    const selected = e.currentTarget.dataset.district || ''
    if (selected === this.data.selected) return
    this.setData({ selected }, () => this.applyFilter())
  },

  applyFilter() {
    const { items, selected } = this.data
    const visibleItems = selected ? items.filter(item => item.district === selected) : items
    this.setData({ visibleItems })
  },

  prioritizeItems(items, fallbackCompare) {
    const priority = ['徐汇区', '闵行区', '浦东新区']
    return items.map((item, index) => {
      const text = [item.district, item.title, item.summary, item.content].filter(Boolean).join('')
      const district = priority.findIndex(name => text.includes(name) || text.includes(name.replace('区', '')))
      return { item, index, priority: district < 0 ? priority.length : district }
    }).sort((a, b) => a.priority - b.priority || fallbackCompare(a.item, b.item) || a.index - b.index).map(entry => entry.item)
  },

  openDetail(e) {
    wx.navigateTo({ url: '/pages/duikou-detail/duikou-detail?id=' + encodeURIComponent(e.currentTarget.dataset.id) })
  }
})
