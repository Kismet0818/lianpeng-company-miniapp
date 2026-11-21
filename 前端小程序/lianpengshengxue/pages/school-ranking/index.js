const { request } = require('../../utils/request')

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
    { rank: 1, name: '上海民办包玉刚实验学校', city: '上海市' },
    { rank: 2, name: '北京市鼎石国际学校', city: '北京市' },
    { rank: 3, name: '上海市民办平和学校', city: '上海市' },
    { rank: 4, name: '广东碧桂园学校', city: '佛山市' },
    { rank: 5, name: '深圳国际交流书院', city: '深圳市' },
    { rank: 6, name: 'WLSA上海学校', city: '上海市' },
    { rank: 7, name: '上海光华学院剑桥国际中心', city: '上海市' },
    { rank: 8, name: '上海市世界外国语中学', city: '上海市' },
    { rank: 9, name: '上海浦东新区民办惠立学校', city: '上海市' },
    { rank: 10, name: '上海星河湾双语学校', city: '上海市' },
    { rank: 10, name: 'UWC常熟世界联合学院', city: '苏州市' },
    { rank: 10, name: '北京乐成国际学校', city: '北京市' },
    { rank: 13, name: '领科教育上海校区', city: '上海市' },
    { rank: 14, name: '哈罗北京', city: '北京市' },
    { rank: 15, name: '广州市斐特思学校', city: '广州市' },
    { rank: 16, name: '北京二十一世纪国际学校', city: '北京市' },
    { rank: 16, name: '长沙康礼克雷格学校', city: '长沙市' },
    { rank: 18, name: '上海新纪元双语学校', city: '上海市' },
    { rank: 19, name: '北京朝阳区世青国际学校', city: '北京市' },
    { rank: 19, name: '武汉英中高级中学', city: '武汉市' },
    { rank: 21, name: '深圳市万科梅沙书院', city: '深圳市' },
    { rank: 22, name: '北京市海嘉国际双语学校', city: '北京市' },
    { rank: 23, name: '青苗国际双语学校（顺义天竺校区）', city: '北京市' },
    { rank: 24, name: '天津英华实验学校', city: '天津市' },
    { rank: 25, name: '北京稻香湖学校', city: '北京市' },
    { rank: 25, name: '东莞市松山湖清澜山学校', city: '东莞市' },
    { rank: 27, name: '常州威雅学校', city: '常州市' },
    { rank: 28, name: '北京市私立汇佳学校', city: '北京市' },
    { rank: 29, name: '青岛墨尔文中学', city: '青岛市' },
    { rank: 30, name: '青苗国际双语学校（海淀校区）', city: '北京市' },
    { rank: 31, name: '上海七宝德怀特高级中学', city: '上海市' },
    { rank: 31, name: '广州荔湾爱莎文华学校', city: '广州市' },
    { rank: 33, name: '启明星双语学校', city: '北京市' },
    { rank: 34, name: '北京海淀凯文学校', city: '北京市' },
    { rank: 35, name: '上海市实验学校剑桥教育中心', city: '上海市' },
    { rank: 35, name: '上海康德双语实验学校', city: '上海市' },
    { rank: 35, name: '北京市新英才学校', city: '北京市' },
    { rank: 38, name: '北外附属杭州橄榄树学校', city: '杭州市' },
    { rank: 38, name: '深圳市南山区爱文学校', city: '深圳市' },
    { rank: 40, name: '武汉爱莎国际学校', city: '武汉市' },
    { rank: 41, name: '北京德闳学校', city: '北京市' },
    { rank: 42, name: '北京市朝阳区人朝分实验学校', city: '北京市' },
    { rank: 42, name: '深圳新哲书院', city: '深圳市' },
    { rank: 44, name: '北京市朝阳区凯文学校', city: '北京市' },
    { rank: 45, name: '上海诺美学校', city: '上海市' },
    { rank: 45, name: '杭州娃哈哈双语学校', city: '杭州市' },
    { rank: 47, name: '西安高新一中国际班', city: '西安市' },
    { rank: 48, name: '万科双语高中（前上海德英乐学院）', city: '上海市' },
    { rank: 49, name: '苏州工业园区海归人才子女学校', city: '苏州市' },
    { rank: 50, name: '合肥安生学校', city: '合肥市' },
    { rank: 51, name: '苏州科技城外国语学校', city: '苏州市' },
    { rank: 51, name: '上海浦东新区民办协和双语学校', city: '上海市' },
    { rank: 53, name: '上海闵行区诺德安达双语学校', city: '上海市' },
    { rank: 54, name: '上海青浦区协和双语学校', city: '上海市' },
    { rank: 55, name: '君诚国际双语学校', city: '北京市' },
    { rank: 56, name: '重庆哈罗礼德学校', city: '重庆市' },
    { rank: 57, name: '杭州威雅学校', city: '杭州市' },
    { rank: 58, name: '上海耀华临港校区', city: '上海市' },
    { rank: 59, name: '海嘉国际双语学校天津校区', city: '天津市' },
    { rank: 60, name: '深圳国际预科书院', city: '深圳市' },
    { rank: 61, name: '上海赫贤学校', city: '上海市' },
    { rank: 61, name: '北京王府学校', city: '北京市' },
    { rank: 61, name: '武汉光谷康桥高级中学', city: '武汉市' },
    { rank: 64, name: '成都市龙泉驿区青苗学校', city: '成都市' },
    { rank: 65, name: '耀华国际教育学校广州校区', city: '广州市' },
    { rank: 66, name: '上海浦东新区民办万科学校国际高中', city: '上海市' },
    { rank: 67, name: '北京市海淀外国语', city: '北京市' },
    { rank: 68, name: '南宁哈罗礼德学校', city: '南宁市' },
    { rank: 69, name: '成都新津墨文学校', city: '成都市' },
    { rank: 70, name: '上海宏润博源学校', city: '上海市' },
    { rank: 71, name: '海口哈罗国际学校', city: '海口市' },
    { rank: 72, name: '成都美视国际学校', city: '成都市' },
    { rank: 74, name: '领科教育北京校区', city: '北京市' },
    { rank: 75, name: '上海诺科学校', city: '上海市' },
    { rank: 76, name: '北京市大兴区耀华京港学校', city: '北京市' },
    { rank: 77, name: '上海闵行区协和双语教科学校BC国际课程', city: '上海市' },
    { rank: 77, name: '苏州工业园区德威联合书院', city: '苏州市' },
    { rank: 77, name: '北京爱迪国际学校', city: '北京市' },
    { rank: 80, name: '上海市光华启迪国际学校', city: '上海市' },
    { rank: 80, name: '广州新侨学校国际部', city: '广州市' },
    { rank: 80, name: '耀华国际教育学校上海古北校区', city: '上海市' },
    { rank: 83, name: '无锡市协和双语学校', city: '无锡市' },
    { rank: 84, name: '北京中加学校', city: '北京市' },
    { rank: 85, name: '上海民办位育中学', city: '上海市' },
    { rank: 86, name: '浙江省诸暨市海亮外国语学校', city: '衢州市' },
    { rank: 87, name: '长沙市宁乡碧桂园学校', city: '长沙市' },
    { rank: 88, name: '上海市民办中芯学校', city: '上海市' },
    { rank: 89, name: '上海美达菲学校', city: '上海市' },
    { rank: 91, name: '上海赫贤学校', city: '上海市' },
    { rank: 92, name: '上海励滕伯克曼学校', city: '上海市' },
    { rank: 92, name: '中加枫华国际学校', city: '苏州市' },
    { rank: 94, name: '广州市华美英语实验学校', city: '广州市' },
    { rank: 95, name: '北京市朝阳区致知学校（一土致知）', city: '北京市' },
    { rank: 97, name: '北外附属宁波外国语学校国际课程中心', city: '宁波市' },
    { rank: 98, name: '合肥高新中加学校', city: '合肥市' },
    { rank: 98, name: '无锡市大桥实验学校国际部', city: '无锡市' },
    { rank: 100, name: '宁波市鄞州中学国际部', city: '宁波市' }
  ]
}

