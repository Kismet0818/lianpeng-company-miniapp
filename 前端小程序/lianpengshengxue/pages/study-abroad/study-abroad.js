const app = getApp()

Page({
  data: {
    categories: ['中高考工具', '语言', '竞赛', '留学', '课程'],
    activeTab: 0,
    statusBarHeight: 20,
    competitions: [
      { id: 'toefl', age: 'G4-G9', en: 'TOEFL', title: '托福能力挑战', sub: '以阶段测评与任务挑战提升学术英语综合能力，为后续标化考试打好基础', tags: ['听说读写', '阶段测评', '国际表达'], action: '咨询托福规划', image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/tuofu.png' },
      { id: 'mun', age: 'G6-G10', en: 'MODEL UN', title: '模拟联合国项目', sub: '在议题研究、立场文件和会议协商中，训练表达、思辨与全球视野', tags: ['公共演讲', '议题研究', '团队协作'], action: '了解参会准备', image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/monilianheguo.png' },
      { id: 'robotics', age: 'G4-G8', en: 'ROBOTICS', title: '国际机器人挑战', sub: '围绕真实任务完成设计、搭建与展示，突出动手能力和团队配合', tags: ['创意搭建', '项目协作', '成果展示'], action: '匹配适合赛事', image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/jiqiren.png' },
      { id: 'innovation', age: 'G5-G9', en: 'INNOVATION', title: '国际科创项目', sub: '从生活问题出发完成调研、方案设计与作品表达，培养创新素养', tags: ['问题探索', '创意实践', '项目表达'], action: '咨询项目方向', image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/kechuang.png' },
      { id: 'service', age: 'G6-G10', en: 'SOCIAL IMPACT', title: '社会实践与公益挑战', sub: '通过公益议题、社区行动和成果复盘，形成有持续性的个人成长经历', tags: ['公益行动', '社会观察', '领导力'], action: '定制实践方案', image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/shehuishijian.png' }
    ],
    studyPlans: [
      { id: 'us-undergrad', region: '美国', en: 'U.S. PATH', coverTitle: '美本名校', coverSub: '升学计划', title: '美本名校升学规划', sub: '适用于目标冲刺 QS 前20或 QS 前50院校的国际体系学生', tags: ['个性定位', '背景提升', '申请规划'], theme: 'us', image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/meiben.png' },
      { id: 'diverse-path', region: '多元', en: 'GLOBAL PATH', coverTitle: '多元升学', coverSub: '规划计划', title: '多元升学规划', sub: '适用于目标冲刺 QS 前100院校，希望进行多国家与多路径选择的学生', tags: ['多国联申', '路径匹配', '稳妥进阶'], theme: 'global', image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/duoyuan.png' },
      { id: 'commonwealth', region: '英联邦', en: 'UK · HK · SG', coverTitle: '英联邦', coverSub: '本科规划', title: '英联邦本科升学规划', sub: '适用于目标冲刺英国 G5、香港前三、新加坡前二院校的国际体系学生', tags: ['英国G5', '香港前三', '新加坡前二'], theme: 'uk', image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/yinglianbang.png' }
    ],
    planningCourses: [
      {
        id: 'international-school-planning',
        badge: '择校',
        eyebrow: 'INTERNATIONAL SCHOOL',
        coverTitle: '国际择校',
        coverSub: '规划课程',
        title: '国际择校规划课',
        sub: '从孩子基础、升学目标与家庭需求出发，匹配适合的国际课程和学校路径',
        tags: ['课程体系', '学校匹配', '升学出口'],
        action: '咨询国际择校规划',
        image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/guoji.png'
      },
      {
        id: 'grade-ten-planning',
        badge: '高一',
        eyebrow: 'NEW SENIOR ONE',
        coverTitle: '新高一',
        coverSub: '课程规划',
        title: '新高一课程规划',
        sub: '梳理高中语数英考点、常见高考分值结构与三年学习节奏',
        tags: ['语数英考点', '题型占比', '三年规划'],
        action: '咨询新高一课程规划',
        image: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/gaoyi.png',
        detailUrl: '/pages/grade-ten-planning/grade-ten-planning'
      }
    ],
    courses: [
      { id: 1, category: '课程', title: '雅思7分冲刺班', sub: '适合高一至高三学生，名师授课', image: '' },
      { id: 2, category: '课程', title: 'AP微积分特训营', sub: '全面覆盖考点，冲刺5分必备', image: '' },
      { id: 3, category: '课程', title: 'A-Level经济精讲班', sub: '同步辅导，攻克经济学科难点', image: '' },
      { id: 4, category: '语言', title: 'PTE学术英语突破', sub: '听说读写全面提升，快速出分', image: '' },
      { id: 5, category: '语言', title: '托福口语专项训练', sub: '外教1对1模拟，告别哑巴英语', image: '' },
      { id: 'score-tool', category: '中高考工具', title: '查分工具', image: '' }
    ],
    filteredCourses: []
  },

  onLoad() {
    try {
      const windowInfo = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
      this.setData({ statusBarHeight: windowInfo.statusBarHeight || 20 })
    } catch (e) {}
    this.filterCourses(0)
  },

  onShow() {
    wx.setNavigationBarColor({ backgroundColor: '#64a8cd', frontColor: '#ffffff' })
    app.globalData.selectedTab = 2
    const tabBar = this.getTabBar && this.getTabBar()
    if (tabBar) tabBar.setData({ selected: 2 })
    wx.nextTick(() => {
      const currentTabBar = this.getTabBar && this.getTabBar()
      if (currentTabBar) currentTabBar.setData({ selected: 2 })
    })
  },

  onTabTap(e) {
    const index = Number(e.currentTarget.dataset.index)
    this.setData({ activeTab: index })
    this.filterCourses(index)
  },

  filterCourses(index) {
    const cat = this.data.categories[index]
    this.setData({
      filteredCourses: this.data.courses.filter(c => c.category === cat)
    })
  },

  onCardTap(e) {
    const id = e.currentTarget.dataset.id
    if (id === 'preschool-language') {
      wx.navigateTo({ url: '/pages/preschool-language/preschool-language' })
      return
    }
  },

  onToolTap() {
    wx.navigateTo({
      url: '/pages/score-tool/score-tool',
      fail(error) {
        console.error('打开查分工具失败', error)
        wx.redirectTo({
          url: '/pages/score-tool/score-tool',
          fail(redirectError) {
            console.error('重定向查分工具失败', redirectError)
            wx.showToast({ title: '页面加载失败，请重新编译', icon: 'none' })
          }
        })
      }
    })
  },

  onCourseCardTap(e) {
    const url = e.currentTarget.dataset.url
    if (url) wx.navigateTo({ url })
  },

  onConsult(e) {
    const target = encodeURIComponent(e.currentTarget.dataset.name || '留学辅导规划')
    wx.navigateTo({ url: '/pages/lead/lead?target=' + target + '&source=study-abroad' })
  }
})
