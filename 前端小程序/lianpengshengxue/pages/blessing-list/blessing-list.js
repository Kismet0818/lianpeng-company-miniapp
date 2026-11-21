const { request } = require('../../utils/request')
const { resolveImageUrl } = require('../../utils/image')

Page({
  data: { records: [], page: 1, pages: 1, total: 0, loading: false, initialized: false, schoolSource: '', schoolId: 0, schoolName: '' },

  onLoad(options) {
    options = options || {}
    let schoolName = ''
    try { schoolName = decodeURIComponent(options.schoolName || '') } catch (e) { schoolName = String(options.schoolName || '') }
    this.setData({ schoolSource: String(options.schoolSource || ''), schoolId: Number(options.schoolId || 0), schoolName })
    this.loadList(true)
  },
  onPullDownRefresh() { this.loadList(true).finally(() => wx.stopPullDownRefresh()) },
  onReachBottom() {
    if (!this.data.loading && this.data.page < this.data.pages) this.loadList(false)
  },

  async loadList(reset) {
    if (this.data.loading) return
    const page = reset ? 1 : this.data.page + 1
    this.setData({ loading: true })
    try {
      const scope = this.data.schoolSource && this.data.schoolId ? '&schoolSource=' + encodeURIComponent(this.data.schoolSource) + '&schoolId=' + this.data.schoolId : ''
      const data = await request('/api/blessing/list?page=' + page + '&size=20' + scope)
      const list = Array.isArray(data && data.records) ? data.records : []
      const records = list.map(item => {
        const nickname = item.nickname || '匿名用户'
        return { ...item, nickname, initial: nickname.slice(0, 1), avatar: resolveImageUrl(item.avatar) }
      })
      this.setData({
        records: reset ? records : this.data.records.concat(records),
        page: Number(data.current) || page,
        pages: Number(data.pages) || 1,
        total: Number(data.total) || records.length,
        initialized: true
      })
    } catch (e) {
      console.error('祝福列表加载失败：', e)
      this.setData({ initialized: true })
    } finally {
      this.setData({ loading: false })
    }
  }
})
