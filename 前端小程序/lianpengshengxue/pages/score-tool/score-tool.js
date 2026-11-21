const { request } = require('../../utils/request')

Page({
  data: { schoolLevel: 'high', schoolKeyword: '', schoolResults: [], schoolSearching: false, searched: false },

  selectSchoolLevel(e) {
    const schoolLevel = e.currentTarget.dataset.level
    if (schoolLevel === 'junior' || schoolLevel === 'high') this.setData({ schoolLevel, schoolResults: [], searched: false })
  },

  onSchoolInput(e) { this.setData({ schoolKeyword: e.detail.value }) },

  async searchSchools() {
    const name = String(this.data.schoolKeyword || '').trim()
    if (!name) return wx.showToast({ title: '请输入学校名称', icon: 'none' })
    this.setData({ schoolSearching: true, searched: true, schoolResults: [] })
    try {
      const data = await request('/api/score-query/schools/search', 'GET', { level: this.data.schoolLevel, name, page: 1, size: 5 })
      const records = Array.isArray(data) ? data : (data && (data.records || (data.data && data.data.records)) || [])
      const schoolResults = await Promise.all(records.slice(0, 5).map(item => this.loadSchoolScores(item)))
      this.setData({ schoolResults })
    } catch (e) {
      wx.showToast({ title: '查询失败，请稍后重试', icon: 'none' })
    } finally { this.setData({ schoolSearching: false }) }
  },

  async loadSchoolScores(item) {
    const year = '2025-2026'
    const id = Number(item.id)
    const paths = this.data.schoolLevel === 'junior'
      ? ['/api/score-query/school?year=' + year + '&district=' + encodeURIComponent(item.district || '') + '&school=' + encodeURIComponent(item.name)]
      : ['/api/score-query/region?year=' + year + '&highSchoolId=' + id, '/api/score-query/school?year=' + year + '&highSchoolId=' + id, '/api/score-query/volunteer?year=' + year + '&highSchoolId=' + id]
    const values = await Promise.all(paths.map(path => Promise.race([
      request(path, 'GET').catch(() => []),
      new Promise(resolve => setTimeout(() => resolve([]), 8000))
    ])))
    const groups = paths.map((path, index) => ({ key: String(index), title: this.data.schoolLevel === 'junior' ? '名额到校' : ['名额到区', '名额到校', '1-15志愿'][index], rows: [] }))
    values.forEach((value, index) => {
      const list = Array.isArray(value) ? value : (value && (value.records || value.data && value.data.records) || [])
      list.forEach((row, n) => groups[index].rows.push({ key: String(row.id || index + '-' + n), label: row.targetSchoolName || row.targetDistrict || '招生计划', plan: row.quotaCount ?? row.planCount ?? '—', score: row.minScore === null || row.minScore === undefined ? '—' : row.minScore }))
    })
    return Object.assign({}, item, { scoreGroups: groups, hasScore: groups.some(group => group.rows.length > 0) })
  }
})
