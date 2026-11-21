const { request } = require('../../utils/request')
Page({
  data: {
    records: [], loading: false, error: '', page: 0, total: 0, hasMore: true,
    keyword: '', category: '', sectionTitle: '全部资料',
    categories: [
      { value: '', name: '全部资料', note: '浏览资料库', icon: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/miniprogram/material-categories/all-materials.png' },
      { value: '志愿填报工具包', name: '志愿填报', note: '分数线 · 填报参考', icon: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/miniprogram/material-categories/volunteer-form.png' },
      { value: '自招配套资料', name: '自主招生', note: '备考 · 简历模板', icon: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/miniprogram/material-categories/independent-admission.png' },
      { value: '中考真题卷', name: '中考真题', note: '各科试卷与答案', icon: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/miniprogram/material-categories/exam-papers.png' }
    ]
  },
  onLoad() { this.load() },
  onUnload() { clearTimeout(this._timer); this._requestId = (this._requestId || 0) + 1 },
  async load(append = false) {
    if (append && (this.data.loading || !this.data.hasMore)) return
    const id = (this._requestId || 0) + 1
    this._requestId = id
    const page = append ? this.data.page + 1 : 1
    this.setData({ loading: true, error: '', ...(append ? {} : { records: [], total: 0, page: 0, hasMore: true }) })
    const params = { page, size: 20 }
    if (this.data.category) params.resourceCategory = this.data.category
    if (this.data.keyword.trim()) params.keyword = this.data.keyword.trim()
    try {
      const query = Object.keys(params).map(key => key + '=' + encodeURIComponent(params[key])).join('&')
      const result = await request('/api/school-materials?' + query)
      if (id !== this._requestId) return
      const rows = (Array.isArray(result.records) ? result.records : []).map(item => ({
        ...item,
        badge: /模板|范文/.test(item.resourceType || '') ? '模板' : /真题/.test(item.resourceType || '') ? '试卷' : '资料',
        meta: [item.subjectCategory, item.resourceType, item.year ? item.year + '年' : ''].filter(Boolean).join(' · ')
      }))
      const records = append ? this.data.records.concat(rows) : rows
      const total = Number(result.total) || 0
      this.setData({ records, total, page, hasMore: rows.length > 0 && page * 20 < total })
    } catch (e) {
      if (id === this._requestId) this.setData({ error: '资料加载失败，请重试' })
    } finally {
      if (id === this._requestId) this.setData({ loading: false })
    }
  },
  onSearchInput(e) {
    clearTimeout(this._timer)
    this._requestId = (this._requestId || 0) + 1
    this.setData({ keyword: e.detail.value, loading: true })
    this._timer = setTimeout(() => this.load(), 350)
  },
  search() { clearTimeout(this._timer); this.load() },
  clearSearch() { this.setData({ keyword: '' }); this.search() },
  selectCategory(e) {
    const item = this.data.categories[Number(e.currentTarget.dataset.index)]
    this.setData({ category: item.value, sectionTitle: item.name })
    this.search()
  },
  showAll() {
    this.setData({ keyword: '', category: '', sectionTitle: '全部资料' })
    this.search()
  },
  retry() { this.load(this.data.records.length > 0) },
  onReachBottom() { this.load(true) },
  openDetail(e) { wx.navigateTo({ url: '/pages/material-detail/material-detail?id=' + encodeURIComponent(e.currentTarget.dataset.id) }) }
})
