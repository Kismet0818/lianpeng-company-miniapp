const rankings = {
  hurun: [
    { rank: 1, name: '上海民办包玉刚实验学校', city: '上海市' },
    { rank: 2, name: '深圳国际交流书院', city: '深圳市' },
    { rank: 3, name: '上海市民办平和学校', city: '上海市' },
    { rank: 4, name: '北京市鼎石国际学校', city: '北京市' },
    { rank: 5, name: '上海市世界外国语中学', city: '上海市' },
    { rank: 6, name: '北京师范大学附属实验中学国际部', city: '北京市' },
    { rank: 7, name: '领科教育上海校区', city: '上海市' },
    { rank: 8, name: 'UWC常熟世界联合学院', city: '苏州市' },
    { rank: 9, name: '北京市十一学校国际部', city: '北京市' },
    { rank: 10, name: '广东碧桂园学校', city: '佛山市' },
    { rank: 11, name: '中国人民大学附属中学国际课程中心', city: '北京市' },
    { rank: 12, name: '华南师范大学附属中学国际部', city: '广州市' },
    { rank: 13, name: 'WLSA上海学校', city: '上海市' },
    { rank: 14, name: '北京市第四中学国际部', city: '北京市' },
    { rank: 15, name: '深圳中学国际部', city: '深圳市' }
  ],
  forbes: [
    { rank: 1, name: '上海惠灵顿外籍人员子女学校', city: '上海市' },
    { rank: 2, name: '深圳国际交流书院', city: '深圳市' },
    { rank: 3, name: '上海民办包玉刚实验学校', city: '上海市' },
    { rank: 4, name: '北京市鼎石国际学校', city: '北京市' },
    { rank: 5, name: '广州美国人国际学校', city: '广州市' },
    { rank: 6, name: '天津惠灵顿学校', city: '天津市' },
    { rank: 7, name: '重庆巴蜀常春藤学校', city: '重庆市' }
  ]
}

Page({
  data: {
    activeRanking: 'hurun',
    bannerTitle: '2025胡润百学·中国国际化学校排行榜',
    bannerSubtitle: 'Hurun Education Top International Schools in China 2025',
    selectedCity: '全部',
    cities: ['全部', '北京市', '上海市', '天津市', '重庆市', '广州市'],
    filteredRanking: [],
    showCityFilter: false
  },

  onLoad() { this.updateRanking() },

  changeRanking(event) {
    const activeRanking = event.currentTarget.dataset.ranking
    if (activeRanking === this.data.activeRanking) return
    const isHurun = activeRanking === 'hurun'
    this.setData({
      activeRanking,
      selectedCity: '全部',
      bannerTitle: isHurun ? '2025胡润百学·中国国际化学校排行榜' : '2025福布斯·中国国际化学校排行榜',
      bannerSubtitle: isHurun ? 'Hurun Education Top International Schools in China 2025' : 'Forbes China International Schools Ranking 2025'
    }, () => this.updateRanking())
  },

  openCityFilter() { this.setData({ showCityFilter: true }) },
  closeCityFilter() { this.setData({ showCityFilter: false }) },

  selectCity(event) {
    this.setData({ selectedCity: event.currentTarget.dataset.city, showCityFilter: false }, () => this.updateRanking())
  },

  updateRanking() {
    const { activeRanking, selectedCity } = this.data
    const source = rankings[activeRanking] || []
    const filteredRanking = source
      .filter(item => selectedCity === '全部' || item.city === selectedCity)
      .map((item, index) => ({ ...item, key: `${item.rank}-${index}-${item.name}` }))
    this.setData({ filteredRanking })
  }
})
