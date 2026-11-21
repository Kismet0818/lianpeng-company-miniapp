const { request } = require('../../utils/request')
const { startSchoolView, finishSchoolView } = require('../../utils/activity')
const ACTIVE_ADMISSION_SCHOOL_IDS = new Set([17, 39, 92, 76, 73, 45, 105, 81, 36, 47, 4, 6])
const ADMISSION_YEAR_BY_SCHOOL_ID = {
  17: '2026', 39: '2026', 92: '2026', 76: '2026',
  73: '2025', 45: '2025', 105: '2025', 81: '2026',
  36: '2026', 47: '2026', 4: '2025'
}

Page({
  data: {
    school: {},
    schoolSource: 'international',
    hasAdmissionContent: false,
    admissionYear: '',
    isFav: false,
    showLead: false,
    showQr: false,
    qrImageUrl: '',
    qrImageType: '',
    activeTab: 0,
    parentName: '',
    childGrade: '',
    gradeOptions: ['幼儿园', '小学一年级', '小学二年级', '小学三年级', '小学四年级', '小学五年级', '小学六年级', '初中一年级', '初中二年级', '初中三年级', '高中一年级', '高中二年级', '高中三年级'],
    targetSchool: '',
    phone: '',
    reactionItems: [
      { type: 1, label: '夯', image: '/assets/ranking-emojis/ranking-1-heng.png', count: 0 },
      { type: 2, label: '赞', image: '/assets/ranking-emojis/ranking-2-zan.png', count: 0 },
      { type: 3, label: '一般', image: '/assets/ranking-emojis/ranking-3-yiban.png', count: 0 },
      { type: 4, label: '差', image: '/assets/ranking-emojis/ranking-4-cha.png', count: 0 },
      { type: 5, label: '拉', image: '/assets/ranking-emojis/ranking-5-la.png', count: 0 }
    ],
    selectedReaction: null,
    reactionLoading: false,
    reactionSubmitting: false,
    reactionNeedsLogin: false,
    enrollSections: []
    ,duikouAdmissionYear: 0
    ,duikouPrimarySchools: []
  },

  onLoad(options) {
    options = options || {}
    const VALID_SOURCES = ['international', 'score', 'junior', 'general', 'primary', 'kindergarten']
    const schoolSource = VALID_SOURCES.indexOf(String(options.schoolSource || '')) >= 0 ? String(options.schoolSource) : 'international'
    this.setData({ schoolSource })
    startSchoolView(this, schoolSource, options.id)
    if (options.id) {
      this.loadSchool(options.id)
    }
  },

  onHide() { finishSchoolView(this) },
  onUnload() { finishSchoolView(this) },

  async loadSchool(id) {
    try {
      const schoolSource = this.data.schoolSource
      const [data, imageData] = await Promise.all([
        schoolSource === 'primary' ? request(`/api/primary-schools/${id}`) : schoolSource === 'kindergarten' ? request(`/api/kindergartens/${id}`) : request(`/api/schools/${id}`),
        (schoolSource === 'primary' || schoolSource === 'kindergarten') ? Promise.resolve([]) : request(`/api/schools/${id}/images`).catch(() => [])
      ])
      const galleryImages = this.normalizeGallery(imageData)
      const school = {
        ...data,
        id: Number(data.id || id),
        logoUrl: this.resolveImageUrl(data.logoUrl || data.backgroundImageUrl),
        galleryImages,
        curriculum: this.normalizeSchoolValue(data.curriculum),
        curriculumItems: this.parseCurriculumItems(data.curriculum ?? data.curriculm),
        grades: this.normalizeSchoolValue(data.grades),
        teachingMode: this.normalizeSchoolValue(data.teachingMode ?? data.teaching_mode),
        description: this.normalizeSchoolValue(data.description),
        addressText: this.normalizeSchoolValue(data.address),
        graduationResult: this.normalizeSchoolValue(data.graduationResult ?? data.graduation_result),
        classSize: this.normalizeSchoolValue(data.classSize ?? data.class_size),
        campusAreaText: this.formatCampusArea(data.campusArea ?? data.campus_area),
        teacherRatioText: this.formatTeacherStudentRatio(
          data.teacherRatio ?? data.teacher_ratio ?? data.teacherStudentRatio ?? data.teacher_student_ratio,
          data.studentCount ?? data.student_count,
          data.teacherCount ?? data.teacher_count
        ),
        teacherSeniorRatioText: this.formatTeacherSeniorRatio(data.teacherSeniorRatio ?? data.teacher_senior_ratio),
        teacherCountText: this.formatTeacherCount(data.teacherCount ?? data.teacher_count)
      }
      school.schoolLevel = data.schoolLevel || (schoolSource === 'kindergarten' ? '幼儿园' : '')
      school.kindergartenLevel = this.normalizeSchoolValue(data.kindergartenLevel)
      school.careType = this.normalizeSchoolValue(data.careType)
      school.ageRange = this.normalizeSchoolValue(data.ageRange)
      school.phone = this.normalizeSchoolValue(data.phone)
      this.setData({
        school,
        hasAdmissionContent: ACTIVE_ADMISSION_SCHOOL_IDS.has(Number(school.id)),
        admissionYear: ADMISSION_YEAR_BY_SCHOOL_ID[Number(school.id)] || '',
        targetSchool: data.name || ''
        ,duikouAdmissionYear: Number(data.duikouAdmissionYear || 0)
        ,duikouPrimarySchools: this.normalizeDuikouSchools(data.duikouPrimarySchools)
      })
      this.cacheRecentSchool(school)
      this.checkFav(id)
      this.loadReactions(school.id)
      this.loadEnrollment(data.name)
    } catch (e) {}
  },

  normalizeDuikouSchools(list) {
    if (!Array.isArray(list)) return []
    return list.map(item => ({
      ...item,
      primarySchoolId: Number(item.primarySchoolId || 0),
      primarySchoolName: item.primarySchoolName || '小学名称待核实',
      relationTypeText: this.duikouRelationText(item.relationType),
      scopeText: item.scopeText || '',
      note: item.note || ''
    }))
  },

  duikouRelationText(type) {
    return ({ direct: '明确对口', area_based: '按地段对应', lottery: '电脑派位', overall: '统筹安排' }[type] || '招生范围')
  },

  onDuikouPrimaryTap(e) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (!id) return
    wx.navigateTo({ url: `/pages/school-detail/school-detail?id=${id}&schoolSource=primary` })
  },

  // 民办小初招生摇号数据（仅小学/初中民办，按学校名称匹配；幼儿园与高中不展示）
  async loadEnrollment(name) {
    if (!name) return
    const src = this.data.schoolSource
    if (src === 'kindergarten' || src === 'general' || src === 'score') return
    try {
      const list = await request('/api/school-enrollments?name=' + encodeURIComponent(name), 'GET', {}, false, true)
      this.setData({ enrollSections: this.buildEnrollmentSections(list || []) })
    } catch (e) {
      this.setData({ enrollSections: [] })
    }
  },

  buildEnrollmentSections(list) {
    const sections = []
    for (const stageName of ['小学', '初中']) {
      const rows = list.filter(r => r.schoolStage === stageName)
      if (!rows.length) continue
      const years = []
      const yearNums = Array.from(new Set(rows.map(r => r.enrollYear))).sort((a, b) => b - a)
      for (const y of yearNums) {
        const items = rows
          .filter(r => r.enrollYear === y)
          .map(r => ({
            campusText: this.normalizeSchoolValue(r.campus) || '统招',
            countText: (r.enrollmentCount === null || r.enrollmentCount === undefined) ? '' : r.enrollmentCount + '人',
            rateText: this.formatLotteryRate(r.lotteryRate)
          }))
          .filter(it => it.countText || it.rateText)
        if (items.length) years.push({ year: y, items })
      }
      if (!years.length) continue
      let metrics = null
      if (stageName === '初中') {
        const m = rows.find(r => r.maxScore != null || r.score700Count || r.keyHighRate != null)
        if (m) {
          metrics = {
            maxScoreText: m.maxScore == null ? '' : String(m.maxScore),
            count700Text: this.normalizeSchoolValue(m.score700Count),
            keyRateText: this.formatLotteryRate(m.keyHighRate)
          }
          if (!metrics.maxScoreText && !metrics.count700Text && !metrics.keyRateText) metrics = null
        }
      }
      sections.push({ stage: stageName, years, metrics })
    }
    return sections
  },

  formatLotteryRate(value) {
    if (value === null || value === undefined || value === '') return ''
    const n = Number(value)
    if (!Number.isFinite(n)) return ''
    const pct = n <= 1 ? n * 100 : n
    return `${Number(pct.toFixed(2))}%`
  },

  normalizeReactionData(data) {
    const counts = data && data.counts ? data.counts : {}
    const selected = Number(data && data.reactionType)
    return {
      selectedReaction: selected >= 1 && selected <= 5 ? selected : null,
      reactionItems: this.data.reactionItems.map(item => ({
        ...item,
        count: Math.max(0, Number(counts[item.type]) || 0)
      }))
    }
  },

  async loadReactions(schoolId) {
    const id = Number(schoolId)
    if (!Number.isInteger(id) || id <= 0) return
    if (!wx.getStorageSync('token')) {
      this.setData({ reactionNeedsLogin: true, reactionLoading: false })
      return
    }
    this.setData({ reactionLoading: true, reactionNeedsLogin: false })
    try {
      const data = await request(`/api/school-reactions/${id}?schoolSource=${encodeURIComponent(this.data.schoolSource)}`, 'GET', {}, true)
      this.setData({ ...this.normalizeReactionData(data), reactionNeedsLogin: false })
    } catch (e) {
      if (e && (e.code === 401 || e.statusCode === 401)) {
        this.setData({ reactionNeedsLogin: true })
        wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
      }
    } finally {
      this.setData({ reactionLoading: false })
    }
  },

  async onReactionLogin() {
    if (this.data.reactionLoading || this.data.reactionSubmitting) return
    this.setData({ reactionLoading: true })
    try {
      await getApp().login()
      await this.loadReactions(this.data.school.id)
    } catch (e) {
      wx.showToast({ title: '登录失败，请重试', icon: 'none' })
    } finally {
      this.setData({ reactionLoading: false })
    }
  },

  async onSelectReaction(e) {
    const reactionType = Number(e.currentTarget.dataset.type)
    if (!Number.isInteger(reactionType) || reactionType < 1 || reactionType > 5) return
    if (this.data.reactionSubmitting) return
    if (!wx.getStorageSync('token')) {
      wx.showToast({ title: '请先登录后再评价', icon: 'none' })
      this.setData({ reactionNeedsLogin: true })
      return
    }
    const schoolId = Number(this.data.school.id)
    if (!Number.isInteger(schoolId) || schoolId <= 0) return
    this.setData({ reactionSubmitting: true })
    try {
      const data = await request(`/api/school-reactions/${schoolId}`, 'POST', {
        schoolSource: this.data.schoolSource,
        reactionType: Number(reactionType)
      }, true)
      this.setData({ ...this.normalizeReactionData(data), reactionNeedsLogin: false })
      wx.showToast({ title: '评价成功', icon: 'success' })
    } catch (e) {
      if (e && (e.code === 401 || e.statusCode === 401)) {
        this.setData({ reactionNeedsLogin: true })
        wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
      } else if (!(e && e.msg)) {
        wx.showToast({ title: '评价提交失败，请重试', icon: 'none' })
      }
    } finally {
      this.setData({ reactionSubmitting: false })
    }
  },

  normalizeGallery(data) {
    let images = data
    if (data && !Array.isArray(data) && typeof data === 'object') {
      images = data.galleryImages || data.images || data.imageUrls || data.gallery || data.list || []
    }
    if (typeof images === 'string') {
      try {
        const parsed = JSON.parse(images)
        images = Array.isArray(parsed) ? parsed : images.split(',')
      } catch (e) {
        images = images.split(',')
      }
    }
    if (!Array.isArray(images)) images = []
    return images
      .slice()
      .sort((a, b) => {
        const aOrder = typeof a === 'object' && a ? (a.sortOrder ?? a.sort_order ?? 0) : 0
        const bOrder = typeof b === 'object' && b ? (b.sortOrder ?? b.sort_order ?? 0) : 0
        return aOrder - bOrder
      })
      .map(item => typeof item === 'string'
        ? item
        : (item && (item.imageUrl || item.image_url || item.url)))
      .filter(Boolean)
      .map(url => this.resolveImageUrl(url))
  },

  cacheRecentSchool(school) {
    const schoolId = Number(school && school.id)
    if (!Number.isInteger(schoolId) || schoolId <= 0) return
    const stored = wx.getStorageSync('recentSchoolCache')
    const schools = Array.isArray(stored)
      ? stored.filter(item => !(Number(item.id) === schoolId && String(item.schoolSource || 'international') === this.data.schoolSource))
      : []
    const summary = {
      id: schoolId,
      schoolSource: this.data.schoolSource,
      name: school.name || '',
      logoUrl: school.logoUrl || '',
      district: school.district || '',
      curriculum: school.curriculum || '',
      grades: school.grades || ''
    }
    wx.setStorageSync('recentSchoolCache', [summary, ...schools].slice(0, 20))
  },

  normalizeSchoolValue(value) {
    if (value === undefined || value === null) return ''
    const text = String(value).trim()
    if (!text || ['null', 'undefined', '暂无', '未知', '待更新', '待确认'].includes(text.toLowerCase())) return ''
    return text
  },

  parseCurriculumItems(value) {
    const text = this.normalizeSchoolValue(value)
    if (!text) return []
    return text
      .split(/[,，、;；]+/)
      .map(item => item.trim())
      .filter(Boolean)
  },

  formatCampusArea(value) {
    const text = this.normalizeSchoolValue(value)
    if (!text) return ''
    if (/平方米|㎡/.test(text)) return text
    const number = Number(text)
    return Number.isFinite(number) ? `${Number(number.toFixed(2))} 平方米` : text
  },

  formatTeacherSeniorRatio(value) {
    const text = this.normalizeSchoolValue(value)
    if (!text) return ''
    if (text.includes('%')) return text
    const number = Number(text)
    if (!Number.isFinite(number)) return text
    const percentage = number <= 1 ? number * 100 : number
    return `${Number(percentage.toFixed(2))}%`
  },

  formatTeacherStudentRatio(value, studentCount, teacherCount) {
    const text = this.normalizeSchoolValue(value)
    if (text) return text
    const students = Number(studentCount)
    const teachers = Number(teacherCount)
    if (!Number.isFinite(students) || !Number.isFinite(teachers) || students <= 0 || teachers <= 0) return ''
    return `1:${Number((students / teachers).toFixed(1))}`
  },

  formatTeacherCount(value) {
    const text = this.normalizeSchoolValue(value)
    if (!text) return ''
    return /人$/.test(text) ? text : `${text} 人`
  },

  resolveImageUrl(url) {
    return require('../../utils/image').resolveImageUrl(url)
  },

  onPreviewImage(e) {
    const current = e.currentTarget.dataset.src
    wx.previewImage({
      current,
      urls: this.data.school.galleryImages || []
    })
  },

  async checkFav(schoolId) {
    try {
      const data = await request('/api/favorite/list', 'GET', {}, true)
      const favs = data || []
      this.setData({ isFav: favs.some(f => Number(f.schoolId) === Number(schoolId) && String(f.schoolSource || 'international') === this.data.schoolSource) })
    } catch (e) {}
  },

  onTab(e) {
    const activeTab = Number(e.currentTarget.dataset.i)
    const targetId = activeTab === 2 ? '#school-detail-section' : '#key-info-section'
    this.setData({ activeTab }, () => {
      wx.createSelectorQuery()
        .select(targetId)
        .boundingClientRect()
        .selectViewport()
        .scrollOffset()
        .exec(result => {
          const rect = result && result[0]
          const viewport = result && result[1]
          if (!rect || !viewport) return
          wx.pageScrollTo({
            scrollTop: Math.max(0, viewport.scrollTop + rect.top - 16),
            duration: 300
          })
        })
    })
  },

  async onToggleFav() {
    try {
      await request('/api/favorite/toggle', 'POST', { schoolSource: this.data.schoolSource, schoolId: Number(this.data.school.id) }, true)
      this.setData({ isFav: !this.data.isFav })
      wx.showToast({ title: this.data.isFav ? '已收藏' : '已取消', icon: 'success' })
    } catch (e) {}
  },

  onParentChat() {
    this.setData({
      showQr: true,
      qrImageUrl: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/lead-materials/jiazhang.png',
      qrImageType: 'parent'
    })
  },

  onCustomPlan() {
    this.setData({
      showQr: true,
      qrImageUrl: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/lead-materials/dingzhi1.png',
      qrImageType: 'custom-plan'
    })
  },

  onExamMaterials() {
    this.setData({
      showQr: true,
      qrImageUrl: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/lead-materials/guojixuexiao.png',
      qrImageType: 'exam-materials'
    })
  },

  closeQr() {
    this.setData({ showQr: false })
  },

  onLead() {
    this.setData({ showLead: true, parentName: '', childGrade: '', phone: '' })
  },

  onAdmissions() {
    if (!this.data.hasAdmissionContent || !this.data.school.id) return
    wx.navigateTo({ url: '/pages/admission-detail/admission-detail?id=' + this.data.school.id })
  },

  closeLead() {
    this.setData({ showLead: false })
  },

  onLeadInput(e) {
    this.setData({ [e.currentTarget.dataset.key]: e.detail.value })
  },

  onGradeChange(e) {
    const childGrade = this.data.gradeOptions[Number(e.detail.value)] || ''
    this.setData({ childGrade })
  },

  async onLeadSubmit() {
    const { parentName, childGrade, targetSchool, phone } = this.data
    if (!parentName || !childGrade || !phone) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' })
      return
    }
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '手机号格式错误', icon: 'none' })
      return
    }
    try {
      await request('/api/lead/submit', 'POST', {
        parent_name: parentName,
        child_grade: childGrade,
        target_school: targetSchool,
        parent_phone: phone
      }, true)
      wx.showToast({ title: '提交成功，老师正在路上~', icon: 'none', duration: 2000 })
      this.setData({ showLead: false })
    } catch (e) {
      if (e && e.code === 401) {
        wx.showToast({ title: '请先登录后再预约', icon: 'none' })
      }
    }
  }
})
