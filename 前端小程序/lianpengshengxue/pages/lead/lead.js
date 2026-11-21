const { request } = require('../../utils/request')

Page({
  data: {
    bannerUrl: '',
    statusBarHeight: 44,
    parentName: '',
    childGrade: '',
    targetSchool: '',
    parentPhone: '',
    concern: ''
  },

  onLoad() {
    const sys = wx.getSystemInfoSync()
    this.setData({ statusBarHeight: sys.statusBarHeight || 44 })
    this.loadBanner()
  },

  async loadBanner() {
    try {
      const config = await request('/api/config/platform', 'GET', {}, false)
      if (config && config.lead_page_banner) {
        this.setData({ bannerUrl: config.lead_page_banner })
      }
    } catch (e) {}
  },

  onInput(e) {
    const { key } = e.currentTarget.dataset
    this.setData({ [key]: e.detail.value })
  },

  onBack() {
    wx.navigateBack({ delta: 1 })
  },

  async onSubmit() {
    if (!this.data.parentName || !this.data.childGrade || !this.data.targetSchool || !this.data.parentPhone) {
      wx.showToast({ title: '请填写完整', icon: 'none' })
      return
    }
    try {
      await request('/api/lead/submit', 'POST', {
        parent_name: this.data.parentName,
        child_grade: this.data.childGrade,
        target_school: this.data.targetSchool,
        parent_phone: this.data.parentPhone,
        concern: this.data.concern
      }, true)
      wx.showToast({ title: '提交成功，老师会尽快联系您', icon: 'success', duration: 2000 })
    } catch (e) {}
  }
})