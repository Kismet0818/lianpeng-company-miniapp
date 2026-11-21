const { request } = require('../../utils/request')
const { startSchoolView, finishSchoolView } = require('../../utils/activity')
const { resolveImageUrl } = require('../../utils/image')

Page({
  data: {
    loading: true,
    error: '',
    school: null,
    galleryImages: [],
    heroImages: [],
    currentHero: 0,
    blessings: [],
    blessingTotal: 0,
    schoolId: 0,
    schoolSource: '',
    isFav: false,
    showLead: false,
    showQr: false,
    qrImageUrl: '',
    qrImageType: '',
    parentName: '',
    childGrade: '',
    phone: '',
    gradeOptions: ['幼儿园', '小学一年级', '小学二年级', '小学三年级', '小学四年级', '小学五年级', '小学六年级', '初中一年级', '初中二年级', '初中三年级', '高中一年级', '高中二年级', '高中三年级'],
    isDistrictKeySchool: false,
    isGeneralHighSchool: false,
    isJuniorHighSchool: false,
    isKindergarten: false,
    hideEmptySchoolFields: false,
    isUnifiedAdmissionSchool: false,
    hasBoardingInfo: false,
    hasTuitionInfo: false,
    hasAddress: false,
    hasDescription: false,
    hasWebsite: false,
    hasSchedule: false,
    hasTeachingFeatures: false,
    hasCampusArea: false,
    featureClasses: [],
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
    planTab: 'region',
    planRows: [],
    planGroups: [],
    planLoading: false,
    planLoaded: {},
    enrollSections: [],
    duikouAdmissionYear: 0,
    duikouPrimarySchools: []
  },

  onLoad(options) {
    options = options || {}
    const id = Number(options.id)
    const schoolSource = this.resolveSchoolSource(options)
    startSchoolView(this, schoolSource, options.id)
    if (!id) {
      this.setData({ loading: false, error: '学校参数无效' })
      return
    }
    const isPrimarySchool = schoolSource === 'primary'
    const isGeneralHighSchool = schoolSource === 'general'
    const isJuniorHighSchool = schoolSource === 'junior'
    const isKindergarten = schoolSource === 'kindergarten'
    this.setData({
      schoolId: id,
      schoolSource,
      isDistrictKeySchool: options.level === 'district',
      isGeneralHighSchool,
      isPrimarySchool,
      isJuniorHighSchool,
      isKindergarten,
      hideEmptySchoolFields: isPrimarySchool || isGeneralHighSchool || isJuniorHighSchool || isKindergarten,
      isUnifiedAdmissionSchool: options.level === 'district' || isGeneralHighSchool
    })
    this.loadSchool(id)
    this.loadBlessings()
  },

  resolveSchoolSource(options) {
    const VALID_SOURCES = ['international', 'score', 'junior', 'general', 'primary', 'kindergarten']
    const explicit = String((options && options.schoolSource) || '')
    if (VALID_SOURCES.indexOf(explicit) >= 0) return explicit
    const level = String((options && options.level) || '')
    if (level === 'general') return 'general'
    if (level === 'junior') return 'junior'
    return 'score'
  },

  onHide() { finishSchoolView(this) },
  onUnload() { finishSchoolView(this) },

  async loadSchool(id) {
    try {
      const requestedGeneral = this.data.isGeneralHighSchool
      const requestedJunior = this.data.isJuniorHighSchool
      const requestedPrimary = this.data.isPrimarySchool
      const requestedKindergarten = this.data.isKindergarten
      const [data, imageData, featureClasses] = await Promise.all([
        requestedKindergarten
          ? request('/api/kindergartens/' + id)
          : (requestedPrimary
          ? request('/api/primary-schools/' + id)
          : (requestedJunior
          ? request('/api/junior-schools/' + id)
          : (requestedGeneral ? this.loadGeneralHighSchool(id) : request('/api/score-query/schools/' + id)))),
        requestedKindergarten || requestedPrimary || requestedGeneral || requestedJunior ? Promise.resolve([]) : request('/api/score-query/schools/' + id + '/images').catch(() => []),
        request('/api/school-feature-classes', 'GET', { schoolId: id, schoolSource: this.data.schoolSource }, false, true).catch(() => [])
      ])
      const min = data.tuitionMin !== undefined ? data.tuitionMin : data.tuition_min
      const max = data.tuitionMax !== undefined ? data.tuitionMax : data.tuition_max
      let tuitionText = '暂未公布'
      if (min !== null && min !== undefined && max !== null && max !== undefined) {
        tuitionText = Number(min) === Number(max) ? this.formatTuition(min) : this.formatTuition(min) + '～' + this.formatTuition(max)
      } else if (min !== null && min !== undefined) {
        tuitionText = this.formatTuition(min) + '起'
      }
      const galleryImages = this.normalizeImages(imageData)
      const backgroundImage = this.resolveImageUrl(data.backgroundImageUrl || data.background_image_url)
      const heroImages = galleryImages.length ? galleryImages : (backgroundImage ? [backgroundImage] : [])
      const boardingInfo = this.cleanOptionalText(data.boardingInfo || data.boarding_info)
      const address = this.cleanOptionalText(data.address)
      const description = this.cleanOptionalText(data.description)
      const website = this.cleanOptionalText(data.website || data.schoolWebsite || data.school_website || data.websiteUrl || data.website_url || data.officialWebsite || data.official_website)
      const schedule = this.cleanOptionalText(data.schedule || data.schoolSchedule || data.school_schedule || data.dailySchedule || data.daily_schedule || data.workRest || data.work_rest)
      const teachingFeatures = this.cleanOptionalText(data.teachingFeatures || data.teaching_features || data.features || data.schoolFeatures || data.school_features)
      const campusAreaRaw = this.cleanOptionalText(data.campusArea ?? data.campus_area ?? data.schoolArea ?? data.school_area)
      const campusArea = campusAreaRaw && /平方米|平米|㎡|m²/i.test(campusAreaRaw) ? campusAreaRaw : (campusAreaRaw ? campusAreaRaw + ' 平方米' : '')
      const hasTuitionInfo = (min !== null && min !== undefined && min !== '') || (max !== null && max !== undefined && max !== '')
      const school = Object.assign({}, data, {
        logo: this.resolveImageUrl(data.logo || data.logoUrl || data.logo_url || data.backgroundImageUrl),
        boardingInfo,
        address,
        description,
        website,
        schedule,
        teachingFeatures,
        campusArea,
        tuitionText,
        natureText: data.nature || data.schoolNature || data.school_nature || data.schoolType || data.school_type || '公办',
        // 高中详情接口中的 level=high 只是内部类型标识，不作为页面文案展示。
        levelText: requestedPrimary ? '小学' : (requestedJunior ? '初中' : (requestedGeneral ? '一般高中' : (/^high$/i.test(String(data.level || '').trim()) ? '' : (data.level || data.levelLabel || data.schoolLevel || data.schoolType || ''))))
      })
      const schoolTypeText = String(data.schoolType || data.school_type || data.nature || '').trim()
      // 后端接口只会为公办初中返回对口数据。前端同时兼容历史数据中
      // schoolType 的不同写法，避免因“公办初中/公办/公办学校”等文本差异不显示。
      const hasDuikouPayload = requestedJunior && Array.isArray(data.duikouPrimarySchools)
      const isPublicJuniorSchool = requestedJunior && !/民办|私立/.test(schoolTypeText) &&
        (/公办/.test(schoolTypeText) || hasDuikouPayload)
      const isDistrictKeySchool = this.data.isDistrictKeySchool || this.isDistrictKey(data)
      const isUnifiedAdmissionSchool = isDistrictKeySchool || requestedGeneral
      this.setData({
        school,
        galleryImages,
        heroImages,
        isDistrictKeySchool,
        isUnifiedAdmissionSchool,
        hasBoardingInfo: !!boardingInfo,
        hasTuitionInfo,
        hasAddress: !!address,
        hasDescription: !!description,
        hasWebsite: !!website,
        hasSchedule: !!schedule,
        hasTeachingFeatures: !!teachingFeatures,
        hasCampusArea: !!campusArea,
        featureClasses: this.normalizeFeatureClasses(featureClasses),
        planTab: requestedJunior ? 'juniorQuota' : (isUnifiedAdmissionSchool ? 'volunteer' : 'region'),
        isPublicJuniorSchool,
        duikouAdmissionYear: isPublicJuniorSchool ? Number(data.duikouAdmissionYear || 0) : 0,
        duikouPrimarySchools: isPublicJuniorSchool ? this.normalizeDuikouSchools(data.duikouPrimarySchools) : [],
        loading: false
      })
      this.cacheRecentSchool(school)
      this.checkFav(school.id)
      if (!requestedPrimary && !requestedKindergarten) {
        this.loadPlanData(requestedJunior ? 'juniorQuota' : (isUnifiedAdmissionSchool ? 'volunteer' : 'region'))
      }
      this.loadReactions(school.id)
      this.loadEnrollment(school.name)
    } catch (e) {
      this.setData({ loading: false, error: '学校信息加载失败' })
    }
  },

  normalizeDuikouSchools(list) {
    if (!Array.isArray(list)) return []
    return list.map(item => ({
      ...item,
      primarySchoolId: Number(item.primarySchoolId || 0),
      primarySchoolName: item.primarySchoolName || '小学名称待核实',
      canOpenPrimarySchool: Number(item.primarySchoolId || 0) > 0,
      relationTypeText: this.duikouRelationText(item.relationType),
      scopeText: item.scopeText || '',
      note: item.note || ''
    }))
  },

  normalizeFeatureClasses(list) {
    if (!Array.isArray(list)) return []
    return list.map(item => ({
      id: Number(item.id || 0),
      featureClass: String(item.featureClass || '').trim(),
      features: this.cleanOptionalText(item.features),
      classCount: this.cleanOptionalText(item.classCount),
      classAssignment: this.cleanOptionalText(item.classAssignment)
    })).filter(item => item.featureClass)
  },

  duikouRelationText(type) {
    return ({ direct: '明确对口', area_based: '按地段对应', lottery: '电脑派位', overall: '统筹安排' }[type] || '招生范围')
  },

  onDuikouPrimaryTap(e) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (!id) return
    wx.navigateTo({ url: `/pages/score-school-detail/score-school-detail?id=${id}&schoolSource=primary` })
  },

  // 民办小初招生摇号数据（体制内学校库同样展示，小学/初中按名称匹配；高中、幼儿园不展示）
  async loadEnrollment(name) {
    const src = this.data.schoolSource
    if (src === 'kindergarten' || src === 'general' || src === 'score') return
    if (!name) { this.setData({ enrollSections: [] }); return }
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
            campusText: this.normalizeEnrollText(r.campus) || '统招',
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
            count700Text: this.normalizeEnrollText(m.score700Count),
            keyRateText: this.formatLotteryRate(m.keyHighRate)
          }
          if (!metrics.maxScoreText && !metrics.count700Text && !metrics.keyRateText) metrics = null
        }
      }
      sections.push({ stage: stageName, years, metrics })
    }
    return sections
  },

  normalizeEnrollText(value) {
    if (value === undefined || value === null) return ''
    const text = String(value).trim()
    if (!text || ['null', 'undefined', '暂无', '未知', '待更新', '待确认'].includes(text.toLowerCase())) return ''
    return text
  },

  formatLotteryRate(value) {
    if (value === null || value === undefined || value === '') return ''
    const n = Number(value)
    if (!Number.isFinite(n)) return ''
    const pct = n <= 1 ? n * 100 : n
    return `${Number(pct.toFixed(2))}%`
  },

  cacheRecentSchool(school) {
    const schoolId = Number(school && (school.id || school.schoolId))
    const schoolSource = String(this.data.schoolSource || '').trim()
    if (!Number.isInteger(schoolId) || schoolId <= 0 || !schoolSource) return
    const key = schoolSource + ':' + schoolId
    const stored = wx.getStorageSync('recentSchoolCache')
    const schools = (Array.isArray(stored) ? stored : []).filter(item =>
      String(item.schoolSource || '') + ':' + Number(item.schoolId || item.id) !== key
    )
    const summary = {
      id: schoolId,
      schoolId,
      schoolSource,
      name: school.name || school.schoolName || '',
      logoUrl: school.logo || school.logoUrl || '',
      district: school.district || '',
      curriculum: school.curriculum || '',
      grades: school.grades || ''
    }
    wx.setStorageSync('recentSchoolCache', [summary, ...schools].slice(0, 20))
  },

  normalizeReactionData(data) {
    const counts = data && data.counts ? data.counts : {}
    const selected = Number(data && data.reactionType)
    return {
      selectedReaction: selected >= 1 && selected <= 5 ? selected : null,
      reactionItems: this.data.reactionItems.map(item => ({ ...item, count: Math.max(0, Number(counts[item.type]) || 0) }))
    }
  },

  async loadReactions(schoolId) {
    if (!wx.getStorageSync('token')) { this.setData({ reactionNeedsLogin: true }); return }
    this.setData({ reactionLoading: true, reactionNeedsLogin: false })
    const schoolSource = this.data.schoolSource
    try {
      const data = await request('/api/school-reactions/' + Number(schoolId) + '?schoolSource=' + schoolSource, 'GET', {}, true)
      this.setData({ ...this.normalizeReactionData(data) })
    } catch (e) {
      if (e && (e.code === 401 || e.statusCode === 401)) this.setData({ reactionNeedsLogin: true })
    } finally { this.setData({ reactionLoading: false }) }
  },

  async checkFav(schoolId) {
    try {
      const list = await request('/api/favorite/list', 'GET', {}, true)
      this.setData({ isFav: (list || []).some(item => Number(item.schoolId) === Number(schoolId) && String(item.schoolSource || '') === this.data.schoolSource) })
    } catch (e) {}
  },

  async onToggleFav() {
    try {
      await request('/api/favorite/toggle', 'POST', { schoolSource: this.data.schoolSource, schoolId: Number(this.data.schoolId) }, true)
      this.setData({ isFav: !this.data.isFav })
      wx.showToast({ title: this.data.isFav ? '已收藏' : '已取消', icon: 'success' })
    } catch (e) {}
  },

  onLead() {
    this.setData({ showLead: true, parentName: '', childGrade: '', phone: '' })
  },

  onCustomPlan() {
    this.setData({ showQr: true, qrImageUrl: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/lead-materials/dingzhi1.png', qrImageType: 'custom-plan' })
  },

  closeQr() { this.setData({ showQr: false }) },
  closeLead() { this.setData({ showLead: false }) },
  onLeadInput(e) { this.setData({ [e.currentTarget.dataset.key]: e.detail.value }) },
  onGradeChange(e) { this.setData({ childGrade: this.data.gradeOptions[Number(e.detail.value)] || '' }) },

  async onLeadSubmit() {
    const { parentName, childGrade, phone } = this.data
    if (!parentName || !childGrade || !phone) { wx.showToast({ title: '请填写完整信息', icon: 'none' }); return }
    if (!/^1\d{10}$/.test(phone)) { wx.showToast({ title: '手机号码格式错误', icon: 'none' }); return }
    try {
      await request('/api/lead/submit', 'POST', {
        parent_name: parentName,
        child_grade: childGrade,
        target_school: (this.data.school && this.data.school.name) || '',
        parent_phone: phone
      }, true)
      wx.showToast({ title: '提交成功，老师正在路上~', icon: 'none', duration: 2000 })
      this.setData({ showLead: false })
    } catch (e) {
      if (e && e.code === 401) wx.showToast({ title: '请先登录后再预约', icon: 'none' })
    }
  },

  async onReactionLogin() {
    try { await getApp().login(); await this.loadReactions(this.data.school.id) } catch (e) { wx.showToast({ title: '登录失败，请重试', icon: 'none' }) }
  },

  async onSelectReaction(e) {
    const reactionType = Number(e.currentTarget.dataset.type)
    if (!Number.isInteger(reactionType) || reactionType < 1 || reactionType > 5 || this.data.reactionSubmitting) return
    if (!wx.getStorageSync('token')) { this.setData({ reactionNeedsLogin: true }); return }
    const schoolSource = this.data.schoolSource
    this.setData({ reactionSubmitting: true })
    try {
      const data = await request('/api/school-reactions/' + Number(this.data.school.id), 'POST', {
        schoolSource,
        reactionType: Number(reactionType)
      }, true)
      this.setData({ ...this.normalizeReactionData(data), reactionNeedsLogin: false })
    } catch (e) {
      if (e && (e.code === 401 || e.statusCode === 401)) this.setData({ reactionNeedsLogin: true })
      else wx.showToast({ title: '评价提交失败，请重试', icon: 'none' })
    } finally { this.setData({ reactionSubmitting: false }) }
  },

  formatTuition(value) {
    const amount = Number(value)
    if (!Number.isFinite(amount)) return '暂未公布'
    return Number(amount.toFixed(2)) + ' 万元/年'
  },

  async loadGeneralHighSchool(id) {
    try {
      return await request('/api/general-high-schools/' + id, 'GET', {}, true)
    } catch (error) {
      if (!(error && error.code === 401)) throw error
      await getApp().login()
      return request('/api/general-high-schools/' + id, 'GET', {}, true)
    }
  },

  cleanOptionalText(value) {
    const text = String(value === null || value === undefined ? '' : value).trim()
    return /^(?:暂无|暂未公布|未公布|待补充|无|--|—)$/i.test(text) ? '' : text
  },

  isDistrictKey(school) {
    const typeText = [school.schoolType, school.school_type, school.level, school.levelLabel, school.schoolLevel]
      .filter(Boolean)
      .join(' ')
    return typeText.indexOf('区实验性示范性高中') >= 0 || typeText.indexOf('区重点') >= 0
  },

  normalizeImages(data) {
    const list = Array.isArray(data)
      ? data
      : (data && (data.list || data.records || data.images || data.data)) || []
    if (!Array.isArray(list)) return []
    return list
      .slice()
      .sort((a, b) => Number(a.sortOrder || a.sort_order || 0) - Number(b.sortOrder || b.sort_order || 0))
      .map(item => {
        const url = typeof item === 'string' ? item : item && (item.imageUrl || item.image_url || item.url)
        return this.resolveImageUrl(url)
      })
      .filter(Boolean)
  },

  resolveImageUrl(url) {
    return resolveImageUrl(url)
  },

  onHeroChange(e) {
    this.setData({ currentHero: e.detail.current })
  },

  onLogoError() {
    this.setData({ 'school.logo': '' })
  },

  onGalleryError(e) {
    const index = Number(e.currentTarget.dataset.index)
    if (!Number.isInteger(index)) return
    const galleryImages = this.data.galleryImages.filter((_, itemIndex) => itemIndex !== index)
    this.setData({ galleryImages, heroImages: galleryImages })
  },

  previewImage(e) {
    const current = e.currentTarget.dataset.src
    if (!current) return
    wx.previewImage({ current, urls: this.data.galleryImages })
  },

  async loadBlessings() {
    try {
      const schoolSource = this.data.schoolSource
      const schoolId = Number(this.data.schoolId)
      if (!schoolSource || !schoolId) return
      const data = await request('/api/blessing/list?page=1&size=3&schoolSource=' + schoolSource + '&schoolId=' + schoolId)
      const records = Array.isArray(data && data.records) ? data.records : []
      this.setData({
        blessings: records.map(item => {
          const nickname = item.nickname || '匿名用户'
          return { ...item, nickname, initial: nickname.slice(0, 1), avatar: resolveImageUrl(item.avatar) }
        }),
        blessingTotal: Number(data && data.total) || records.length
      })
    } catch (e) {
      console.warn('祝福列表加载失败：', e)
    }
  },

  openBlessingCompose() {
    const schoolSource = this.data.schoolSource
    const schoolId = Number(this.data.schoolId)
    const schoolName = this.data.school && this.data.school.name ? this.data.school.name : ''
    wx.navigateTo({
      url: '/pages/blessing-compose/blessing-compose?schoolSource=' + schoolSource + '&schoolId=' + schoolId + '&schoolName=' + encodeURIComponent(schoolName),
      success: res => {
        res.eventChannel.on('blessingPublished', () => this.loadBlessings())
      },
      fail: error => {
        console.error('打开祝福发布页失败：', error)
        wx.showToast({ title: '页面打开失败，请重新编译', icon: 'none' })
      }
    })
  },
  openAllBlessings() {
    const schoolSource = this.data.schoolSource
    const schoolId = Number(this.data.schoolId)
    const schoolName = this.data.school && this.data.school.name ? this.data.school.name : ''
    wx.navigateTo({ url: '/pages/blessing-list/blessing-list?schoolSource=' + schoolSource + '&schoolId=' + schoolId + '&schoolName=' + encodeURIComponent(schoolName) })
  },

  switchPlanTab(e) {
    const tab = e.currentTarget.dataset.tab
    if (!tab || tab === this.data.planTab) return
    const cachedRows = this.data.planLoaded[tab] || []
    this.setData({ planTab: tab, planRows: cachedRows, planGroups: tab === 'school' ? this.groupSchoolRows(cachedRows) : [] })
    if (!this.data.planLoaded[tab]) this.loadPlanData(tab)
  },

  async loadPlanData(tab) {
    if (this.data.planLoading || !this.data.school) return
    this.setData({ planLoading: true })
    try {
      const list = await this.requestPlanList(tab, '2025-2026')
      const rows = this.buildPlanRows(tab, list)
      const planLoaded = Object.assign({}, this.data.planLoaded, { [tab]: rows })
      const active = this.data.planTab === tab
      this.setData({
        planLoaded,
        planRows: active ? rows : this.data.planRows,
        planGroups: active && tab === 'school' ? this.groupSchoolRows(rows) : this.data.planGroups
      })
    } catch (e) {
      console.error('招生计划加载失败：', tab, e)
      wx.showToast({ title: '招生计划加载失败', icon: 'none' })
    } finally {
      this.setData({ planLoading: false })
      const activeTab = this.data.planTab
      if (activeTab !== tab && !this.data.planLoaded[activeTab]) {
        setTimeout(() => this.loadPlanData(activeTab), 0)
      }
    }
  },

  requestPlanList(tab, year) {
    const path = tab === 'region' ? '/api/score-query/region' : (tab === 'school' || tab === 'juniorQuota' ? '/api/score-query/school' : '/api/score-query/volunteer')
    const districtQuery = tab === 'juniorQuota' && this.data.school && this.data.school.district
      ? '&district=' + encodeURIComponent(this.data.school.district)
      : ''
    return request(path + '?year=' + encodeURIComponent(year) + districtQuery).then(data => Array.isArray(data) ? data : [])
  },

  buildPlanRows(tab, list) {
    const schoolId = Number(this.data.schoolId)
    const schoolName = this.normalizeSchoolName(this.data.school && this.data.school.name)
    const schoolShortName = this.normalizeSchoolName(this.data.school && (this.data.school.shortName || this.data.school.short_name))
    const rows = []
    list.forEach(item => {
      if (tab === 'juniorQuota') {
        const targetSchoolName = this.normalizeSchoolName(item.targetSchoolName)
        const matchedJunior = targetSchoolName === schoolName || targetSchoolName === schoolShortName ||
          (!!schoolShortName && targetSchoolName.endsWith(schoolShortName)) ||
          (!!targetSchoolName && schoolName.endsWith(targetSchoolName))
        if (!matchedJunior) return
        const plan = Number(item.quotaCount)
        const score = item.minScore === null || item.minScore === undefined ? '—' : Number(item.minScore)
        const hasChange = item.changeRange !== null && item.changeRange !== undefined && item.changeRange !== ''
        const change = Number(item.changeRange)
        rows.push({
          key: String(item.id || item.highSchoolName),
          district: item.highSchoolDistrict || '',
          label: item.highSchoolName || item.schoolName || '未注明高中',
          plan: Number.isFinite(plan) ? plan : '—',
          score: Number.isFinite(score) ? score : '—',
          changeText: hasChange && Number.isFinite(change) && change !== 0 ? Math.abs(change) : '',
          changeClass: change > 0 ? 'up' : (change < 0 ? 'down' : 'flat'),
          changeSymbol: hasChange ? (change > 0 ? '▲' : (change < 0 ? '▼' : '平')) : ''
        })
        return
      }
      if (tab === 'volunteer' && Number(item.moduleType) !== 1) return
      const itemSchoolName = this.normalizeSchoolName(item.schoolName || item.highSchoolName)
      const matched = tab === 'volunteer'
        ? (Number(item.highSchoolId) === schoolId || itemSchoolName === schoolName || (!!schoolShortName && itemSchoolName === schoolShortName))
        : Number(item.highSchoolId) === schoolId
      if (!matched) return
      const label = tab === 'region'
        ? (item.targetDistrict || '未注明区域')
        : (tab === 'school' ? (item.targetSchoolName || '未注明初中') : (item.targetDistrict || '未注明区域'))
      const plan = Number(tab === 'volunteer' ? item.planCount : item.quotaCount)
      const score = item.minScore === null || item.minScore === undefined ? '—' : Number(item.minScore)
      const hasChange = item.changeRange !== null && item.changeRange !== undefined && item.changeRange !== ''
      const change = Number(item.changeRange)
      rows.push({
        key: String(item.id || label),
        district: item.targetDistrict || '未注明区域',
        label,
        plan: Number.isFinite(plan) ? plan : '—',
        score: Number.isFinite(score) ? score : '—',
        changeText: hasChange && Number.isFinite(change) && change !== 0 ? Math.abs(change) : '',
        changeClass: change > 0 ? 'up' : (change < 0 ? 'down' : 'flat'),
        changeSymbol: hasChange ? (change > 0 ? '▲' : (change < 0 ? '▼' : '平')) : ''
      })
    })
    return rows
  },

  groupSchoolRows(rows) {
    const groups = []
    const groupMap = {}
    rows.forEach(row => {
      const district = row.district || '未注明区域'
      if (!groupMap[district]) {
        groupMap[district] = { district, items: [] }
        groups.push(groupMap[district])
      }
      groupMap[district].items.push(row)
    })
    return groups
  },

  normalizeSchoolName(value) {
    return String(value || '').replace(/^上海市/, '').replace(/\s+/g, '')
  },

  openDistrictQuery(e) {
    const mode = e.currentTarget.dataset.mode
    const district = String(this.data.school && this.data.school.district || '').trim()
    if (!district || (mode !== 'volunteer' && mode !== 'region')) {
      wx.showToast({ title: '未获取到学校所在区', icon: 'none' })
      return
    }
    const url = '/pages/quota-query/quota-query?mode=' + mode + '&district=' + encodeURIComponent(district)
    wx.navigateTo({
      url,
      fail: error => {
        console.error('初中详情查询页跳转失败，尝试重定向：', error)
        wx.redirectTo({
          url,
          fail: redirectError => {
            console.error('初中详情查询页重定向失败：', redirectError)
            wx.showToast({ title: '页面打开失败，请重新编译', icon: 'none' })
          }
        })
      }
    })
  }
})
