const { request } = require('../../utils/request')
Page({
  data: { articles: [], total: 0, loading: true, error: '', navBarHeight: 88, menuTop: 26, menuHeight: 32 },
  onLoad() { try { const menu = wx.getMenuButtonBoundingClientRect(); this.setData({ navBarHeight: menu.bottom + 8, menuTop: menu.top, menuHeight: menu.height }) } catch (error) {} this.load() },
  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()) },
  async load() { this.setData({ loading: true, error: '' }); try { const result = await request('/api/lottery-guide/articles', 'GET', { page: 1, size: 100 }); const articles = (Array.isArray(result) ? result : (result && (result.records || result.list || result.content) || [])).filter(item => Number(item.status) === 1).slice().sort((a, b) => Number(b.sortOrder || 0) - Number(a.sortOrder || 0)); this.setData({ articles, total: articles.length, loading: false }) } catch (e) { this.setData({ loading: false, error: '摇号指南加载失败，请稍后重试' }) } },
  openArticle(e) { wx.navigateTo({ url: '/pages/admission-content-detail/admission-content-detail?id=' + encodeURIComponent(e.currentTarget.dataset.id) + '&source=lottery-guide' }) },
  goBack() { const pages = getCurrentPages(); if (pages.length > 1) wx.navigateBack({ delta: 1 }); else wx.switchTab({ url: '/pages/home/home' }) }
})
