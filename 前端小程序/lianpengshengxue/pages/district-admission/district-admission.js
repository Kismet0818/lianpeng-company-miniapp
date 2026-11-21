const { request } = require('../../utils/request')
const { resolveImageUrl } = require('../../utils/image')

const SCORE_SCHOOL_ALIAS_IDS = {
  '交附嘉定':42,'嘉定一中':43,'上师嘉分':44,'上师嘉定':44,'奉贤中学':64,'格致奉贤':65,'华二临港':66,
  '华二宝山':37,'上大附中':38,'上师宝分':39,'吴淞中学':40,'行知中学':41,'崇明中学':67,'上实东滩':68,
  '复附徐汇':6,'市二中学':7,'南洋模范':8,'南洋中学':9,'上海中学':10,'位育中学':11,'晋元高级':21,'华二普陀':22,'宜川中学':23,'曹杨二中':69,
  '控江中学':27,'同济一附':28,'杨浦高级':29,'上外云间':56,'松江一中':57,'松江二中':58,'上师松分':59,'华二松江':60,
  '川沙中学':45,'复附浦东':46,'高桥中学':47,'进才中学':48,'南汇中学':49,'上中东校':50,'上海实验':51,'上师附中':52,'洋泾中学':53,'建平中学':70,
  '复旦复兴':24,'华师一附':25,'上财北郊':26,'华师三附':54,'金山中学':55,'复旦中学':12,'市三女中':13,'延安中学':14,
  '交附闵分':30,'闵行中学':31,'七宝中学':32,'市二梅陇':33,'上师闵分':34,'向明浦江':35,'华二紫竹':36,
  '复附青浦':61,'青浦高级':62,'朱家角':63,'华东模范':15,'回民中学':16,'市北中学':17,'六十中':18,'新中高级':19,'育才中学':20,'市西中学':71,
  '光明中学':1,'敬业中学':2,'大境中学':3,'同济大学科技中学':4,'向明中学':5,'格致中学':72,'大同中学':73,'卢湾高级中学':74
}

