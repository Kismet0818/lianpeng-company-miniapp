const { request } = require('../../utils/request')

const RANDOM_BLESSINGS = [
  '中考顺利，沉着发挥，考入理想高中！',
  '愿你以梦为马，不负韶华，金榜题名！',
  '愿所有努力都有回报，落笔生花，未来可期！',
  '保持从容与自信，奔赴属于你的美好未来！',
  '愿你带着勇气走进考场，带着喜悦迎接梦想！'
]

Page({
  data: {
    target: 'child',
    name: '',
    blessing: '',
    submitting: false,
    schoolSource: '',
    schoolId: 0,
    schoolName: ''
  },

  onLoad(options) {
    options = options || {}
    const schoolSource = String(options.schoolSource || '')
    const schoolId = Number(options.schoolId || 0)
    let schoolName = ''
    try { schoolName = decodeURIComponent(options.schoolName || '') } catch (e) { schoolName = String(options.schoolName || '') }
    this.setData({ schoolSource, schoolId, schoolName })
  },

  selectTarget(e) { this.setData({ target: e.currentTarget.dataset.value }) },
  onNameInput(e) { this.setData({ name: e.detail.value }) },
  onBlessingInput(e) { this.setData({ blessing: e.detail.value }) },
  randomBlessing() {
    const current = this.data.blessing
    const candidates = RANDOM_BLESSINGS.filter(item => item !== current)
    const list = candidates.length ? candidates : RANDOM_BLESSINGS
    this.setData({ blessing: list[Math.floor(Math.random() * list.length)] })
  },

  buildContent() {
    const name = String(this.data.name || '').trim()
    const blessing = String(this.data.blessing || '').trim()
    const targetName = this.data.target === 'self' ? '自己' : (this.data.target === 'classmate' ? (name || '同学') : (name || '孩子'))
    return '祝' + targetName + ' ' + (blessing || '中考顺利，考入理想高中！')
  },

  async submit() {
    if (this.data.submitting) return
    const content = this.buildContent()
    if (!this.data.schoolSource || !this.data.schoolId) {
      wx.showToast({ title: '学校信息缺失，请返回重试', icon: 'none' })
      return
    }
    if (content.length > 200) {
      wx.showToast({ title: '祝福内容不能超过200字', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      if (!wx.getStorageSync('token')) await getApp().login()
      await request('/api/blessing', 'POST', { content, schoolSource: this.data.schoolSource, schoolId: String(this.data.schoolId) }, true)
      const eventChannel = this.getOpenerEventChannel()
      eventChannel.emit('blessingPublished')
      wx.showToast({ title: '祝福发布成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 500)
    } catch (e) {
      console.error('祝福发布失败：', e)
    } finally {
      this.setData({ submitting: false })
    }
  }
})
