const { request } = require('../../utils/request')

Page({
  data: {
    records: [],
    loading: true
  },

  onShow() {
    this.loadRecords()
  },

  async loadRecords() {
    this.setData({ loading: true })
    try {
      const data = await request('/api/user/lead-history', 'GET', {}, true)
      const records = (Array.isArray(data) ? data : []).map(item => ({
        ...item,
        displayTime: this.formatRecordTime(item.createTime || item.create_time)
      }))
      this.setData({
        records,
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  formatRecordTime(value) {
    if (!value) return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
})
