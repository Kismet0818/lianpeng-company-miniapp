const { request } = require('../../utils/request')

Page({
  data: {
    loading: true,
    allSchools: [],
    schools: [],
    cityOptions: ['上海市'],
    cityIndex: 0,
    monthOptions: ['8月', '9月', '10月'],
    monthIndex: 0,
    stageOptions: ['全部学段', '幼儿园', '小学', '初中', '高中'],
    stageIndex: 0
  },

  onLoad() {
    this.loadOpenDays()
  },

  async loadOpenDays() {
    this.setData({ loading: true })
    try {
      const data = await request('/api/open-day/list', 'GET')
      const list = Array.isArray(data) ? data : []
      const schoolMap = new Map()
      list.forEach(item => {
        const schoolId = Number(item.schoolId || item.school_id)
        if (!schoolId) return
        const schoolCategoryText = [
          item.schoolName,
          item.school_name,
          item.schoolType,
          item.school_type,
          item.schoolNature,
          item.school_nature,
          item.curriculum
        ].filter(Boolean).join(' ')
        if (!/国际|国际部|外籍|外国人员子女|IB|AP|A-Level|IGCSE|IBDP|英式|美式|加拿大|新加坡|法国|德国|日本|韩国/i.test(schoolCategoryText)) return
        const current = schoolMap.get(schoolId)
        if (!current || Number(item.id) > Number(current.id)) {
          schoolMap.set(schoolId, {
            ...item,
            schoolId,
            logoUrl: this.resolveImageUrl(item.logoUrl || item.logo_url),
            educationalStages: item.educationalStages || item.educational_stages || '',
            schoolName: item.schoolName || item.school_name || ''
          })
        }
      })
      const allSchools = [...schoolMap.values()].sort((a, b) => Number(a.id) - Number(b.id))
      this.setData({ allSchools, loading: false }, () => this.applyFilters())
    } catch (error) {
      console.error('开放日列表加载失败：', error)
      this.setData({ allSchools: [], schools: [], loading: false })
    }
  },

  resolveImageUrl(url) {
    return require('../../utils/image').resolveImageUrl(url)
  },

  applyFilters() {
    const stage = this.data.stageOptions[this.data.stageIndex]
    const schools = this.data.allSchools.filter(item => {
      if (stage === '全部学段') return true
      return String(item.educationalStages || '').includes(stage)
    })
    this.setData({ schools })
  },

  onCityChange(e) {
    this.setData({ cityIndex: Number(e.detail.value) })
  },

  onMonthChange(e) {
    this.setData({ monthIndex: Number(e.detail.value) })
  },

  onStageChange(e) {
    this.setData({ stageIndex: Number(e.detail.value) }, () => this.applyFilters())
  },

  onSchoolTap(e) {
    const id = Number(e.currentTarget.dataset.id)
    const month = this.data.monthIndex + 8
    if (!id) return
    wx.navigateTo({ url: `/pages/open-day-detail/open-day-detail?id=${id}&month=${month}` })
  }
})
