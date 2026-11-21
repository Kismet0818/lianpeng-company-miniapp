const { request } = require('../../utils/request')

Page({
  data: {
    schoolId: 0,
    school: {},
    schoolInitial: '校',
    admission: {},
    progressSection: null,
    contentSections: [],
    navSections: [],
    activeKey: '',
    admissionYear: '2026',
    navBarHeight: 88,
    menuTop: 26,
    menuHeight: 32,
    loading: true,
    empty: false,
    showBackTop: false
  },

  onLoad(options) {
    this._scrollTop = 0
    let navBarHeight = 88
    let menuTop = 26
    let menuHeight = 32
    try {
      const menu = wx.getMenuButtonBoundingClientRect()
      navBarHeight = menu.bottom + 8
      menuTop = menu.top
      menuHeight = menu.height
    } catch (e) {}
    this.setData({ navBarHeight, menuTop, menuHeight })
    const schoolId = Number(options.id || 0)
    if (!schoolId) {
      this.setData({ loading: false, empty: true })
      return
    }
    this.setData({ schoolId })
    this.loadData()
  },

  async loadData() {
    this.setData({ loading: true, empty: false })
    const schoolTask = request(`/api/schools/${this.data.schoolId}`, 'GET', {}, false)
      .catch(() => null)
    const admissionTask = request(`/api/schools/${this.data.schoolId}/admission`, 'GET', {}, false)
      .catch(() => null)

    const [schoolData, admissionData] = await Promise.all([schoolTask, admissionTask])
    const school = schoolData || {}
    school.logoUrl = this.resolveImageUrl(school.logoUrl)
    const normalized = this.normalizeAdmission(admissionData)

    this.setData({
      school,
      schoolInitial: (school.name || normalized.schoolName || '校').slice(0, 1),
      admission: admissionData || {},
      progressSection: normalized.progressSection,
      contentSections: normalized.contentSections,
      navSections: normalized.navSections,
      activeKey: normalized.navSections.length ? normalized.navSections[0].key : '',
      admissionYear: normalized.admissionYear,
      loading: false,
      empty: !admissionData || !normalized.contentSections.length && !normalized.progressSection
    })
  },

  normalizeAdmission(data) {
    if (!data) {
      return {
        schoolName: '',
        progressSection: null,
        contentSections: [],
        navSections: [],
        admissionYear: '2026'
      }
    }

    const sourceSections = Array.isArray(data.sections) && data.sections.length
      ? data.sections
      : this.buildSectionsFromFlatData(data)
    const rawText = JSON.stringify(data)
    const yearMatch = rawText.match(/20\d{2}/)
    const sections = sourceSections.map((item, index) => {
      const title = String(item.title || '招生信息')
      const key = this.getSectionKey(title, index)
      const sectionIcons = {
        project: '/assets/icons/zhaoshengdongtai/zhaoshengxiangmu2.png',
        candidate: '/assets/icons/zhaoshengdongtai/kaoshengyaoqiu.png',
        rules: '/assets/icons/zhaoshengdongtai/luquyaoqiu.png',
        exam: '/assets/icons/zhaoshengdongtai/ruxueceshi2.png',
        application: '/assets/icons/zhaoshengdongtai/baomingliucheng.png',
        graduation: '/assets/icons/zhaoshengdongtai/shengxuechengguo.png'
      }
      const blocks = String(item.content || '')
        .split(/\r?\n+/)
        .map(text => text.trim())
        .filter(Boolean)
      return {
        ...item,
        title,
        cleanTitle: title.replace(/^[^\u4e00-\u9fa5A-Za-z0-9]+/, ''),
        key,
        iconPath: sectionIcons[key] || '',
        anchorId: `section-${key}`,
        blocks,
        rows: Array.isArray(item.rows) ? item.rows : [],
        isExam: key === 'exam',
        isApplication: key === 'application',
        isGraduation: key === 'graduation'
      }
    })

    const progressSection = sections.find(item => item.key === 'progress') || null
    const contentSections = sections.filter(item => item.key !== 'progress')
    const navOrder = ['project', 'exam', 'application', 'graduation', 'timeline']
    const navLabels = {
      project: '招生项目',
      exam: '入学测试',
      application: '报名与申请',
      graduation: '升学情况',
      timeline: '招生时间轴'
    }
    const navSections = navOrder
      .filter(key => contentSections.some(item => item.key === key || (key === 'project' && ['candidate', 'rules'].includes(item.key))))
      .map(key => ({ key, label: navLabels[key], anchorId: this.findAnchor(contentSections, key) }))

    return {
      schoolName: data.schoolName || '',
      progressSection,
      contentSections,
      navSections,
      admissionYear: yearMatch ? yearMatch[0] : '2026'
    }
  },

  buildSectionsFromFlatData(data) {
    const sections = []
    const progressParts = []
    const eventInfo = data.admissionEventInfo || data.admission_event_info
    const admissionTime = data.admissionTime || data.admission_time
    if (eventInfo) progressParts.push(eventInfo)
    if (admissionTime && !String(eventInfo || '').includes(String(admissionTime))) {
      progressParts.push(`招生时间：${admissionTime}`)
    }
    if (progressParts.length) {
      sections.push({ title: '📅 招生进度', content: progressParts.join('\n') })
    }

    const projectRows = []
    this.addCompatRow(projectRows, '开设学段', data.educationalStages || data.targetGrades || data.educational_stages)
    this.addCompatRow(projectRows, '每年学费', data.tuitionRange || data.tuition_range, true)
    this.addCompatRow(projectRows, '招生人数', data.enrollmentCount || data.enrollment_count)
    this.addCompatRow(projectRows, '课程体系', data.curriculum)
    if (projectRows.length) sections.push({ title: '📌 招生项目', rows: projectRows })

    this.addCompatSection(sections, '👤 考生要求', data.targetStudents || data.target_students)
    this.addCompatSection(sections, '📋 录取要求', data.admissionRules || data.admissionRequirements || data.admission_rules)
    this.addCompatSection(sections, '📝 入学测试', data.examContent || data.exam_content)
    this.addCompatSection(sections, '📎 报名流程', data.applicationFlow || data.application_flow)
    this.addCompatSection(sections, '🏆 升学成果', data.graduationResult || data.graduationResults || data.graduation_result)
    return sections
  },

  addCompatRow(rows, label, value, highlight = false) {
    if (value === null || value === undefined || value === '') return
    rows.push({ label, value: String(value), highlight })
  },

  addCompatSection(sections, title, content) {
    if (content === null || content === undefined || content === '') return
    sections.push({ title, content: String(content) })
  },

  getSectionKey(title, index) {
    if (title.includes('招生进度')) return 'progress'
    if (title.includes('招生项目')) return 'project'
    if (title.includes('考生要求')) return 'candidate'
    if (title.includes('录取要求')) return 'rules'
    if (title.includes('入学测试')) return 'exam'
    if (title.includes('报名')) return 'application'
    if (title.includes('升学')) return 'graduation'
    if (title.includes('时间轴')) return 'timeline'
    return `other-${index}`
  },

  findAnchor(sections, key) {
    let target = sections.find(item => item.key === key)
    if (!target && key === 'project') {
      target = sections.find(item => ['candidate', 'rules'].includes(item.key))
    }
    return target ? target.anchorId : ''
  },

  resolveImageUrl(url) {
    return require('../../utils/image').resolveImageUrl(url)
  },

  onTapTag(e) {
    const { key, anchor } = e.currentTarget.dataset
    if (!anchor) return
    this.setData({ activeKey: key })
    wx.createSelectorQuery().select(`#${anchor}`).boundingClientRect(rect => {
      if (!rect) return
      wx.pageScrollTo({
        scrollTop: Math.max(0, rect.top + this._scrollTop - 52),
        duration: 260
      })
    }).exec()
  },

  onPageScroll(e) {
    this._scrollTop = e.scrollTop
    const showBackTop = e.scrollTop > 560
    if (showBackTop !== this.data.showBackTop) this.setData({ showBackTop })
  },

  onBackTop() {
    wx.pageScrollTo({ scrollTop: 0, duration: 300 })
  },

  onRetry() {
    this.loadData()
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.switchTab({ url: '/pages/school/school' })
    }
  },

  onShareAppMessage() {
    const name = this.data.school.name || this.data.admission.schoolName || ''
    return { title: `${name} · 招生动态`, path: `/pages/admission-detail/admission-detail?id=${this.data.schoolId}` }
  },

  onSchoolIntro() {
    wx.navigateTo({ url: `/pages/school-detail/school-detail?id=${this.data.schoolId}&schoolSource=international` })
  },

  onMaterials() {
    wx.navigateTo({ url: '/pages/material-list/material-list' })
  },

  onVisit() {
    const school = encodeURIComponent(this.data.school.name || this.data.admission.schoolName || '')
    wx.navigateTo({ url: `/pages/lead/lead?school=${school}` })
  },

  onConsult() {
    const school = encodeURIComponent(this.data.school.name || this.data.admission.schoolName || '')
    wx.navigateTo({ url: `/pages/lead/lead?school=${school}` })
  }
})
