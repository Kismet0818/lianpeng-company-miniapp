const { request } = require('../../utils/request')

Page({
  data: {
    step: 0,
    questions: ['选择区域', '选择课程', '学费预算', '当前学段'],
    options: [
      ['浦东新区','徐汇区','长宁区','闵行区','杨浦区','不限'],
      ['IB','AP','A-Level','不限'],
      ['20万内','20-30万','30万以上','不限'],
      ['小学','初中','高中','不限']
    ],
    answers: [],
    result: []
  },

  onSelect(e) {
    const val = e.currentTarget.dataset.val
    const answers = [...this.data.answers, val]
    if (answers.length < 4) {
      this.setData({ step: answers.length, answers })
    } else {
      this.setData({ answers })
      this.submit()
    }
  },

  async submit() {
    try {
      wx.showLoading({ title: '匹配中...' })
      const data = await request('/api/assessment/match', 'POST', { answers: this.data.answers })
      this.setData({ result: data, step: 4 })
      wx.hideLoading()
    } catch (e) {
      wx.hideLoading()
    }
  },

  restart() {
    this.setData({ step: 0, answers: [], result: [] })
  },

  onTapSchool(e) {
    wx.navigateTo({ url: '/pages/school-detail/school-detail?id=' + e.currentTarget.dataset.id + '&schoolSource=international' })
  }
})