const districtPlans = {
  '青浦区': {
    key: { value: '1556', percent: '48%', change: '223', trend: 'up' },
    district: { value: '625', percent: '19%', change: '291', trend: 'up' },
    general: { value: '1085', percent: '33%', change: '7', trend: 'up' },
    total: { value: '3266', change: '521', trend: 'up' }
  },
  '奉贤区': {
    key: { value: '878', percent: '27%', change: '113', trend: 'up' },
    district: { value: '994', percent: '31%', change: '109', trend: 'up' },
    general: { value: '1334', percent: '42%', change: '258', trend: 'up' },
    total: { value: '3206', change: '480', trend: 'up' }
  },
  '崇明区': {
    key: { value: '490', percent: '29%', change: '4', trend: 'down' },
    district: { value: '1038', percent: '61%', change: '1', trend: 'up' },
    general: { value: '170', percent: '10%', change: '29', trend: 'down' },
    total: { value: '1698', change: '32', trend: 'down' }
  },
  '金山区': {
    key: { value: '805', percent: '36%', change: '22', trend: 'down' },
    district: { value: '1282', percent: '58%', change: '100', trend: 'up' },
    general: { value: '134', percent: '6%', change: '43', trend: 'down' },
    total: { value: '2221', change: '35', trend: 'up' }
  },
  '松江区': {
    key: { value: '1764', percent: '28%', change: '164', trend: 'up' },
    district: { value: '2286', percent: '36%', change: '79', trend: 'up' },
    general: { value: '2340', percent: '37%', change: '440', trend: 'up' },
    total: { value: '6390', change: '683', trend: 'up' }
  },
  '浦东新区': {
    key: { value: '5234', percent: '25%', change: '178', trend: 'up' },
    district: { value: '9282', percent: '44%', change: '197', trend: 'up' },
    general: { value: '6553', percent: '31%', change: '1205', trend: 'up' },
    total: { value: '21069', change: '1580', trend: 'up' }
  },
  '嘉定区': {
    key: { value: '1454', percent: '28%', change: '146', trend: 'up' },
    district: { value: '2329', percent: '44%', change: '246', trend: 'up' },
    general: { value: '1501', percent: '28%', change: '265', trend: 'up' },
    total: { value: '5284', change: '657', trend: 'up' }
  },
  '宝山区': {
    key: { value: '2390', percent: '33%', change: '326', trend: 'up' },
    district: { value: '2608', percent: '36%', change: '235', trend: 'up' },
    general: { value: '2241', percent: '31%', change: '140', trend: 'up' },
    total: { value: '7239', change: '701', trend: 'up' }
  },
  '杨浦区': {
    key: { value: '1708', percent: '37%', change: '65', trend: 'up' },
    district: { value: '2676', percent: '57%', change: '321', trend: 'up' },
    general: { value: '294', percent: '6%', change: '63', trend: 'up' },
    total: { value: '4678', change: '449', trend: 'up' }
  },
  '虹口区': {
    key: { value: '1166', percent: '41%', change: '18', trend: 'up' },
    district: { value: '1363', percent: '47%', change: '16', trend: 'up' },
    general: { value: '343', percent: '12%', change: '24', trend: 'up' },
    total: { value: '2872', change: '58', trend: 'up' }
  },
  '普陀区': {
    key: { value: '1692', percent: '37%', change: '147', trend: 'up' },
    district: { value: '2011', percent: '44%', change: '165', trend: 'up' },
    general: { value: '871', percent: '19%', change: '75', trend: 'up' },
    total: { value: '4574', change: '387', trend: 'up' }
  },
  '静安区': {
    key: { value: '2406', percent: '47%', change: '186', trend: 'up' },
    district: { value: '1850', percent: '36%', change: '114', trend: 'up' },
    general: { value: '877', percent: '17%', change: '67', trend: 'up' },
    total: { value: '5133', change: '367', trend: 'up' }
  },
  '长宁区': {
    key: { value: '967', percent: '39%', change: '16', trend: 'down' },
    district: { value: '840', percent: '34%', change: '24', trend: 'up' },
    general: { value: '656', percent: '27%', change: '90', trend: 'up' },
    total: { value: '2463', change: '98', trend: 'up' }
  },
  '徐汇区': {
    key: { value: '2137', percent: '38%', change: '27', trend: 'up' },
    district: { value: '2169', percent: '39%', change: '184', trend: 'up' },
    general: { value: '1325', percent: '24%', change: '172', trend: 'up' },
    total: { value: '5631', change: '383', trend: 'up' }
  },
  '黄浦区': {
    key: { value: '2007', percent: '65%', change: '3', trend: 'down' },
    district: { value: '465', percent: '15%', change: '10', trend: 'up' },
    general: { value: '595', percent: '19%', change: '25', trend: 'up' },
    total: { value: '3067', change: '32', trend: 'up' }
  },
  '闵行区': {
    key: { value: '3155', percent: '31%', change: '196', trend: 'up' },
    district: { value: '2401', percent: '24%', change: '97', trend: 'up' },
    general: { value: '4542', percent: '45%', change: '714', trend: 'up' },
    total: { value: '10098', change: '1007', trend: 'up' }
  }
}

const emptyPlan = {
  key: { value: '--', percent: '--', change: '--' },
  district: { value: '--', percent: '--', change: '--' },
  general: { value: '--', percent: '--', change: '--' },
  total: { value: '--', change: '--' }
}

