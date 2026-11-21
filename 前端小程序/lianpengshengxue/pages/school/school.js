const { request } = require('../../utils/request')
const app = getApp()
const ACTIVE_ADMISSION_SCHOOL_IDS = new Set([17, 39, 92, 76, 73, 45, 105, 81, 36, 47, 4, 6])

// 体制内高中优先展示“四校八大”。只按学校正式名称匹配，避免将分校误排到主校位置。
const TOP_HIGH_SCHOOL_ORDER = new Map([
  ['上海市上海中学', 0],
  ['上海中学', 0],
  ['华东师范大学第二附属中学', 1],
  ['复旦大学附属中学', 2],
  ['上海交通大学附属中学', 3],
  ['上海市南洋模范中学', 4],
  ['上海市建平中学', 5],
  ['上海市七宝中学', 6],
  ['上海市控江中学', 7],
  ['上海市大同中学', 8],
  ['上海市延安中学', 9],
  ['上海市复兴高级中学', 10],
  ['上海市复兴中学', 10],
  ['复旦大学附属复兴中学', 10],
  ['上海市格致中学', 11]
])

Page({
  data: {
    schools: [],
    schoolSystem: 'domestic',
    schoolSystemOptions: [{ key: 'domestic', label: '体制内' }, { key: 'international', label: '体制外' }],
    page: 1,
    total: 0,
    district: '浦东新区',
    grades: '高中',
    tuitionLabel: '',
    tuitionMin: '',
    tuitionMax: '',
    schoolNature: '',
    boardingType: '',
    draftSchoolNature: '',
    draftBoardingType: '',
    showMoreFilter: false,
    activeQuickFilter: '',
    searchName: '',
    showLead: false,
    studentName: '',
    phone: '',
    childGrade: '',
    targetSchool: '',
    statusBarHeight: 20,
    navBarHeight: 88,
    gradeOptions: ['幼儿园', '小学一年级', '小学二年级', '小学三年级', '小学四年级', '小学五年级', '小学六年级', '初中一年级', '初中二年级', '初中三年级', '高中一年级', '高中二年级', '高中三年级'],
    districts: ['全部','浦东新区','徐汇区','长宁区','闵行区','杨浦区','黄浦区','松江区','宝山区','嘉定区','金山区','静安区','青浦区','普陀区','虹口区','崇明区','奉贤区'],
    displayDistricts: ['浦东新区','徐汇区','长宁区','闵行区','杨浦区','黄浦区','松江区','宝山区','嘉定区','金山区','静安区','青浦区','普陀区','虹口区','崇明区','奉贤区'],
    gradesList: ['高中','初中','小学','幼儿园'],
    tuitionOptions: [
      { label: '不限学费', min: '', max: '' },
      { label: '10万以下', min: '', max: 10 },
      { label: '10-20万', min: 10, max: 20 },
      { label: '20-30万', min: 20, max: 30 },
      { label: '30万以上', min: 30, max: '' }
    ],
    schoolNatureOptions: ['公办', '民办', '外籍/国际部'],
    boardingTypeOptions: ['可寄宿', '可走读']
  },

  onLoad() {
    let navBarHeight = 88
    let statusBarHeight = 20
    try {
      const menu = wx.getMenuButtonBoundingClientRect()
      const windowInfo = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
      statusBarHeight = windowInfo.statusBarHeight || 20
      navBarHeight = menu.bottom + 8
    } catch (e) {}
    const presetGrade = app.globalData.presetGrade || wx.getStorageSync('schoolPresetGrade') || ''
    app.globalData.presetGrade = null
    wx.removeStorageSync('schoolPresetGrade')
    this.setData({
      statusBarHeight,
      navBarHeight,
      grades: presetGrade || '高中',
      schoolSystem: presetGrade === '幼儿园' ? 'domestic' : this.data.schoolSystem,
      schoolNatureOptions: presetGrade === '幼儿园' ? ['公办', '民办'] : this.data.schoolNatureOptions,
      schoolNature: presetGrade === '幼儿园' ? '' : this.data.schoolNature,
      draftSchoolNature: presetGrade === '幼儿园' ? '' : this.data.draftSchoolNature
    })
    this.loadList()
  },

  onShow() {
    app.globalData.selectedTab = 1
    const tabBar = this.getTabBar && this.getTabBar()
    if (tabBar) tabBar.setData({ selected: 1 })
    wx.nextTick(() => {
      const currentTabBar = this.getTabBar && this.getTabBar()
      if (currentTabBar) currentTabBar.setData({ selected: 1 })
    })
    const preset = app.globalData.presetDistrict
    if (preset && this.data.district !== preset) {
      app.globalData.presetDistrict = null
      this.setData({ district: preset, page: 1, schools: [] })
      this.loadList()
    }
    const presetGrade = app.globalData.presetGrade || wx.getStorageSync('schoolPresetGrade')
    if (presetGrade) {
      app.globalData.presetGrade = null
      wx.removeStorageSync('schoolPresetGrade')
      if (this.data.grades !== presetGrade) {
        this.setData({
          grades: presetGrade,
          schoolSystem: presetGrade === '幼儿园' ? 'domestic' : this.data.schoolSystem,
          schoolNatureOptions: presetGrade === '幼儿园' ? ['公办', '民办'] : this.data.schoolNatureOptions,
          schoolNature: presetGrade === '幼儿园' ? '' : this.data.schoolNature,
          draftSchoolNature: presetGrade === '幼儿园' ? '' : this.data.draftSchoolNature,
          page: 1,
          schools: []
        })
        this.loadList()
      }
    }
    // Tab 页面会被微信缓存，重新进入时也要对现有体制外列表应用最新排序规则。
    if (this.data.schoolSystem === 'international' && this.data.schools.length) {
      this.setData({ schools: this.sortInternationalRecords(this.data.schools) })
    }
  },

  async loadList() {
    if (this.data.schoolSystem === 'domestic') {
      return this.loadDomesticList()
    }
    const requestPage = this.data.page
    if (requestPage > 1 && this._listLoading) return

    const requestId = (this._schoolRequestId || 0) + 1
    const queryKey = this.getSchoolQueryKey()
    this._schoolRequestId = requestId
    this._listLoading = true
    let shouldAutoLoadNext = false

    try {
      // 体制外学段支持“初中、高中”这类复合字段，不能让后端按整字段过滤。
      // 拉取体制外高中数据后由前端按包含关系筛选；体制内逻辑保持不变。
      const params = { page: requestPage, size: this.data.schoolSystem === 'international' ? 500 : 10 }
      if (this.data.searchName) params.name = this.data.searchName
      if (this.data.district && this.data.district !== '全部') params.district = this.data.district
      if (this.data.grades && this.data.grades !== '全部' && this.data.schoolSystem !== 'international') params.grades = this.data.grades
      if (this.data.tuitionMin !== '') params.tuitionMin = this.data.tuitionMin
      if (this.data.tuitionMax !== '') params.tuitionMax = this.data.tuitionMax
      if (this.data.schoolNature) params.schoolNature = this.data.schoolNature
      const data = await request('/api/schools/list', 'GET', params)
      // 筛选条件改变后，丢弃此前尚未结束的旧请求，避免未筛选数据混入新列表。
      if (requestId !== this._schoolRequestId || queryKey !== this.getSchoolQueryKey()) return

      const rawRecords = Array.isArray(data.records) ? data.records : []
      const matchedRecords = this.filterSchoolRecords(rawRecords)
      const records = matchedRecords.map(item => ({
        ...item,
        logoUrl: this.resolveImageUrl(item.logoUrl),
        displayFeatureTags: this.parseFeatureTags(item.featureTags),
        compareKey: 'international:' + item.id,
        system: 'international',
        schoolSource: 'international',
        hasAdmissionContent: ACTIVE_ADMISSION_SCHOOL_IDS.has(Number(item.id))
      }))
      // 每次都对合并后的完整列表排序，避免分页追加后双语学校仍留在列表后方。
      const mergedRecords = this.sortInternationalRecords(
        requestPage === 1 ? records : [...this.data.schools, ...records]
      )
      const serverTotal = Number(data.total) || 0
      const requestSize = this.data.schoolSystem === 'international' ? 500 : 10
      this._serverHasMore = rawRecords.length >= requestSize && requestPage * requestSize < serverTotal
      shouldAutoLoadNext = Boolean(this.data.boardingType) && mergedRecords.length < 10 && this._serverHasMore
      this.setData({
        schools: mergedRecords,
        // 体制外学段和寄宿条件都由前端筛选，页面总数必须使用筛选后的数量。
        // 后端 total 是当前地区的全部学校数，直接展示会造成“总数 61、列表仅 31 条”的错觉。
        total: this.data.boardingType || this.isInternationalGradeFiltered()
          ? mergedRecords.length
          : data.total
      })
    } catch (e) {
    } finally {
      if (requestId === this._schoolRequestId) {
        this._listLoading = false
        if (shouldAutoLoadNext && queryKey === this.getSchoolQueryKey()) {
          this.setData({ page: requestPage + 1 })
          this.loadList()
        }
      }
    }
  },

  async loadDomesticList() {
    const requestId = (this._schoolRequestId || 0) + 1
    this._schoolRequestId = requestId
    this._listLoading = true
    try {
      const district = this.data.district || '浦东新区'
      if (!this._domesticSchools) this._domesticSchools = {}
      if (!this._domesticSchools[district]) {
        this._domesticSchools[district] = await this.fetchDomesticSchools(district)
      }
      if (requestId !== this._schoolRequestId) return
      const keyword = String(this.data.searchName || '').trim().toLowerCase()
      const KIND_ORDER = { municipal: 0, district: 1, general: 2, junior: 3, primary: 4, kindergarten: 5 }
      const schools = this._domesticSchools[district].filter(item => {
        // 后端多个体制内接口的 district 参数可能未生效，合并后必须以前端地区字段再次兜底过滤。
        // “全部”不做地区限制，其余地区要求精确匹配，避免跨区学校混入列表。
        if (district !== '全部' && String(item.district || '').trim() !== district) return false
        if (keyword && String(item.name).toLowerCase().indexOf(keyword) < 0) return false
        const wanted = this.data.grades
        if (wanted === '高中' && item.kind === 'junior') return false
        if (wanted === '初中' && item.kind !== 'junior') return false
        if (wanted === '小学' && item.kind !== 'primary') return false
        if (wanted === '幼儿园' && item.kind !== 'kindergarten') return false
        if (wanted !== '小学' && wanted !== '幼儿园' && (item.kind === 'primary' || item.kind === 'kindergarten')) return false
        const min = item.tuitionMin == null || item.tuitionMin === '' ? null : Number(item.tuitionMin)
        const max = item.tuitionMax == null || item.tuitionMax === '' ? min : Number(item.tuitionMax)
        if (this.data.tuitionMin !== '' && (max == null || max < Number(this.data.tuitionMin))) return false
        if (this.data.tuitionMax !== '' && (min == null || min > Number(this.data.tuitionMax))) return false
        return this.matchesSchoolNature(item, this.data.schoolNature) && this.matchesBoardingType(item, this.data.boardingType)
      }).sort((a, b) => {
        if (this.data.grades === '高中') {
          const topRankA = TOP_HIGH_SCHOOL_ORDER.get(String(a.name || '').trim()) ?? 99
          const topRankB = TOP_HIGH_SCHOOL_ORDER.get(String(b.name || '').trim()) ?? 99
          if (topRankA !== topRankB) return topRankA - topRankB
        }
        const TIER_ORDER = { '一梯队': 0, '二梯队': 1, '三梯队': 2, '四梯队': 3 }
        const rankTier = item => {
          if (item.kind === 'kindergarten') return null
          const text = String(item.tier || '').trim()
          return text in TIER_ORDER ? TIER_ORDER[text] : 99
        }
        const ra = rankTier(a)
        const rb = rankTier(b)
        if (ra !== null && rb !== null && ra !== rb) return ra - rb
        // 幼儿园单独按等级排序，其余小学/初中/高中均按梯队排。
        if (ra === null && rb === null) {
          const levelRank = item => {
            if (item.kind !== 'kindergarten') return null
            const text = String(item.kindergartenLevel || '').trim()
            if (text.includes('示范')) return 0
            if (text.includes('一级')) return 1
            if (text.includes('二级')) return 2
            if (text.includes('三级')) return 3
            return 99
          }
          const la = levelRank(a)
          const lb = levelRank(b)
          if (la !== null && lb !== null && la !== lb) return la - lb
        }
        const ka = KIND_ORDER[a.kind] ?? 99
        const kb = KIND_ORDER[b.kind] ?? 99
        if (ka !== kb) return ka - kb
        return String(a.name).localeCompare(String(b.name), 'zh')
      })
      this._serverHasMore = false
      this.setData({ schools, total: schools.length })
    } catch (e) {
      wx.showToast({ title: '体制内学校加载失败', icon: 'none' })
    } finally {
      if (requestId === this._schoolRequestId) this._listLoading = false
    }
  },

  async fetchDomesticSchools(district) {
    const safe = promise => promise.catch(() => [])
    const [regionSchools, districtKeySchools, generalSchools, juniorSchools, primarySchools, kindergartenSchools] = await Promise.all([
      safe(request('/api/score-query/region', 'GET', { year: '2025-2026' })),
      safe(request('/api/score-query/schools/district-key', 'GET', { district }, false)),
      safe(request('/api/general-high-schools', 'GET', { page: 1, size: 500, district }, false)),
      safe(request('/api/junior-schools', 'GET', { district }, false)),
      safe(request('/api/primary-schools', 'GET', { page: 1, size: 500, district }, false)),
      safe(request('/api/kindergartens', 'GET', { page: 1, size: 500, district }, false))
    ])
    const result = []
    const seen = new Set()
    const push = (raw, meta) => {
      const id = Number(raw.highSchoolId || raw.high_school_id || raw.id) || 0
      const name = String(raw.name || raw.schoolName || raw.highSchoolName || '').trim()
      if (!id || !name) return
      const compareKey = 'domestic:' + meta.kind + ':' + id
      if (seen.has(compareKey)) return
      seen.add(compareKey)
      const tuitionMin = raw.tuitionMin ?? raw.tuition_min
      const tuitionMax = raw.tuitionMax ?? raw.tuition_max
      const shortName = raw.shortName || raw.short_name || name.replace(/^上海市/, '')
      const sourceLogoUrl = raw.logoUrl || raw.logo_url || raw.logo || raw.backgroundImageUrl
      // 幼儿园没有 score-query 的校徽资源，不能回退到高中校徽地址。
      const logoUrl = sourceLogoUrl
        ? this.resolveImageUrl(sourceLogoUrl)
        : (meta.kind === 'kindergarten' ? '' : this.resolveImageUrl('score-query/school-logo/' + id + '.jpg'))
      result.push({
        ...raw, id, name, shortName, logoUrl, compareKey, system: 'domestic', kind: meta.kind,
        schoolSource: this.sourceByKind(meta.kind),
        district: raw.district || raw.highSchoolDistrict || meta.district || '',
        level: meta.level || '', grades: meta.kind === 'junior' ? '初中' : (meta.kind === 'primary' ? '小学' : (meta.kind === 'kindergarten' ? '幼儿园' : '高中')),
        schoolType: raw.schoolType || raw.school_type || raw.nature || '',
        boardingInfo: raw.boardingInfo || raw.boarding_info || '', tuitionMin, tuitionMax,
        tuitionDisplay: this.formatTuition(tuitionMin, tuitionMax),
        boardingDisplay: this.formatBoarding(raw.boardingInfo || raw.boarding_info),
        natureDisplay: this.formatNature(raw.schoolType || raw.school_type || raw.nature),
        kindergartenLevel: raw.kindergartenLevel || '',
        careType: raw.careType || '',
        ageRange: raw.ageRange || '',
        tier: raw.tier == null ? null : String(raw.tier).trim(),
        tierDisplay: meta.kind === 'primary' && raw.tier ? ' · ' + String(raw.tier).trim() : ''
      })
    }
    const scoreItems = []
    const municipalSeen = new Set()
    ;(Array.isArray(regionSchools) ? regionSchools : []).forEach(raw => {
      const schoolId = Number(raw.highSchoolId || raw.high_school_id)
      if (!schoolId || municipalSeen.has(schoolId)) return
      const schoolDistrict = String(raw.highSchoolDistrict || '').trim()
      if (schoolDistrict !== district) return
      municipalSeen.add(schoolId)
      scoreItems.push({ raw, meta: { kind: 'municipal', level: '市重点', district: schoolDistrict } })
    })
    const unwrap = value => Array.isArray(value) ? value : (value && (value.records || value.list || value.content)) || []
    unwrap(districtKeySchools).forEach(raw => scoreItems.push({ raw, meta: { kind: 'district', level: '区重点', district } }))
    unwrap(generalSchools).forEach(raw => push(raw, { kind: 'general', level: '一般高中', district }))
    unwrap(juniorSchools).forEach(raw => push(raw, { kind: 'junior', level: '', district }))
    unwrap(primarySchools).forEach(raw => push(raw, { kind: 'primary', level: '小学', district }))
    unwrap(kindergartenSchools).forEach(raw => push(raw, { kind: 'kindergarten', level: '幼儿园', district }))
    const scoreDetails = await this.loadDomesticScoreDetails(scoreItems)
    scoreItems.forEach(({ raw, meta }) => {
      const schoolId = Number(raw.highSchoolId || raw.high_school_id || raw.id)
      push({ ...raw, ...(scoreDetails[schoolId] || {}) }, meta)
    })
    return result
  },

  async loadDomesticScoreDetails(items) {
    const ids = Array.from(new Set(items.map(({ raw }) => Number(raw.highSchoolId || raw.high_school_id || raw.id)).filter(Boolean)))
    const details = {}
    let cursor = 0
    const worker = async () => {
      while (cursor < ids.length) {
        const id = ids[cursor++]
        try {
          details[id] = await request('/api/score-query/schools/' + id)
        } catch (e) {}
      }
    }
    await Promise.all(Array.from({ length: Math.min(8, ids.length) }, worker))
    return details
  },

  onGradeStatTap(e) {
    const grade = String(e.currentTarget.dataset.grade || '').trim()
    if (['小学', '初中', '高中', '幼儿园'].indexOf(grade) < 0) return
    const system = e.currentTarget.dataset.system === 'international' ? 'international' : 'domestic'
    if (grade === this.data.grades && system === this.data.schoolSystem) return
    this.setData({
      grades: grade,
      schoolSystem: system,
      schoolNatureOptions: system === 'domestic' ? ['公办', '民办'] : ['公办', '民办', '外籍/国际部'],
      schoolNature: '',
      draftSchoolNature: '',
      page: 1,
      schools: [],
      total: 0
    })
    this.loadList()
  },

  onRanking() {
    wx.navigateTo({ url: '/pages/school-ranking/index' })
  },

  onScoreQuery() {
    wx.navigateTo({ url: '/pages/score-tool/score-tool' })
  },

  onLogoError(e) {
    const index = Number(e.currentTarget.dataset.index)
    const key = 'schools[' + index + '].logoUrl'
    this.setData({ [key]: '' })
  },

  formatTuition(min, max) {
    if (min === undefined || min === null || min === '') min = null
    if (max === undefined || max === null || max === '') max = null
    if (min != null && max != null) return Number(min) === Number(max) ? min + '万/年' : min + '-' + max + '万/年'
    if (min != null) return min + '万/年起'
    if (max != null) return max + '万/年'
    return '---'
  },

  formatBoarding(value) {
    const text = String(value || '').trim()
    if (!text) return '---'
    if (/全寄宿|全部寄宿|全住宿/.test(text)) return '全寄宿'
    if (/无寄宿|无住宿|不提供寄宿|不可寄宿|走读|^否$/.test(text)) return '无寄宿'
    if (/部分寄宿|寄宿|住宿|^是$/.test(text)) return '部分寄宿'
    return text
  },

  formatNature(value) {
    const text = String(value || '').trim()
    if (!text) return '---'
    if (/民办|私立/.test(text)) return '民办'
    if (/公办|公立/.test(text)) return '公办'
    return text
  },

  getBilingualPriority(item) {
    if (!item) return 2
    // 校名明确包含“双语”最符合用户认知，必须排在仅课程或授课方式含“双语”的学校之前。
    const schoolName = [item.name, item.shortName, item.short_name].filter(Boolean).join(' ')
    if (/双语/i.test(schoolName)) return 0
    const schoolInfo = [
      item.schoolType, item.school_type, item.nature,
      item.curriculum, item.teachingMode, item.teaching_mode,
      item.featureTags, item.feature_tags
    ].filter(Boolean).join(' ')
    return /双语|bilingual/i.test(schoolInfo) ? 1 : 2
  },

  onSystemChange(e) {
    const schoolSystem = e.currentTarget.dataset.system
    if (!schoolSystem || schoolSystem === this.data.schoolSystem) return
    this.setData({
      schoolSystem,
      page: 1, schools: [], total: 0,
      schoolNatureOptions: schoolSystem === 'domestic' ? ['公办', '民办'] : ['公办', '民办', '外籍/国际部']
    })
    this.loadList()
  },

  sourceByKind(kind) {
    if (kind === 'primary') return 'primary'
    if (kind === 'kindergarten') return 'kindergarten'
    if (kind === 'junior') return 'junior'
    if (kind === 'general') return 'general'
    return 'score'
  },

  getSchoolQueryKey() {
    return JSON.stringify([
      this.data.searchName,
      this.data.district,
      this.data.grades,
      this.data.tuitionMin,
      this.data.tuitionMax,
      this.data.schoolNature,
      this.data.boardingType
    ])
  },

  parseFeatureTags(value) {
    if (value === undefined || value === null) return []
    return [...new Set(
      String(value)
        .split(/[,，]+/)
        .map(tag => tag.trim())
        .filter(Boolean)
    )].slice(0, 3)
  },

  filterSchoolRecords(records) {
    return records.filter(item => {
      if (this.data.schoolSystem === 'international' && this.data.grades && this.data.grades !== '全部') {
        const grades = String(item.grades || item.grade || '').replace(/\s+/g, '')
        if (grades.indexOf(this.data.grades) < 0) return false
      }
      return this.matchesSchoolNature(item, this.data.schoolNature) &&
        this.matchesBoardingType(item, this.data.boardingType)
    })
  },

  sortInternationalRecords(records) {
    return records
      .map((item, index) => ({ item, index }))
      .sort((a, b) => {
        // 校名含“双语” > 课程/授课信息含“双语” > 其他体制外学校。
        const bilingualDiff = this.getBilingualPriority(a.item) - this.getBilingualPriority(b.item)
        if (bilingualDiff) return bilingualDiff
        const priorityDiff = this.getInternationalSchoolTypePriority(a.item) -
          this.getInternationalSchoolTypePriority(b.item)
        return priorityDiff || a.index - b.index
      })
      .map(entry => entry.item)
  },

  getInternationalSchoolTypePriority(item) {
    // 榜单/学校类型字段可能不完整，名称中明确包含“民办”的学校优先置顶。
    if (/民办/.test(String(item.name || '').trim())) return 0
    const schoolType = String(item.school_type || item.schoolType || '').trim()
    if (/民办|私立/.test(schoolType) && !/外籍人员子女学校|国际课程班/.test(schoolType)) return 0
    if (/外籍人员子女学校|外籍子女/.test(schoolType)) return 3
    if (/国际部|国际课程班/.test(schoolType)) return 2
    return 1
  },

  isInternationalGradeFiltered() {
    return this.data.schoolSystem === 'international' &&
      Boolean(this.data.grades) &&
      this.data.grades !== '全部'
  },

  matchesSchoolNature(item, selected) {
    if (!selected) return true
    const natureText = this.collectSchoolFieldText(item, [
      'schoolNature', 'schoolNatureName', 'nature', 'natureName',
      'schoolType', 'schoolTypeName', 'type'
    ])
    const nameText = String(item.name || '')
    const text = natureText + ' ' + nameText

    if (selected === '外籍/国际部') {
      return /外籍|外国人员子女|国际部/.test(text)
    }
    if (selected === '民办') {
      return /民办|私立/.test(text) && !/外籍|外国人员子女|国际部/.test(text)
    }
    if (selected === '公办') {
      return /公办|公立/.test(text) && !/外籍|外国人员子女|国际部/.test(text)
    }
    return natureText.indexOf(selected) >= 0
  },

  matchesBoardingType(item, selected) {
    if (!selected) return true
    const value = item.boardingInfo !== undefined ? item.boardingInfo : item.boardinginfo
    const boardingType = this.getBoardingType(value)
    if (selected === '可寄宿') return boardingType === 'boarding'
    if (selected === '可走读') return boardingType === 'day'
    return true
  },

  getBoardingType(value) {
    if (value === true || value === 1) return 'boarding'
    if (value === false || value === 0) return 'day'

    const text = String(value === undefined || value === null ? '' : value).trim().toLowerCase()
    if (!text || ['暂无', '未知', '待确认', 'null', 'undefined'].includes(text)) return 'unknown'

    // 必须先判断否定表达，避免“没有寄宿”因含“有”或“寄宿”被误判。
    const negativeKeywords = [
      '没有寄宿', '不提供寄宿', '不可寄宿', '不寄宿', '无寄宿',
      '走读', '否', '无', 'false', 'no'
    ]
    const positiveKeywords = [
      '提供寄宿', '支持寄宿', '可寄宿', '寄宿',
      '是', '有', 'true', 'yes'
    ]

    if (negativeKeywords.some(keyword => text.includes(keyword))) return 'day'
    if (positiveKeywords.some(keyword => text.includes(keyword))) return 'boarding'
    return 'unknown'
  },

  collectSchoolFieldText(item, keys) {
    return keys
      .map(key => item[key])
      .filter(value => value !== undefined && value !== null && value !== '')
      .map(value => String(value).toLowerCase())
      .join(' ')
  },

  onReachBottom() {
    if (!this._listLoading && this._serverHasMore) {
      this.setData({ page: this.data.page + 1 })
      this.loadList()
    }
  },

  onSearch(e) {
    clearTimeout(this._timer)
    this._timer = setTimeout(() => {
      this.setData({ searchName: e.detail.value, page: 1, schools: [] })
      this.loadList()
    }, 300)
  },

  onQuickFilterToggle(e) {
    const key = e.currentTarget.dataset.key
    this.setData({
      activeQuickFilter: this.data.activeQuickFilter === key ? '' : key,
      showMoreFilter: false
    })
  },

  onQuickFilterOption(e) {
    const key = e.currentTarget.dataset.key
    const index = Number(e.currentTarget.dataset.index)
    if (key === 'tuition') {
      const option = this.data.tuitionOptions[index]
      if (!option) return
      this.setData({
        tuitionLabel: option.min === '' && option.max === '' ? '' : option.label,
        tuitionMin: option.min,
        tuitionMax: option.max,
        activeQuickFilter: '', page: 1, schools: []
      })
    } else {
      const options = key === 'district' ? this.data.displayDistricts : this.data.gradesList
      const value = options[index]
      const nextData = { [key]: value, activeQuickFilter: '', page: 1, schools: [] }
      if (key === 'grades' && value === '幼儿园') {
        nextData.schoolSystem = 'domestic'
        nextData.schoolNatureOptions = ['公办', '民办']
        nextData.schoolNature = ''
        nextData.draftSchoolNature = ''
      }
      this.setData(nextData)
    }
    this.loadList()
  },

  closeQuickFilter() {
    this.setData({ activeQuickFilter: '' })
  },

  onMoreFilter() {
    if (this.data.showMoreFilter) {
      this.setData({ showMoreFilter: false })
      return
    }
    this.setData({
      showMoreFilter: true,
      activeQuickFilter: '',
      draftSchoolNature: this.data.schoolNature,
      draftBoardingType: this.data.boardingType
    })
  },

  closeMoreFilter() {
    this.setData({ showMoreFilter: false })
  },

  preventTouchMove() {},

  onMoreOption(e) {
    const key = e.currentTarget.dataset.key
    const value = e.currentTarget.dataset.value
    this.setData({ [key]: this.data[key] === value ? '' : value })
  },

  onResetMoreFilter() {
    this.setData({ draftSchoolNature: '', draftBoardingType: '' })
  },

  onConfirmMoreFilter() {
    this.setData({
      schoolNature: this.data.draftSchoolNature,
      boardingType: this.data.draftBoardingType,
      showMoreFilter: false,
      page: 1,
      schools: []
    })
    this.loadList()
  },

  onTapSchool(e) {
    const school = this.data.schools.find(item => item.compareKey === e.currentTarget.dataset.key)
    if (!school) return
    const schoolSource = String(school.schoolSource || '')
    if (school.system === 'domestic') {
      if (school.kind === 'kindergarten') {
        wx.navigateTo({ url: '/pages/score-school-detail/score-school-detail?id=' + school.id + '&schoolSource=kindergarten' })
        return
      }
      if (school.kind === 'primary') {
        wx.navigateTo({ url: '/pages/score-school-detail/score-school-detail?id=' + school.id + '&schoolSource=primary' })
        return
      }
      const level = school.kind === 'junior' ? 'junior' : (school.kind === 'district' ? 'district' : (school.kind === 'general' ? 'general' : ''))
      wx.navigateTo({ url: '/pages/score-school-detail/score-school-detail?id=' + school.id + (level ? '&level=' + level : '') + '&schoolSource=' + encodeURIComponent(schoolSource || 'score') })
      return
    }
    wx.navigateTo({ url: '/pages/school-detail/school-detail?id=' + school.id + '&schoolSource=' + encodeURIComponent(schoolSource || 'international') })
  },

  onAdmissions(e) {
    wx.navigateTo({ url: '/pages/admission-detail/admission-detail?id=' + e.currentTarget.dataset.id })
  },

  onVisit(e) {
    const name = e.currentTarget.dataset.name || ''
    this.setData({
      showLead: true,
      studentName: '',
      phone: '',
      childGrade: '',
      targetSchool: name
    })
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
    const { studentName, phone, childGrade, targetSchool } = this.data
    if (!studentName || !phone || !childGrade) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' })
      return
    }
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '手机号格式错误', icon: 'none' })
      return
    }
    try {
      await request('/api/lead/submit', 'POST', {
        parent_name: studentName,
        child_grade: childGrade,
        target_school: targetSchool,
        parent_phone: phone
      }, true)
      wx.showToast({ title: '预约成功，老师将尽快联系您', icon: 'none', duration: 2000 })
      this.setData({ showLead: false })
    } catch (e) {
      if (e && e.code === 401) {
        wx.showToast({ title: '请先登录后再预约', icon: 'none' })
      }
    }
  },

  resolveImageUrl(url) {
    return require('../../utils/image').resolveImageUrl(url)
  }
})
