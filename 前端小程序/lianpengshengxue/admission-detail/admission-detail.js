const { request } = require('../../utils/request')

Page({
  data: {
    schoolId: 0,
    school: null,
    admission: null,
    loading: true,
    requirementShort: ''
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ schoolId: Number(options.id) })
      this.loadData()
    }
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      const school = await request('/api/schools/' + this.data.schoolId, 'GET', {}, false)
      this.setData({ school })
    } catch (e) {}

    try {
      const admission = await request('/api/schools/' + this.data.schoolId + '/admission', 'GET', {}, false)
      if (admission && admission.admissionRequirements) {
        const short = admission.admissionRequirements.length > 50
          ? admission.admissionRequirements.slice(0, 50) + '...查看详情'
          : admission.admissionRequirements
        this.setData({ requirementShort: short })
      }
      this.setData({ admission: admission || {} })
    } catch (e) {}

    this.setData({ loading: false })
  },

  onTapTag(e) {},

  onShareAppMessage() {
    const name = this.data.school ? this.data.school.name : ''
    return { title: name + ' · 招生动态', path: '/pages/admission-detail/admission-detail?id=' + this.data.schoolId }
  },

  onSchoolIntro() {
    wx.navigateTo({ url: '/pages/school-detail/school-detail?id=' + this.data.schoolId + '&schoolSource=international' })
  },

  onMaterials() {
    wx.showToast({ title: '历年资料即将上线', icon: 'none' })
  },

  onVisit() {
    wx.navigateTo({ url: '/pages/lead/lead?school=' + encodeURIComponent((this.data.school && this.data.school.name) || '') })
  },

  onConsult() {
    wx.navigateTo({ url: '/pages/lead/lead?school=' + encodeURIComponent((this.data.school && this.data.school.name) || '') })
  }
})
