const { request } = require('../../utils/request')

Page({
  data: {
    statusBarHeight: 20,
    activeDistrict: '全部',
    districts: ['全部', '徐汇区', '黄浦区', '静安区', '长宁区', '杨浦区', '普陀区'],
    schools: [],
    loading: true
  },

  onLoad() {
    try {
      const info = wx.getSystemInfoSync()
      this.setData({ statusBarHeight: info.statusBarHeight || 20 })
    } catch (e) {}
    this.loadSchools()
  },

  async loadSchools() {
    this.setData({ loading: true })
    try {
      const params = { page: 1, size: 500, tier: '一梯队' }
      if (this.data.activeDistrict !== '全部') params.district = this.data.activeDistrict
      const result = await request('/api/primary-schools', 'GET', params, false)
      const records = result && Array.isArray(result.records) ? result.records : []
      const schools = records.map(item => ({
        ...item,
        logoUrl: item.backgroundImageUrl || '',
        badge: item.schoolType === '民办' ? '民办学校' : '公办学校'
      }))
      this.setData({ schools, loading: false })
    } catch (e) {
      this.setData({ schools: [], loading: false })
      wx.showToast({ title: '学校信息加载失败', icon: 'none' })
    }
  },

  onDistrictTap(e) {
    const district = e.currentTarget.dataset.district
    if (district === this.data.activeDistrict) return
    this.setData({ activeDistrict: district }, () => this.loadSchools())
  },

  onSchoolTap(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: '/pages/score-school-detail/score-school-detail?id=' + id + '&schoolSource=primary' })
  },

  onBack() { wx.navigateBack() },
  onShareAppMessage() { return { title: '上海一梯队热门学校', path: '/pages/hot-schools/hot-schools' } }
})