Page({
  data: {
    district: '',
    year: 2026,
    hasPlan: false,
    plan: emptyPlan,
    activeCatalogTab: 'municipal',
    municipalTabClass: 'active',
    districtTabClass: '',
    generalTabClass: '',
    juniorTabClass: '',
    schoolCatalog: [],
    catalogGroups: [],
    catalogLetters: [],
    catalogLoading: false
  },

  onLoad(options) {
    let district = '区县'
    try {
      district = decodeURIComponent(options.district || '区县')
    } catch (e) {}
    const plan = districtPlans[district] || emptyPlan
    this.setData({
      district,
      plan,
      hasPlan: !!districtPlans[district],
      activeCatalogTab: 'municipal',
      municipalTabClass: 'active',
      districtTabClass: '',
      generalTabClass: '',
      juniorTabClass: ''
    })
    wx.setNavigationBarTitle({ title: '招生计划·' + district })
    this.loadHighSchoolCatalog()
  },

  onQueryTap(e) {
    const mode = e.currentTarget.dataset.mode
    wx.navigateTo({
      url: '/pages/quota-query/quota-query?mode=' + mode + '&district=' + encodeURIComponent(this.data.district)
    })
  },

  onIndependentTap(e) {
    const index = Number(e.currentTarget.dataset.index)
    if (index === 0) {
      wx.navigateTo({ url: '/pages/autonomous-admission/autonomous-admission?district=' + encodeURIComponent(this.data.district) })
    }
  },

  openInternationalAdmission() {
    wx.navigateTo({
      url: '/pages/international-admission/international-admission',
      fail(error) {
        console.error('国际课程招生页面跳转失败：', error)
        wx.showModal({
          title: '页面打开失败',
          content: error && error.errMsg ? error.errMsg : '未知跳转错误',
          showCancel: false
        })
      }
    })
  },

  onCatalogTabTap(e) {
    const tab = e.currentTarget.dataset.tab
    if (!tab || tab === this.data.activeCatalogTab) return
    const groups = this.groupCatalog(this.data.schoolCatalog, tab)
    this.setData({
      activeCatalogTab: tab,
      municipalTabClass: tab === 'municipal' ? 'active' : '',
      districtTabClass: tab === 'district' ? 'active' : '',
      generalTabClass: tab === 'general' ? 'active' : '',
      juniorTabClass: tab === 'junior' ? 'active' : '',
      catalogGroups: groups,
      catalogLetters: groups.map(group => group.initial)
    })
  },

  async loadHighSchoolCatalog() {
    this.setData({ catalogLoading: true })
    try {
      const [data, regionData, districtKeyData, generalData, juniorData] = await Promise.all([
        request('/api/score-query/volunteer?year=2025-2026'),
        request('/api/score-query/region?year=2025-2026'),
        request('/api/score-query/schools/district-key', 'GET', { district: this.data.district }, false),
        this.loadGeneralHighSchools().catch(error => {
          console.error('一般高中学校名录加载失败：', error)
          return []
        }),
        request('/api/junior-schools', 'GET', { district: this.data.district }, false).catch(error => {
          console.error('初中学校名录加载失败：', error)
          return []
        })
      ])
      const source = Array.isArray(data) ? data : []
      const schoolIdMap = {}
      ;(Array.isArray(regionData) ? regionData : []).forEach(item => {
        const key = this.getCatalogSchoolKey(item.highSchoolName)
        const id = Number(item.highSchoolId)
        if (key && id && !schoolIdMap[key]) schoolIdMap[key] = id
      })
      const categoryMap = {
        '市实验性示范性高中': { category: 'municipal', level: '市重点' },
        '区实验性示范性高中': { category: 'district', level: '区重点' },
        '一般高中': { category: 'general', level: '一般高中' }
      }
      const deduped = {}
      source.forEach(item => {
        const categoryInfo = categoryMap[String(item.schoolType || '').trim()]
        if (!categoryInfo || categoryInfo.category === 'district' || categoryInfo.category === 'general') return
        const normalizedName = this.normalizeCatalogSchoolName(item.schoolName)
        if (!normalizedName) return
        const shortName = normalizedName.replace(/^上海市/, '')
        const highSchoolId = Number(item.highSchoolId) || SCORE_SCHOOL_ALIAS_IDS[shortName] || schoolIdMap[this.getCatalogSchoolKey(normalizedName)] || 0
        const key = categoryInfo.category + '-' + (highSchoolId || shortName)
        if (!deduped[key] || normalizedName.length > deduped[key].name.length) {
          deduped[key] = {
            id: key, highSchoolId, name: normalizedName,
            initial: this.getPinyinInitial(shortName), category: categoryInfo.category, level: categoryInfo.level,
            nature: '', boarding: '', code: '',
            logo: highSchoolId ? resolveImageUrl('score-query/school-logo/' + highSchoolId + '.jpg') : ''
          }
        }
      })
      const candidates = Object.keys(deduped).map(key => deduped[key])
      const schoolDetails = await this.loadCatalogSchoolDetails(candidates)
      const legacyCatalog = candidates
        .filter(item => {
          const detail = schoolDetails[item.highSchoolId]
          return detail && String(detail.district || '').trim() === this.data.district
        })
        .map(item => {
          const detail = schoolDetails[item.highSchoolId] || {}
          return Object.assign({}, item, {
            name: this.normalizeCatalogSchoolName(detail.name) || item.name,
            nature: detail.nature || detail.schoolNature || detail.school_type || item.nature,
            boarding: detail.boardingInfo || detail.boarding_info || item.boarding,
            logo: resolveImageUrl(detail.logo || detail.logoUrl || detail.logo_url) || item.logo
          })
        })
      const districtCatalog = (Array.isArray(districtKeyData) ? districtKeyData : []).map(item => {
        const highSchoolId = Number(item.id) || 0
        const fullName = this.normalizeCatalogSchoolName(item.name)
        const shortName = this.normalizeCatalogSchoolName(item.shortName || item.short_name) || fullName
        return {
          id: 'district-' + (highSchoolId || fullName),
          highSchoolId,
          name: fullName,
          fullName,
          shortName,
          initial: this.getPinyinInitial(shortName),
          category: 'district',
          level: '区重点',
          nature: '',
          boarding: '',
          code: '',
          logo: resolveImageUrl(item.logo)
        }
      }).filter(item => item.highSchoolId && item.name)
      const generalRecords = Array.isArray(generalData)
        ? generalData
        : (generalData && (generalData.records || generalData.list || generalData.content)) || []
      const generalCatalog = generalRecords
        .filter(item => String(item.district || '').trim() === this.data.district)
        .map(item => {
          const schoolId = Number(item.id) || 0
          const fullName = this.normalizeCatalogSchoolName(item.name)
          const shortName = this.normalizeCatalogSchoolName(item.shortName || item.short_name) || fullName
          return {
            id: 'general-' + (schoolId || fullName),
            highSchoolId: schoolId,
            name: fullName,
            fullName,
            shortName,
            initial: this.getPinyinInitial(shortName),
            category: 'general',
            level: '一般高中',
            nature: item.schoolType || item.school_type || '',
            boarding: item.boardingInfo || item.boarding_info || '',
            code: '',
            logo: ''
          }
        })
        .filter(item => item.highSchoolId && item.name)
      const juniorRecords = Array.isArray(juniorData)
        ? juniorData
        : (juniorData && (juniorData.records || juniorData.list || juniorData.content)) || []
      const juniorCatalog = juniorRecords
        .filter(item => String(item.district || '').trim() === this.data.district)
        .map(item => {
          const schoolId = Number(item.id) || 0
          const fullName = this.normalizeCatalogSchoolName(item.name)
          const shortName = this.normalizeCatalogSchoolName(item.shortName || item.short_name) || fullName
          return {
            id: 'junior-' + (schoolId || fullName),
            schoolId,
            schoolKind: 'junior',
            name: fullName,
            fullName,
            shortName,
            initial: this.getPinyinInitial(shortName),
            category: 'junior',
            level: '初中',
            nature: item.schoolType || item.school_type || '',
            boarding: item.boardingInfo || item.boarding_info || '',
            code: '',
            logo: ''
          }
        })
        .filter(item => item.schoolId && item.name)
      const schoolCatalog = legacyCatalog.concat(districtCatalog, generalCatalog, juniorCatalog)
      const catalogGroups = this.groupCatalog(schoolCatalog, this.data.activeCatalogTab)
      this.setData({ schoolCatalog, catalogGroups, catalogLetters: catalogGroups.map(group => group.initial) })
    } catch (e) {
      console.error('高中学校名录加载失败：', e)
      wx.showToast({ title: '学校名录加载失败', icon: 'none' })
    } finally {
      this.setData({ catalogLoading: false })
    }
  },

  async loadCatalogSchoolDetails(schools) {
    const ids = Array.from(new Set((Array.isArray(schools) ? schools : [])
      .map(item => Number(item.highSchoolId))
      .filter(Boolean)))
    const details = {}
    let cursor = 0
    const worker = async () => {
      while (cursor < ids.length) {
        const id = ids[cursor++]
        try {
          details[id] = await request('/api/score-query/schools/' + id)
        } catch (e) {
          console.warn('学校详情加载失败：', id, e)
        }
      }
    }
    await Promise.all(Array.from({ length: Math.min(8, ids.length) }, worker))
    return details
  },

  async loadGeneralHighSchools() {
    const params = { page: 1, size: 500, district: this.data.district }
    try {
      return await request('/api/general-high-schools', 'GET', params, true)
    } catch (error) {
      if (!(error && error.code === 401)) throw error
      await getApp().login()
      return request('/api/general-high-schools', 'GET', params, true)
    }
  },

  normalizeCatalogSchoolName(value) { return String(value || '').replace(/\s+/g, '').trim() },
  getCatalogSchoolKey(value) { return this.normalizeCatalogSchoolName(value).replace(/^上海市/, '') },

  getPinyinInitial(name) {
    const originalName = String(name || '')
    if (originalName.indexOf('大境中学') >= 0) return 'D'
    if (originalName.indexOf('七宝中学') >= 0) return 'Q'
    const char = originalName.replace(/^上海市?/, '').charAt(0)
    if (!char) return '#'
    if (/^[A-Za-z]$/.test(char)) return char.toUpperCase()
    const initials = 'ABCDEFGHJKLMNOPQRSTWXYZ'
    const boundaries = '阿芭擦搭蛾发噶哈击喀垃妈拿哦啪期然撒塌挖昔压匝'
    let result = '#'
    for (let i = 0; i < boundaries.length; i += 1) {
      if (char.localeCompare(boundaries.charAt(i), 'zh-CN') >= 0) result = initials.charAt(i)
      else break
    }
    return result || '#'
  },

  onCatalogLetterTap(e) {
    const letter = e.currentTarget.dataset.letter
    if (!letter) return
    const query = wx.createSelectorQuery()
    query.select('#catalog-letter-' + letter).boundingClientRect()
    query.selectViewport().scrollOffset()
    query.exec(result => {
      const target = result && result[0]
      const viewport = result && result[1]
      if (target && viewport) wx.pageScrollTo({ scrollTop: viewport.scrollTop + target.top - 20, duration: 260 })
    })
  },

  onCatalogSchoolTap(e) {
    const id = Number(e.currentTarget.dataset.id)
    if (!id) {
      wx.showToast({ title: '该校详情数据暂未接入', icon: 'none' })
      return
    }
    const level = e.currentTarget.dataset.level
    const schoolKind = e.currentTarget.dataset.kind
    let schoolSource = 'score'
    if (schoolKind === 'junior') schoolSource = 'junior'
    else if (level === '一般高中') schoolSource = 'general'
    const levelParam = schoolKind === 'junior' ? 'junior' : (level === '区重点' ? 'district' : (level === '一般高中' ? 'general' : ''))
    wx.navigateTo({ url: '/pages/score-school-detail/score-school-detail?id=' + id + (levelParam ? '&level=' + levelParam : '') + '&schoolSource=' + schoolSource })
  },

  groupCatalog(list, tab) {
    const groupMap = {}
    ;(Array.isArray(list) ? list : []).filter(item => item.category === tab).forEach(item => {
      const initial = String(item.initial || '#').trim().charAt(0).toUpperCase() || '#'
      if (!groupMap[initial]) groupMap[initial] = { initial, schools: [] }
      groupMap[initial].schools.push(item)
    })
    return Object.keys(groupMap).sort().map(key => groupMap[key])
  }
})
