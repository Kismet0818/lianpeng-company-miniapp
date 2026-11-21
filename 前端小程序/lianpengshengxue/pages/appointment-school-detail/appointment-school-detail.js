const { request } = require('../../utils/request')

Page({
  data: { school: {}, loading: true, error: false, favorite: false },
  normalizeSchool(school) {
    const textFields = ['description', 'schedule', 'teachingFeatures', 'featuresCourses', 'foreignSituation', 'competitionResult', 'facultyStrength', 'enrollmentTarget', 'enrollmentPlanning', 'enrollmentWay', 'feesStandard']
    const normalized = { ...school }
    textFields.forEach(key => { if (typeof normalized[key] === 'string') normalized[key] = normalized[key].replace(/\\r\\n/g, '\n').replace(/\\n/g, '\n').replace(/\\r/g, '\n') })
    return normalized
  },
  onLoad(options) { this.source = options.source || ''; this.schoolId = options.id || ''; this.loadDetail() },
  async loadDetail() {
    try {
      const school = this.normalizeSchool(await request('/api/appointment-schools/detail', 'GET', { source: this.source, id: this.schoolId }, false, true))
      if (!school || !school.id) throw new Error('NOT_FOUND')
      this.setData({ school, loading: false }); wx.setNavigationBarTitle({ title: school.name || '学校详情' })
    } catch (e) { this.setData({ loading: false, error: true }) }
  },
  goBack() { wx.navigateBack() },
  openMap() { const s = this.data.school; if (s.latitude && s.longitude) wx.openLocation({ latitude: Number(s.latitude), longitude: Number(s.longitude), name: s.name, address: s.address || '' }); else wx.showToast({ title: '暂无地图坐标', icon: 'none' }) },
  toggleFavorite() { this.setData({ favorite: !this.data.favorite }) },
  reserve() { wx.navigateTo({ url: '/pages/lead/lead?school=' + encodeURIComponent(this.data.school.name || '') }) }
})
