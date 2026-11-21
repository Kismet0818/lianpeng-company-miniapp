const { request } = require('../../utils/request')
const app = getApp()

const fallbackChannelsVideos = [
  { feedId: 'export/UzFfBgAAxOagUGJiQG2TjMzT4DCaL2RIjyTkzE-vRHjQhPS1WA', title: '平和系', cover: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/video/ph.png', sort: 0 },
  { feedId: 'export/UzFfBgAAxMagNCBBWXuOjMzT4DCa4nCLgvoqfiDy9ylsPBeHFw', title: '世外系', cover: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/video/swx.png', sort: 1 },
  { feedId: 'export/UzFfBgAAxOqgUFhYDmS-jMzT4DCa9DxsBi_JT5epuGj_O_ehCw', title: '别被焦虑带节奏', cover: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/video/zkfl.png', sort: 2 },
  { feedId: 'export/UzFfBgAAxOegOGI_UxOFjMzT4DCaYm5JTSuA8M3KCnbAMgtWfQ', title: '上海中考自招录取分布', cover: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/video/zzlq.png', sort: 3 },
  { feedId: 'export/UzFfBgAAxPOgIFlMThOyjMzT4DCaqSnMazh9u6wWRa_HZGJueg', title: '上海中考十六区幸福指数', cover: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/video/xfzs.png', sort: 4 },
  { feedId: 'export/UzFfBgAAxPigKD47b2SzjMzT4DCawSJOSzFEUw4mARt1F3128A', title: '上海中考真相', cover: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/video/zsqk.png', sort: 5 },
  { feedId: 'export/UzFfBgAAxPKgIB4VZ3SJjMzT4DCaZFv3fkGdgNaFqhwJ0SxHhQ', title: '上海小孩的童年噩梦是什么', cover: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/video/tnem.png', sort: 6 },
  { feedId: 'export/UzFfBgAAxPageFQ6VjSyjMzT4DCa6QS500iOITaRwMj0FnVxNg', title: '上海中考各分数段升学规划表', cover: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/video/sxgh.png', sort: 7 },
  { feedId: 'export/UzFfBgAAxP6gfFwFVB-TjMzT4DCalnBZC7wawOCG96UoC1UDtA', title: '什么是预制娃', cover: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/video/yzc.png', sort: 8 },
  { feedId: 'export/UzFfBgAAxPSgaDZ5Hga2jMzT4DCaT8OS_Jcc0EvuKE55ZNz_mg', title: '上海转学大坑避雷', cover: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/video/zxdk.png', sort: 9 }
]

const buildChannelCards = videos => videos.map(video => ({
  title: video.title,
  image: video.cover,
  action: 'channelsVideo',
  feedId: video.feedId,
  finderUserName: 'sphkgLpLsVqLxCS'
}))

Page({
  data: {
    heroTop: 76,
    schoolCount: 0,
    notices: [],
    hotSchools: [],
    hotArticles: [],
    shortcuts: [],
    schoolStats: [],
    tabs: ['当地热门学校', '最近浏览', '收藏的学校'],
    activeTab: 0,
    homeTopics: [
      { label: '家长问答', count: '加载中', statKey: 'parentQa', icon: '/assets/icons/zixun.png', action: 'qa' },
      { label: '摇号指南', count: '加载中', statKey: 'lotteryGuide', icon: '/assets/icons/chafengongju/daoqu.svg', action: 'guide' },
      { label: '热门学校', count: '加载中', statKey: 'hotSchools', icon: '/assets/icons/remen.png', action: 'schools' }
    ],
    homeShortcuts: [
      { label: '查对口', icon: '/assets/icons/chafengongju/daoxiao-active.svg', action: 'duikou' },
      { label: '查政策', icon: '/assets/icons/zhengce.png', action: 'policy' },
      { label: '查学校', icon: '/assets/icons/school-ranking.png', action: 'school' },
      { label: '升学规划', icon: '/assets/icons/zexiaoguihua.png', action: 'planning' },
      { label: '小初预约', icon: '/assets/icons/xuexiaoku/yuyue.png', action: 'appointment' }
    ],
    displaySchools: [],
    displayMultipleItems: 4,
    districtStats: [
      { district: '黄浦区', count: 60 },
      { district: '普陀区', count: 71 },
      { district: '杨浦区', count: 80 },
      { district: '嘉定区', count: 68 },
      { district: '浦东新区', count: 25 },
      { district: '长宁区', count: 54 },
      { district: '徐汇区', count: 78 },
      { district: '闵行区', count: 173 }
    ],
    showQr: false,
    qrImageUrl: '',
    showStartupAd: false,
    contentLinks: {},
    startupAdImage: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/lead-materials/lingziliao.png',
    strategyCards: buildChannelCards(fallbackChannelsVideos),
    guideSteps: [
      { stepTitle: '升学规划', subTitle: '明确升学方向', stepIcon: '/assets/icons/zexiaozhinan/shengxueguihua.png', stepIconSize: 46, stepIconX: 0, stepIconY: 0, menus: [
        { label: '升学路径', category: '升学规划', icon: '/assets/icons/zexiaozhinan/1/shengxuelujing.png' },
        { label: '规划须知', category: '升学规划', icon: '/assets/icons/zexiaozhinan/1/guihuaxuzhi.png' },
        { label: '学校介绍', category: '升学规划', icon: '/assets/icons/zexiaozhinan/1/xuexiaojieshao.png', iconClass: 'menu-icon-school' },
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
        { label: '提前学习', category: '录取准备', icon: '/assets/icons/zexiaozhinan/4/tiqianxuesxi.png' }
      ] }
    ],
    expandedStep: 0,
    banners: [
      {
        image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/lead-materials/zexiaoshouce.png',
        sub: '择校手册'
      }
    ]
  },

  onLoad() {
    this.setHeroSafeTop()
    this.loadOverview()
    this.loadChannelsVideos()
    this.loadContentLinks()
  },

  showStartupAdOnce() {
    if (app.globalData.startupAdShown) return
    app.globalData.startupAdShown = true
    this.setData({ showStartupAd: true })
  },

  closeStartupAd() {
    this.setData({ showStartupAd: false })
  },

  preventStartupAdTouchMove() {},

  onShow() {
    wx.setNavigationBarColor({ backgroundColor: '#64a8cd', frontColor: '#ffffff' })
    app.globalData.selectedTab = 0
    const tabBar = this.getTabBar && this.getTabBar()
    if (tabBar) tabBar.setData({ selected: 0 })
    wx.nextTick(() => {
      const currentTabBar = this.getTabBar && this.getTabBar()
      if (currentTabBar) currentTabBar.setData({ selected: 0 })
    })
    if (this.data.activeTab === 1) this.loadRecentSchools()
    if (this.data.activeTab === 2) this.loadFavorites()
    this.loadContentLinks()
  },

  onPullDownRefresh() {
    this.loadContentLinks().finally(() => wx.stopPullDownRefresh())
  },

  async loadContentLinks() {
    try {
      const result = await request('/api/content-links', 'GET', {}, false, true)
      const links = {}
      const list = Array.isArray(result) ? result : []
      list.forEach(item => {
        const type = String(item && (item.contentType || item.content_type) || '').trim().toUpperCase()
        const linkUrl = String(item && (item.linkUrl || item.link_url) || '').trim()
        if (['OFFICIAL_ACCOUNT', 'LIVE'].indexOf(type) < 0 || !/^https:\/\//i.test(linkUrl)) return
        links[type] = { title: String(item.title || '').trim(), linkUrl }
      })
      this.setData({ contentLinks: links })
    } catch (e) {
      // 配置接口失败时不展示过期或写死的外链入口。
      this.setData({ contentLinks: {} })
      console.error('公众号与直播链接加载失败：', e)
    }
  },

  onContentLinkTap(e) {
    const type = String(e.currentTarget.dataset.type || '')
    // 直播场次由 /api/live/list 提供，不能依赖 content-links 中的网页地址。
    if (type === 'LIVE') {
      wx.navigateTo({ url: '/pages/monthly-live/monthly-live' })
      return
    }
    const item = this.data.contentLinks[type]
    if (!item || !item.linkUrl) {
      wx.showToast({ title: '直播信息暂未配置', icon: 'none' })
      return
    }
    wx.setStorageSync('webviewTarget', item.linkUrl)
    wx.navigateTo({ url: '/pages/webview/webview' })
  },

  setHeroSafeTop() {
    try {
      const menu = wx.getMenuButtonBoundingClientRect()
      if (menu && menu.bottom) {
        this.setData({
          heroTop: menu.bottom + 12
        })
      }
    } catch (e) {}
  },

  async loadOverview() {
    try {
      const data = await request('/api/home/overview')
      const hotSchools = (Array.isArray(data.hotSchools) ? data.hotSchools : []).map(item => ({
        ...item,
        id: item.id || item.schoolId,
        schoolSource: item.schoolSource || 'primary',
        uid: (item.schoolSource || 'primary') + ':' + (item.id || item.schoolId),
        logoUrl: this.resolveHomeImageUrl(item.logoUrl || item.backgroundImageUrl || ''),
        address: item.address || ''
      }))
      const rawStats = data.schoolStats
      const statsList = Array.isArray(rawStats)
        ? rawStats
        : (rawStats && typeof rawStats === 'object'
          ? Object.keys(rawStats).map(label => ({ label, count: rawStats[label] }))
          : [])
      const schoolStats = statsList.map(item => {
        const label = item.label || item.grade || item.name || ''
        const isInternational = label.indexOf('国际') === 0
        return Object.assign({}, item, {
          count: item.count !== undefined ? item.count : (item.value || item.schoolCount || 0),
          labelPrefix: isInternational ? '国际' : '',
          labelGrade: isInternational ? label.slice(2) : label
        })
      })
      const topicStats = data.topicStats && typeof data.topicStats === 'object' ? data.topicStats : {}
      const homeTopics = this.data.homeTopics.map(item => {
        const value = Number(topicStats[item.statKey])
        return Object.assign({}, item, { count: Number.isFinite(value) ? value + '条内容' : '0条内容' })
      })
      this.setData({
        schoolCount: data.schoolCount,
        notices: Array.isArray(data.notices) ? data.notices : [],
        hotSchools,
        hotArticles: Array.isArray(data.hotArticles) ? data.hotArticles : [],
        shortcuts: Array.isArray(data.shortcuts) ? data.shortcuts : [],
        schoolStats,
        homeTopics,
        displaySchools: this.data.activeTab === 0 ? hotSchools : this.data.displaySchools,
        displayMultipleItems: 4
      })
      this.data.hotSchools = hotSchools
      if (this.data.activeTab === 1) this.loadRecentSchools()
      if (this.data.activeTab === 2) this.loadFavorites()
    } catch (e) {
      console.error('首页概览加载失败：', e)
    }
  },

  async loadChannelsVideos() {
    try {
      const videos = await request('/api/config/channels-videos', 'GET')
      if (!Array.isArray(videos)) {
        console.error('视频号攻略接口返回格式错误：', videos)
        return
      }
      const activeVideos = videos
        .filter(item => item && Number(item.isActive !== undefined ? item.isActive : item.is_active) === 1)
        .map(item => ({
          id: Number(item.id || 0),
          title: String(item.title || '').trim(),
          cover: String(item.cover || '').trim(),
          feedId: String(item.feedId || item.feed_id || '').trim(),
          sort: Number(item.sort || 0)
        }))
        .filter(item => item.feedId)
        // 数据库 id 越大代表越新，最新上传的视频排在最左侧。
        .sort((a, b) => b.id - a.id || a.sort - b.sort)
      if (!activeVideos.length) return

      const strategyCards = activeVideos.map(video => ({
          title: video.title || '',
          image: this.resolveHomeImageUrl(video.cover || ''),
          action: 'channelsVideo',
          feedId: video.feedId,
          finderUserName: 'sphkgLpLsVqLxCS'
      }))
      this.setData({ strategyCards })
    } catch (e) {
      console.error('视频号攻略加载失败：', e)
    }
  },

  onShortcut(e) {
    const path = e.currentTarget.dataset.path
    if (!path) return
    const normalizedPath = String(path).replace(/^\/+/, '').split('?')[0]
    const tabRoutes = [
      'pages/home/home',
      'pages/school/school',
      'pages/study-abroad/study-abroad',
      'pages/user/user'
    ]
    if (tabRoutes.indexOf(normalizedPath) >= 0) {
      wx.switchTab({ url: '/' + normalizedPath })
    } else {
      wx.navigateTo({ url: path })
    }
  },

  onTapSchool(e) {
    const source = e.currentTarget.dataset.source || 'international'
    const page = source === 'international' ? '/pages/school-detail/school-detail' : '/pages/score-school-detail/score-school-detail'
    wx.navigateTo({ url: page + '?id=' + e.currentTarget.dataset.id + '&schoolSource=' + encodeURIComponent(source) })
  },
  onMoreSchools() {
    wx.navigateTo({ url: '/pages/hot-schools/hot-schools' })
  },

  onNoticeTap(e) {
    const schoolId = e.currentTarget.dataset.id
    if (!schoolId) return
    wx.navigateTo({ url: '/pages/admission-detail/admission-detail?id=' + schoolId })
  },

  onTabChange(e) {
    const activeTab = Number(e.currentTarget.dataset.index)
    if (activeTab === this.data.activeTab) return
    if (activeTab === 1) {
      this.setData({ activeTab, displaySchools: [] }, () => this.loadRecentSchools())
    } else if (activeTab === 2) {
      this.setData({ activeTab, displaySchools: [] }, () => this.loadFavorites())
    } else {
      this.setData({ activeTab, displaySchools: this.data.hotSchools, displayMultipleItems: 4 })
    }
  },

  onHomeShortcut(e) {
    const action = e.currentTarget.dataset.action
    if (action === 'school') {
      wx.switchTab({ url: '/pages/school/school' })
    } else if (action === 'duikou') {
      wx.navigateTo({ url: '/pages/duikou-list/duikou-list' })
    } else if (action === 'policy') {
      wx.navigateTo({ url: '/pages/duikou-list/duikou-list?mode=policy' })
    } else if (action === 'planning') {
      wx.navigateTo({ url: '/pages/lead/lead' })
    } else if (action === 'appointment') {
      wx.navigateTo({ url: '/pages/appointment/appointment' })
    }
  },

  onHomeTopic(e) {
    const action = e.currentTarget.dataset.action
    if (action === 'qa') {
      wx.navigateTo({ url: '/pages/admission-content/admission-content?category=' + encodeURIComponent('家长问答') })
    } else if (action === 'guide') {
      wx.navigateTo({ url: '/pages/lottery-guide/lottery-guide' })
    } else if (action === 'schools') {
      wx.switchTab({ url: '/pages/school/school' })
    }
  },

  async loadRecentSchools() {
    // 旧版本只保存数字 ID，无法区分不同学校来源，升级后不再读取。
    wx.removeStorageSync('recentSchoolIds')
    const normalize = item => {
      const schoolId = Number(item && (item.schoolId ?? item.id))
      const schoolSource = String(item && item.schoolSource || '').trim()
      if (!Number.isInteger(schoolId) || schoolId <= 0 || !schoolSource) return null
      const rawLogo = item.logoUrl || item.logo_url || item.logo || item.schoolLogo || item.school_logo || ''
      const fallbackLogo = schoolSource === 'score' ? 'score-query/school-logo/' + schoolId + '.jpg' : ''
      return {
        ...item,
        id: schoolId,
        schoolId,
        schoolSource,
        uid: schoolSource + ':' + schoolId,
        name: item.name || item.schoolName || '',
        logoUrl: this.resolveHomeImageUrl(rawLogo || fallbackLogo)
      }
    }
    const toArray = value => {
      if (Array.isArray(value)) return value
      if (!value || typeof value !== 'object') return []
      if (Array.isArray(value.records)) return value.records
      if (Array.isArray(value.list)) return value.list
      if (Array.isArray(value.content)) return value.content
      if (Array.isArray(value.data)) return value.data
      return []
    }
    const dedupe = value => {
      const items = toArray(value)
      const seen = new Set()
      return items.map(normalize).filter(item => {
        if (!item) return false
        const key = item.schoolSource + ':' + item.schoolId
        if (seen.has(key)) return false
        seen.add(key)
        return true
      }).slice(0, 20)
    }

    try {
      const token = wx.getStorageSync('token')
      const cached = dedupe(wx.getStorageSync('recentSchoolCache'))
      const list = token
        ? await request('/api/user/activity/recent?limit=20', 'GET', {}, true)
        : cached
      const schools = dedupe(list)
      if (this.data.activeTab !== 1) return
      if (schools.length) wx.setStorageSync('recentSchoolCache', schools)
      this.setData({ displaySchools: schools, displayMultipleItems: 4 })
    } catch (e) {
      console.error('最近浏览加载失败：', e)
      if (this.data.activeTab === 1) this.setData({ displaySchools: [], displayMultipleItems: 4 })
    }
  },

  resolveHomeImageUrl(url) {
    return require('../../utils/image').resolveImageUrl(url)
  },

  onSchoolLogoError(e) {
    const index = Number(e.currentTarget.dataset.index)
    if (!Number.isInteger(index) || index < 0 || index >= this.data.displaySchools.length) return
    this.setData({ ['displaySchools[' + index + '].logoUrl']: '' })
  },

  async loadFavorites() {
    try {
      const list = await request('/api/favorite/list', 'GET', {}, true)
      if (this.data.activeTab !== 2) return
      const schools = (Array.isArray(list) ? list : []).map(item => ({
        id: item.schoolId,
        schoolSource: item.schoolSource || 'international',
        uid: (item.schoolSource || 'international') + ':' + item.schoolId,
        name: item.schoolName,
        logoUrl: this.resolveHomeImageUrl(item.schoolLogo || item.schoolLogoUrl || item.school_logo || item.logoUrl || item.logo_url || item.logo || ''),
        district: item.district,
        curriculum: item.curriculum,
        grades: item.grades
      }))
      this.setData({
        displaySchools: schools,
        displayMultipleItems: 4
      })
    } catch (e) {
      if (this.data.activeTab === 2) {
        this.setData({ displaySchools: [], displayMultipleItems: 4 })
      }
      if (e && e.code === 401) {
        wx.showToast({ title: '登录后可查看收藏学校', icon: 'none' })
      }
    }
  },

  onSearch() {
    wx.switchTab({ url: '/pages/school/school' })
  },

  openAdvisor() { wx.navigateTo({ url: '/pages/advisor/advisor' }) },

  onPlan() {
    wx.navigateTo({ url: '/pages/lead/lead' })
  },

  openWebsite() {
    wx.navigateTo({ url: '/pages/webview/webview' })
  },

  openBeianMps() { wx.setStorageSync('webviewTarget', 'https://beian.mps.gov.cn/#/query/webSearch?code=31010402337112'); wx.navigateTo({ url: '/pages/webview/webview' }) },
  openBeianMiit() { wx.setStorageSync('webviewTarget', 'https://beian.miit.gov.cn/'); wx.navigateTo({ url: '/pages/webview/webview' }) },

  onDistrictTap(e) {
    const district = e.currentTarget.dataset.district
    app.globalData.presetDistrict = district
    wx.switchTab({ url: '/pages/school/school' })
  },

  onStrategyCardTap(e) {
    const card = this.data.strategyCards[Number(e.currentTarget.dataset.index)]
    if (!card) return
    if (card.action === 'channelsVideo') {
      if (!card.feedId) {
        wx.showToast({ title: '视频内容暂未配置', icon: 'none' })
        return
      }
      if (typeof wx.openChannelsActivity !== 'function') {
        wx.showToast({ title: '当前微信版本暂不支持打开视频', icon: 'none' })
        return
      }
      wx.openChannelsActivity({
        finderUserName: card.finderUserName,
        feedId: card.feedId,
        fail(error) {
          console.error('打开视频号视频失败：', error)
          wx.showToast({ title: '暂时无法打开视频', icon: 'none' })
        }
      })
      return
    }
    if (card.action === 'channelsProfile') {
      if (typeof wx.openChannelsUserProfile !== 'function') {
        wx.showToast({ title: '当前微信版本暂不支持打开视频号', icon: 'none' })
        return
      }
      wx.openChannelsUserProfile({
        finderUserName: card.finderUserName,
        fail(error) {
          console.error('打开视频号主页失败：', error)
          wx.showToast({ title: '暂时无法打开视频号', icon: 'none' })
        }
      })
      return
    }
    wx.showToast({ title: '攻略内容即将上线', icon: 'none' })
  },

  onHotArticleTap(e) {
    const item = this.data.hotArticles[Number(e.currentTarget.dataset.index)]
    if (!item || !item.url) return wx.showToast({ title: '文章链接暂未配置', icon: 'none' })
    if (typeof wx.openOfficialAccountArticle !== 'function') {
      wx.showToast({ title: '当前微信版本暂不支持打开公众号文章', icon: 'none' })
      return
    }
    wx.openOfficialAccountArticle({
      url: item.url,
      fail: error => {
        console.error('打开公众号文章失败', error)
        wx.showToast({ title: '暂时无法打开公众号文章', icon: 'none' })
      }
    })
  },

  onParentChat() {
    this.setData({
      showQr: true,
      qrImageUrl: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/lead-materials/lingziliao.png'
    })
  },

  closeQr() {
    this.setData({ showQr: false })
  },

  onGuideTap(e) {
    const index = Number(e.currentTarget.dataset.index)
    this.setData({ expandedStep: this.data.expandedStep === index ? -1 : index })
  },

  onAdmissionArticles() {
    wx.navigateTo({ url: '/pages/admission-content/admission-content' })
  },

  onGuideMenu(e) {
    const category = e.currentTarget.dataset.category
    const menu = e.currentTarget.dataset.menu
    wx.navigateTo({
      url: '/pages/guide-list/guide-list?category=' + encodeURIComponent(category) + '&path=' + encodeURIComponent(menu)
    })
  },

  onBannerTap(e) {
    this.setData({
      showQr: true,
      qrImageUrl: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/lead-materials/lingziliao.png'
    })
  },

  onOpenDay() {
    wx.navigateTo({ url: '/pages/open-day/open-day' })
  },

  onMaterials() {
    wx.navigateTo({ url: '/pages/material-list/material-list' })
  }
})
