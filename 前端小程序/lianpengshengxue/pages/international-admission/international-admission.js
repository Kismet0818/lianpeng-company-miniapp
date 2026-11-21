const { request } = require('../../utils/request')

Page({
  data: {
    schools: [],
    displaySchools: [],
    loading: false,
    error: '',
    publicFilter: true,
    privateFilter: true,
    cooperationFilter: true,
    publicFilterClass: 'active',
    privateFilterClass: 'active',
    cooperationFilterClass: 'active'
  },

  onLoad() { this.loadSchools() },

  async loadSchools() {
    this.setData({ loading: true, error: '' })
    try {
      const data = await request('/api/schools/list', 'GET', { page: 1, size: 200 })
      const records = Array.isArray(data && data.records) ? data.records : []
      const schools = records.filter(item => /公办国际高中|民办国际高中/.test(String(item.schoolType || ''))).map(item => {
        const totalRecruit = this.formatNumber(item.totalRecruit !== undefined ? item.totalRecruit : item.total_recruit)
        const localStudents = this.formatNumber(item.localStudents !== undefined ? item.localStudents : item.local_students)
        const nonLocalStudents = this.formatNumber(item.nonLocalStudents !== undefined ? item.nonLocalStudents : item.non_local_students)
        return Object.assign({}, item, {
          levelLabel: '市重点',
          natureLabel: String(item.schoolType || '').indexOf('民办') >= 0 ? '民办' : '公办',
          boardingLabel: this.formatBoarding(item.boardingInfo),
          isCooperation: /中外合作|合作办学|中外教师|中加|中美|中德/.test([item.curriculum, item.description, item.teachingMode].filter(Boolean).join(' ')),
          totalRecruit,
          localStudents,
          nonLocalStudents,
          totalRecruitClass: totalRecruit === '—' ? 'unknown' : '',
          localStudentsClass: localStudents === '—' ? 'unknown' : '',
          nonLocalStudentsClass: nonLocalStudents === '—' ? 'unknown' : ''
        })
      })
      this.setData({ schools })
      this.applyFilters()
    } catch (e) {
      console.error('国际课程招生信息加载失败：', e)
      this.setData({ error: '招生信息加载失败', displaySchools: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  onFilterTap(e) {
    const key = e.currentTarget.dataset.filter
    if (!key) return
    const dataKey = key + 'Filter'
    const classKey = key + 'FilterClass'
    const nextValue = !this.data[dataKey]
    const update = {}
    update[dataKey] = nextValue
    update[classKey] = nextValue ? 'active' : ''
    this.setData(update)
    this.applyFilters()
  },

applyFilters() {
  const publicFilter = this.data.publicFilter
  const privateFilter = this.data.privateFilter
  const cooperationFilter = this.data.cooperationFilter

  const displaySchools = this.data.schools.filter(item => {
    // 场景：公办、民办两个大类全部不勾选 → 全部展示
    if (!publicFilter && !privateFilter) {
      return true
    }

    // 处理公办
    if (item.natureLabel === '公办') {
      // 大类公办开关打开才允许展示；公办不受中外合作子筛控制
      return publicFilter
    }

    // 处理民办
    if (item.natureLabel === '民办') {
      if (!privateFilter) return false
      // 民办大类开启；如果是中外合作学校，需要cooperationFilter为true才放行；普通民办直接放行
      if (item.isCooperation) {
        return cooperationFilter
      } else {
        return true
      }
    }

    // 既不是公办也不是民办的未知类型，直接过滤
    return false
  })

  this.setData({ displaySchools })
},

  formatNumber(value) {
    if (value === null || value === undefined || value === '') return '—'
    const number = Number(value)
    return Number.isFinite(number) ? String(number) : '—'
  },

  formatBoarding(value) {
    const text = String(value || '')
    if (!text) return ''
    if (/无|否|不提供|走读/.test(text)) return '无寄宿'
    if (/全部寄宿|全寄宿/.test(text)) return '全部寄宿'
    if (/有|寄宿/.test(text)) return '部分寄宿'
    return ''
  }
})
