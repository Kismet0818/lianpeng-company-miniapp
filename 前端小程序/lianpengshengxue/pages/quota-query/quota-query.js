const { request } = require('../../utils/request')

const ACADEMIC_YEAR = '2025-2026'
const REQUEST_TIMEOUT = 15000

function requestWithTimeout(path) {
  return Promise.race([
    request(path),
    new Promise((resolve, reject) => {
      setTimeout(() => reject(new Error('REQUEST_TIMEOUT')), REQUEST_TIMEOUT)
    })
  ])
}

Page({
  data: {
    mode: 'region',
    pageTitle: '名额到区',
    district: '',
    displayYear: '2026年',
    schoolKeyword: '',
    results: [],
    allSchoolResults: [],
    schoolGroups: [],
    volunteerScopes: ['全部', '本区高中', '外区高中'],
    selectedVolunteerScope: '全部',
    allVolunteerResults: [],
    volunteerGroups: [],
    loading: true,
    searched: false,
    error: ''
  },

  onLoad(options) {
    const mode = options.mode === 'school' || options.mode === 'volunteer' ? options.mode : 'region'
    let district = ''
    try { district = decodeURIComponent(options.district || '') } catch (e) {}
    const pageTitle = mode === 'school' ? '名额到校' : (mode === 'volunteer' ? '1-15志愿' : '名额到区')
    this.setData({ mode, pageTitle, district })
    wx.setNavigationBarTitle({ title: district ? pageTitle + '·' + district : pageTitle })
    this.queryResults()
  },

  onSchoolInput(e) {
    this.setData({ schoolKeyword: e.detail.value })
  },

  onSchoolSearch() {
    const keyword = String(this.data.schoolKeyword || '').trim()
    const results = keyword
      ? this.data.allSchoolResults.filter(item => String(item.targetSchoolName || '').includes(keyword))
      : this.data.allSchoolResults
    this.setData({ results, schoolGroups: this.groupSchoolResults(results), searched: true })
  },

  groupSchoolResults(results) {
    const schoolGroups = []
    const groupMap = {}
    results.forEach(item => {
      const name = item.targetSchoolName || '目标初中待补充'
      if (!groupMap[name]) {
        groupMap[name] = { name, items: [] }
        schoolGroups.push(groupMap[name])
      }
      groupMap[name].items.push(item)
    })
    return schoolGroups
  },

  groupVolunteerResults(results) {
    const groups = []
    const groupMap = {}
    results.forEach(item => {
      const name = item.schoolType || '其他高中'
      if (!groupMap[name]) {
        groupMap[name] = { name, items: [] }
        groups.push(groupMap[name])
      }
      groupMap[name].items.push(item)
    })
    const preferredOrder = ['市实验性示范性高中', '市特色普通高中']
    return groups.sort((a, b) => {
      const aIndex = preferredOrder.indexOf(a.name)
      const bIndex = preferredOrder.indexOf(b.name)
      if (aIndex === -1 && bIndex === -1) return 0
      if (aIndex === -1) return 1
      if (bIndex === -1) return -1
      return aIndex - bIndex
    })
  },

  onVolunteerScopeTap(e) {
    const scope = e.currentTarget.dataset.scope || '全部'
    let results = this.data.allVolunteerResults
    if (scope === '本区高中') {
      results = results.filter(item => String(item.highSchoolDistrict || '').trim() === String(this.data.district || '').trim())
    } else if (scope === '外区高中') {
      results = results.filter(item => String(item.highSchoolDistrict || '').trim() !== String(this.data.district || '').trim())
    }
    this.setData({
      selectedVolunteerScope: scope,
      results,
      volunteerGroups: this.groupVolunteerResults(results)
    })
  },

  async queryResults() {
    if (!this.data.district && this.data.mode === 'region') {
      this.setData({ loading: false, error: '未获取到目标区县' })
      return
    }

    let path = '/api/score-query/region?district=' + encodeURIComponent(this.data.district) + '&year=' + encodeURIComponent(ACADEMIC_YEAR)
    if (this.data.mode === 'school') {
      path = '/api/score-query/school?district=' + encodeURIComponent(this.data.district) + '&year=' + encodeURIComponent(ACADEMIC_YEAR)
    } else if (this.data.mode === 'volunteer') {
      path = '/api/score-query/volunteer?district=' + encodeURIComponent(this.data.district) + '&year=' + encodeURIComponent(ACADEMIC_YEAR)
    }

    this.setData({ loading: true, searched: true, error: '', results: [], schoolGroups: [], volunteerGroups: [] })
    try {
      const data = await requestWithTimeout(path)
      let source = Array.isArray(data) ? data : []
      source = source.filter(item => !item.academicYear || item.academicYear === ACADEMIC_YEAR)
      if (this.data.mode === 'region') {
        source = source.filter(item => {
          const isRegionQuota = Number(item.targetType) === 2
          const isTargetDistrict = String(item.targetDistrict || '').trim() === String(this.data.district || '').trim()
          return isRegionQuota && isTargetDistrict
        })
      } else if (this.data.mode === 'school') {
        source = source.filter(item => {
          const isSchoolQuota = Number(item.targetType) === 1
          const isTargetDistrict = String(item.targetDistrict || '').trim() === String(this.data.district || '').trim()
          return isSchoolQuota && isTargetDistrict
        })
      } else {
        source = source.filter(item => String(item.targetDistrict || '').trim() === String(this.data.district || '').trim())
      }
      const results = source.map(item => {
        const change = Number(item.changeRange)
        const hasChange = item.changeRange !== null && item.changeRange !== undefined && !Number.isNaN(change)
        return Object.assign({}, item, {
          highSchoolName: item.highSchoolName || item.schoolName || '学校名称待补充',
          displayDistrict: item.highSchoolDistrict || '所在区待补充',
          quotaCount: item.quotaCount === null || item.quotaCount === undefined ? item.planCount : item.quotaCount,
          minScoreText: item.minScore === null || item.minScore === undefined ? '—' : String(item.minScore),
          changeText: !hasChange ? '—' : (change > 0 ? '+' : '') + change,
          changeValueText: !hasChange || change === 0 ? '—' : String(Math.abs(change)),
          changeClass: !hasChange || change === 0 ? 'flat' : (change > 0 ? 'up' : 'down')
        })
      })
      const schoolGroups = this.data.mode === 'school' ? this.groupSchoolResults(results) : []
      const volunteerGroups = this.data.mode === 'volunteer' ? this.groupVolunteerResults(results) : []
      this.setData({
        results,
        allSchoolResults: this.data.mode === 'school' ? results : [],
        schoolGroups,
        allVolunteerResults: this.data.mode === 'volunteer' ? results : [],
        volunteerGroups,
        loading: false
      })
    } catch (e) {
      let message = '数据加载失败，请检查网络后重试'
      if (e && e.message === 'REQUEST_TIMEOUT') {
        message = '查询数据较多，接口响应超时，请缩小查询范围后重试'
      } else if (e && /^HTTP_\d+$/.test(e.message || '')) {
        message = '志愿接口请求失败（' + e.message.replace('HTTP_', '') + '）'
      } else if (e && (e instanceof SyntaxError || e.message === 'INVALID_RESPONSE')) {
        message = '志愿接口返回的不是标准 JSON 数据'
      }
      this.setData({ loading: false, error: message })
    }
  },

  retry() {
    this.queryResults()
  },

  openSchool(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: '/pages/score-school-detail/score-school-detail?id=' + id + '&schoolSource=score' })
  },

  onShareAppMessage() {
    return {
      title: this.data.pageTitle + '·' + this.data.district,
      path: '/pages/quota-query/quota-query?mode=' + this.data.mode + '&district=' + encodeURIComponent(this.data.district)
    }
  }
})
