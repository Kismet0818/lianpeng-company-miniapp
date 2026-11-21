const { request } = require('../../utils/request')
const app = getApp()

Page({
  data: {
    openDayId: 0,
    school: {},
    loading: true,
    campaignYear: 2026,
    monthOptions: [],
    showReserve: false,
    selectedMonth: 8,
    reserveStart: '2026-08-01',
    reserveEnd: '2026-08-31',
    reserveTime: '',
    parentName: '',
    parentPhone: '',
    childGrade: '',
    gradeOptions: ['幼儿园', '小学一年级', '小学二年级', '小学三年级', '小学四年级', '小学五年级', '小学六年级', '初中一年级', '初中二年级', '初中三年级', '高中一年级', '高中二年级', '高中三年级'],
    submitting: false,
    activeRecommendTab: 0,
    recommendTabs: ['相关推荐', '热门推荐'],
    relatedRecommendations: [
      { id: 28, title: '国际学校探校怎么探，探校时家长一定要问清楚的那些问题！' },
      { id: 29, title: '如何考察一所国际学校？10个维度教你对话校长、老师和家长' },
      { id: 30, title: '国际学校探校的时候最容易忽视的几个问题！？' },
      { id: 31, title: '国际学校为什么要做开放日？' },
      { id: 25, title: '国际学校择校6步骤，帮孩子选择更适合的学校！' }
    ],
    hotRecommendations: [
      { id: 1, title: '国际教育常见问题-小学阶段' },
      { id: 3, title: '从小学到大学，这几条升学路径你知道吗？' },
      { id: 7, title: '选择给孩子规划国际教育路线，家长需要考虑哪几个方面？' },
      { id: 13, title: '国际学校三大类型全解析' },
      { id: 24, title: '一文了解国际学校课程体系（A-Level、AP、IB）' }
    ],
    currentRecommendations: [],
    guideSteps: [
      { stepTitle: '升学规划', subTitle: '明确升学方向', stepIcon: '/assets/icons/zexiaozhinan/shengxueguihua.png', stepIconSize: 46, stepIconX: 0, stepIconY: 0, menus: [
        { label: '升学路径', category: '升学规划', icon: '/assets/icons/zexiaozhinan/1/shengxuelujing.png' },
        { label: '规划须知', category: '升学规划', icon: '/assets/icons/zexiaozhinan/1/guihuaxuzhi.png' },
        { label: '学校介绍', category: '升学规划', icon: '/assets/icons/zexiaozhinan/1/xuexiaojieshao.png' },
        { label: '课程体系', category: '升学规划', icon: '/assets/icons/zexiaozhinan/1/kechengtixi.png' }
      ] },
      { stepTitle: '择校探校', subTitle: '匹配理想学校', stepIcon: '/assets/icons/zexiaozhinan/zexiaotanxiao.png', stepIconSize: 30, stepIconX: 5, stepIconY: 2, menus: [
        { label: '择校须知', category: '择校探校', icon: '/assets/icons/zexiaozhinan/2/zexiaotongzhi.png' },
        { label: '实地探校', category: '择校探校', icon: '/assets/icons/zexiaozhinan/2/shiditanxiao.png' },
        { label: '学校开放日', category: '择校探校', icon: '/assets/icons/zexiaozhinan/2/xuexiaokaifangri.png' },
        { label: '报考要求', category: '择校探校', icon: '/assets/icons/zexiaozhinan/2/baokaoyaoqiu.png' }
      ] },
      { stepTitle: '递交申请', subTitle: '展示自我优势', stepIcon: '/assets/icons/zexiaozhinan/dijiaoshenqing.png', stepIconSize: 30, stepIconX: 1, stepIconY: 2, menus: [
        { label: '如何申请', category: '递交申请', icon: '/assets/icons/zexiaozhinan/3/ruheshenqing.png' },
        { label: '申请时间', category: '递交申请', icon: '/assets/icons/zexiaozhinan/3/shenqingshijian.png' },
        { label: '申请材料', category: '递交申请', icon: '/assets/icons/zexiaozhinan/3/shenqingcailiao.png' },
        { label: '入学测试', category: '递交申请', icon: '/assets/icons/zexiaozhinan/3/ruxueceshi.png' }
      ] },
      { stepTitle: '录取准备', subTitle: '开启学业新征程', stepIcon: '/assets/icons/zexiaozhinan/luquzhunbei.png', stepIconSize: 30, stepIconX: 0, stepIconY: 0, menus: [
        { label: '录取须知', category: '录取准备', icon: '/assets/icons/zexiaozhinan/4/zexiaotongzhi.png' },
        { label: '入学问题', category: '录取准备', icon: '/assets/icons/zexiaozhinan/4/ruxuewenti.png' },
        { label: '开学时间', category: '录取准备', icon: '/assets/icons/zexiaozhinan/4/shenqingshijian.png' },
        { label: '提前学习', category: '录取准备', icon: '/assets/icons/zexiaozhinan/4/tiqianxuexi.png' }
      ] }
    ]
  },

  onLoad(options) {
    const openDayId = Number(options.id)
    const presetMonth = [8, 9, 10].includes(Number(options.month)) ? Number(options.month) : 8
    this.setData({
      openDayId,
      selectedMonth: presetMonth,
      currentRecommendations: this.data.relatedRecommendations,
      monthOptions: this.buildMonthOptions()
    })
    if (openDayId) this.loadDetail(openDayId)
    else this.setData({ loading: false })
  },

  async loadDetail(id) {
    try {
      const data = await request(`/api/open-day/${id}`, 'GET')
      this.setData({
        school: {
          ...data,
          id: Number(data.id || id),
          schoolId: Number(data.schoolId || data.school_id),
          schoolName: data.schoolName || data.school_name || '',
          educationalStages: data.educationalStages || data.educational_stages || '',
          logoUrl: this.resolveImageUrl(data.logoUrl || data.logo_url)
        },
        loading: false
      })
    } catch (error) {
      console.error('开放日详情加载失败：', error)
      this.setData({ loading: false })
    }
  },

  resolveImageUrl(url) {
    return require('../../utils/image').resolveImageUrl(url)
  },

  buildMonthOptions() {
    const year = this.data.campaignYear
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
    return [8, 9, 10].map(month => {
      const end = new Date(year, month, 0).getTime()
      return { month, label: `${month}月`, status: end < today ? '已结束' : '可预约', disabled: end < today }
    })
  },

  formatDate(year, month, day) {
    const pad = value => String(value).padStart(2, '0')
    return `${year}-${pad(month)}-${pad(day)}`
  },

  onMonthTap(e) {
    const month = Number(e.currentTarget.dataset.month)
    const option = this.data.monthOptions.find(item => item.month === month)
    if (!option || option.disabled) return
    const year = this.data.campaignYear
    const now = new Date()
    const monthStart = new Date(year, month - 1, 1)
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const startDate = today > monthStart ? today : monthStart
    const endDay = new Date(year, month, 0).getDate()
    const reserveStart = this.formatDate(startDate.getFullYear(), startDate.getMonth() + 1, startDate.getDate())
    const reserveEnd = this.formatDate(year, month, endDay)
    this.setData({ selectedMonth: month, reserveStart, reserveEnd, reserveTime: '', showReserve: true })
  },

  closeReserve() {
    if (this.data.submitting) return
    this.setData({ showReserve: false })
  },

  noop() {},

  onFormInput(e) {
    const key = e.currentTarget.dataset.key
    if (!key) return
    this.setData({ [key]: e.detail.value })
  },

  onGradeChange(e) {
    this.setData({ childGrade: this.data.gradeOptions[Number(e.detail.value)] || '' })
  },

  onDateChange(e) {
    this.setData({ reserveTime: e.detail.value || '' })
  },

  async submitLead() {
    return request('/api/lead/submit', 'POST', {
      parent_name: this.data.parentName.trim(),
      parent_phone: this.data.parentPhone.trim(),
      child_grade: this.data.childGrade,
      target_school: this.data.school.schoolName || '',
      open_day_id: this.data.openDayId,
      reserve_time: this.data.reserveTime
    }, true)
  },

  async onReserveSubmit() {
    const parentName = this.data.parentName.trim()
    const parentPhone = this.data.parentPhone.trim()
    if (!parentName) return wx.showToast({ title: '请填写姓名', icon: 'none' })
    if (!/^1\d{10}$/.test(parentPhone)) return wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
    if (!this.data.childGrade) return wx.showToast({ title: '请选择在读年级', icon: 'none' })
    if (!this.data.reserveTime) return wx.showToast({ title: '请选择探校日期', icon: 'none' })
    if (Number(this.data.reserveTime.slice(5, 7)) !== this.data.selectedMonth) {
      return wx.showToast({ title: '请选择对应月份的日期', icon: 'none' })
    }

    this.setData({ submitting: true })
    try {
      if (!wx.getStorageSync('token')) await app.login()
      try {
        await this.submitLead()
      } catch (error) {
        if (!error || error.code !== 401) throw error
        await app.login()
        await this.submitLead()
      }
      this.setData({ showReserve: false, submitting: false })
      wx.showToast({ title: '预约成功', icon: 'success' })
    } catch (error) {
      console.error('开放日预约失败：', error)
      this.setData({ submitting: false })
    }
  },

  copyAddress() {
    if (!this.data.school.address) return
    wx.setClipboardData({ data: this.data.school.address })
  },

  onRecommendTab(e) {
    const activeRecommendTab = Number(e.currentTarget.dataset.index)
    this.setData({
      activeRecommendTab,
      currentRecommendations: activeRecommendTab === 0 ? this.data.relatedRecommendations : this.data.hotRecommendations
    })
  },

  onRecommendationTap(e) {
    const id = Number(e.currentTarget.dataset.id)
    if (!id) return
    wx.navigateTo({ url: '/pages/guide-detail/guide-detail?id=' + id })
  },

  onGuideMenu(e) {
    this.openGuide(e.currentTarget.dataset.category, e.currentTarget.dataset.menu)
  },

  openGuide(category, menu) {
    wx.navigateTo({
      url: '/pages/guide-list/guide-list?category=' + encodeURIComponent(category) + '&path=' + encodeURIComponent(menu)
    })
  },

  backToTop() {
    wx.pageScrollTo({ scrollTop: 0, duration: 300 })
  }
})
