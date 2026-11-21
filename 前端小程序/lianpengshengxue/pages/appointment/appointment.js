const { request } = require('../../utils/request')
Page({
  data: { filters: [{ key: 'all', label: '全部' }, { key: 'primary', label: '小学' }, { key: 'junior', label: '初中' }, { key: 'high', label: '高中' }], activeFilter: 'all', selectedDistrict: '全部地区', districts: ['全部地区'], allSchools: [], visibleSchools: [], loading: true, placeholder: '/assets/logo.png', statusTop: 20, navHeight: 84 },
  onLoad() { this.setNavigationMetrics(); this.loadSchools() },
  setNavigationMetrics() { try { const menu = wx.getMenuButtonBoundingClientRect(); const top = menu && menu.top ? menu.top : 20; const height = menu && menu.bottom ? menu.bottom + 8 : top + 44; this.setData({ statusTop: top, navHeight: height }) } catch (e) {} },
  async loadSchools() { try { const data = await request('/api/appointment-schools', 'GET', {}, false, true); const all = Object.keys(data || {}).reduce((list, key) => list.concat(data[key] || []), []); const districts = ['全部地区'].concat([...new Set(all.map(item => item.district).filter(Boolean))]); this.setData({ allSchools: all, districts, loading: false }, () => this.applyFilters()) } catch (e) { this.setData({ loading: false }) } },
  filterSchools(e) { this.setData({ activeFilter: e.currentTarget.dataset.key }, this.applyFilters) },
  onDistrictChange(e) { this.setData({ selectedDistrict: this.data.districts[Number(e.detail.value)] || '全部地区' }, this.applyFilters) },
  applyFilters() { const source = this.data.activeFilter === 'high' ? 'score' : this.data.activeFilter; const schools = this.data.allSchools.filter(item => (source === 'all' || item.schoolSource === source) && (this.data.selectedDistrict === '全部地区' || item.district === this.data.selectedDistrict)); const priority = ['徐汇区', '闵行区', '浦东新区']; schools.sort((a, b) => this.districtPriority(a, priority) - this.districtPriority(b, priority)); this.setData({ visibleSchools: schools }) },
  districtPriority(item, priority) { const district = String(item.district || ''); const index = priority.findIndex(name => district === name || district.includes(name.replace('区', ''))); return index < 0 ? priority.length : index },
  goBack() { wx.navigateBack() },
  reserve(e) { wx.navigateTo({ url: '/pages/lead/lead?school=' + encodeURIComponent(e.currentTarget.dataset.school || '') }) },
  openDetail(e) { const item = e.currentTarget.dataset.item || {}; wx.navigateTo({ url: '/pages/appointment-school-detail/appointment-school-detail?source=' + encodeURIComponent(item.schoolSource || '') + '&id=' + encodeURIComponent(item.id || '') }) },
  onPullDownRefresh() { this.loadSchools().finally(() => wx.stopPullDownRefresh()) }
})