Page({
  data: {
    activeRanking: 'hurun',
    bannerTitle: '2025胡润百学·中国国际化学校排行榜',
    bannerSubtitle: 'Hurun Education Top International Schools in China 2025',
    selectedCity: '全部',
    cities: ['全部', '上海市', '北京市', '佛山市', '深圳市', '苏州市', '广州市', '长沙市', '武汉市', '天津市', '东莞市', '常州市', '青岛市', '杭州市', '西安市', '合肥市', '重庆市', '成都市', '南宁市', '海口市', '无锡市', '衢州市', '宁波市'],
    filteredRanking: [],
    showCityFilter: false
    ,loading: false
  },

  onLoad() { this.updateRanking() },

  changeRanking(e) {
    const activeRanking = e.currentTarget.dataset.ranking
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
  selectCity(e) {
    this.setData({ selectedCity: e.currentTarget.dataset.city, showCityFilter: false }, () => this.updateRanking())
  },
  async updateRanking() {
    if (this.data.activeRanking === 'hurun') {
      this.setData({ loading: true })
      try {
        const params = 'year=2025&page=1&size=100' + (this.data.selectedCity !== '全部' ? '&city=' + encodeURIComponent(this.data.selectedCity) : '')
        const data = await request('/api/rankings/hurun?' + params)
        const records = Array.isArray(data.records) ? data.records : []
        const filteredRanking = records.map((item, index) => ({ ...item, city: item.city || '', key: item.rank + '-' + index, schoolId: item.schoolId == null ? null : item.schoolId, schoolSource: 'international' }))
        this.setData({ filteredRanking, bannerTitle: data.title || this.data.bannerTitle, bannerSubtitle: data.subtitle || this.data.bannerSubtitle, loading: false })
        return
      } catch (e) {
        wx.showToast({ title: '排行榜加载失败', icon: 'none' })
      }
      this.setData({ loading: false })
    }
    const source = rankings[this.data.activeRanking] || []
    const filteredRanking = source
      .filter(item => this.data.selectedCity === '全部' || item.city === this.data.selectedCity)
      .map((item, index) => Object.assign({}, item, { key: item.rank + '-' + index }))
    this.setData({ filteredRanking })
  },

  onSchoolTap(e) {
    const item = this.data.filteredRanking[Number(e.currentTarget.dataset.index)]
    if (!item || item.schoolId == null) {
      wx.showToast({ title: '学校详情暂不可用', icon: 'none' })
      return
    }
    wx.navigateTo({ url: '/pages/school-detail/school-detail?id=' + encodeURIComponent(item.schoolId) + '&schoolSource=international' })
  }
})
